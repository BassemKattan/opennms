/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.timeseries.meta;

import static org.opennms.netmgt.timeseries.util.TimeseriesUtils.toResourceId;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.inject.Named;
import javax.sql.DataSource;

import org.opennms.core.cache.Cache;
import org.opennms.core.cache.CacheBuilder;
import org.opennms.core.cache.CacheConfig;
import org.opennms.core.utils.DBUtils;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.netmgt.model.ResourcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheLoader;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.agkn.hll.HLL;

/**
 * TimeSeriesMetaDataDao stores string values associated with resourceids in the database. It leverages a Guava cache.
 * Design choice: for speed and high throughput we don't keep the cache and the database 100% in sync at the time of writing.
 * Latest 'cache_duration' after writing they will be the same.
 */
@Service
public class TimeSeriesMetaDataDao {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesMetaDataDao.class);

    final static String SQL_WRITE = "INSERT INTO timeseries_meta(resourceid, name, value)  values (?, ?, ?) ON CONFLICT (resourceid, name) DO UPDATE SET value=?";
    final static String SQL_READ = "SELECT name, value FROM timeseries_meta where resourceid = ?";

    private final DataSource dataSource;

    private final Cache<String, Map<String, String>> cache; // resourceId, Map<name, value>

    private final Timer metadataWriteTimer;
    private final Timer metadataReadTimer;
    private final Gauge<Long> metadataCardinality;
    private final Gauge<Long> metadataResourceCardinality;
    private final Gauge<Long> metadataKeyCardinality;
    private final Gauge<Long> metadataValueCardinality;

    /**
     * A HyperLogLog used to track the cardinality of the meta-data
     * log2m = 14
     * regwidth = 5
     * numberOfBits = 2 ^ log2m * regwidth.
     *              = 2 ^ 14 * 5
     *              = 81000
     * should support tracking 100,000,000 unique elements with a tolerated loss of 1%
     */
    private final HLL numUniqueResourcesHll = new HLL(14, 5);
    private final HLL numUniqueKeysHll = new HLL(14, 5);
    private final HLL numUniqueValuesHll = new HLL(14, 5);
    private final HLL numUniqueMetadataHll = new HLL(14, 5);

    @SuppressWarnings("UnstableApiUsage")
    private final HashFunction hllHashFunction = Hashing.murmur3_128();

    /**
     * Hack: Short circuit database writes for performance testing.
     */
    private final static boolean DISABLE_DB_WRITES = Boolean.getBoolean("org.opennms.timeseries.dont.write.metadata.to.db");

    @Autowired
    public TimeSeriesMetaDataDao(final DataSource dataSource,
                                 @Named("timeseriesMetaDataCache") CacheConfig cacheConfig,
                                 @Named("timeseriesMetricRegistry") MetricRegistry registry
    ) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null.");
        Objects.requireNonNull(registry, "registry cannot be null.");

        CacheLoader<String, Map<String, String>> loader = new CacheLoader<>(){
            @Override
            public Map<String, String> load(String resourceId) throws Exception {
                if(!DISABLE_DB_WRITES) {
                    return loadFromDataBase(resourceId);
                }
                return new ConcurrentHashMap<>();
            }
        };
        this.cache = new CacheBuilder<>()
                .withConfig(cacheConfig)
                .withCacheLoader(loader)
                .build();
        this.metadataWriteTimer = registry.timer("metadata.write.db");
        this.metadataReadTimer = registry.timer("metadata.read.db");
        // meta-data cardinality tracked via HLL
        this.metadataCardinality = registry.register("metadata.cardinality", numUniqueMetadataHll::cardinality);
        this.metadataResourceCardinality = registry.register("metadata.cardinality.resources", numUniqueResourcesHll::cardinality);
        this.metadataKeyCardinality = registry.register("metadata.cardinality.keys", numUniqueKeysHll::cardinality);
        this.metadataValueCardinality = registry.register("metadata.cardinality.values", numUniqueValuesHll::cardinality);
    }

    public void store(final Collection<MetaData> metaDataCollection) throws SQLException, ExecutionException {
        Objects.requireNonNull(metaDataCollection);
        Set<MetaData> writeToDb = new HashSet<>();

        // find all MetaData that is not present in the cache
        for(MetaData meta : metaDataCollection) {
            trackMetaDataCardinality(meta);

            Map<String, String> attributesForResource = cache.get(meta.getResourceId(), HashMap::new);
            if(attributesForResource.get(meta.getName()) == null) {
                writeToDb.add(meta);
                attributesForResource.put(meta.getName(), meta.getValue()); // add to cache
            }
        }
        // store the uncached meta data
        if(!DISABLE_DB_WRITES && !writeToDb.isEmpty()) {
            storeUncached(writeToDb);
        }
    }

    public void storeUncached(final Collection<MetaData> metaDataCollection) throws SQLException {
        Objects.requireNonNull(metaDataCollection);

        final DBUtils db = new DBUtils(this.getClass());
        try (Timer.Context context = metadataWriteTimer.time()) {

            Connection connection = this.dataSource.getConnection();
            db.watch(connection);

            PreparedStatement ps = connection.prepareStatement(SQL_WRITE);
            db.watch(ps);

            LOG.debug("Inserting {} attributes", metaDataCollection.size());
            for (MetaData metaData : metaDataCollection) {
                ps.setString(1, metaData.getResourceId());
                ps.setString(2, metaData.getName());
                ps.setString(3, metaData.getValue());
                ps.setString(4, metaData.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } finally{
            db.cleanUp();
        }
    }

    public Map<String, String> getForResourcePath(final ResourcePath path) throws StorageException {
        Objects.requireNonNull(path);
        String resourceId = toResourceId(path);
        try {
            return this.cache.get(resourceId);
        } catch (ExecutionException e) {
            throw new StorageException(e);
        }
    }

    private Map<String, String> loadFromDataBase(final String resourceId) throws StorageException {
        Objects.requireNonNull(resourceId);

        final DBUtils db = new DBUtils(this.getClass());
        try (Timer.Context context = metadataReadTimer.time()) {
            final Connection connection = this.dataSource.getConnection();
            db.watch(connection);
            PreparedStatement statement = connection.prepareStatement(SQL_READ);
            db.watch(statement);
            statement.setString(1, resourceId);
            ResultSet rs = statement.executeQuery();
            db.watch(rs);
            Map<String, String> metaData = new HashMap<>();
            while (rs.next()) {
                String name = rs.getString("name");
                String value = rs.getString("value");
                metaData.put(name, value);
            }
            return metaData;
        } catch (SQLException e) {
            LOG.error("Could not retrieve meta data for resourceId={}", resourceId, e);
            throw new StorageException(e);
        } finally {
            db.cleanUp();
        }
    }

    /**
     * Used to keep track of the cardinality of the meta-data.
     *
     * @param meta
     */
    @SuppressWarnings("UnstableApiUsage")
    private void trackMetaDataCardinality(MetaData meta) {
        numUniqueResourcesHll.addRaw(hllHashFunction.hashString(meta.getResourceId(), StandardCharsets.UTF_8).asLong());
        numUniqueKeysHll.addRaw(hllHashFunction.hashString(meta.getName(), StandardCharsets.UTF_8).asLong());
        numUniqueValuesHll.addRaw(hllHashFunction.hashString(meta.getValue(), StandardCharsets.UTF_8).asLong());

        StringBuilder sb = new StringBuilder();
        sb.append(meta.getResourceId());
        sb.append(meta.getName());
        sb.append(meta.getValue());
        numUniqueMetadataHll.addRaw(hllHashFunction.hashString(sb, StandardCharsets.UTF_8).asLong());
    }
}
