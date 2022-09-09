/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.dao.support;

import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opennms.core.sysprops.SystemProperties;
import org.opennms.core.utils.StringUtils;
import org.opennms.netmgt.dao.api.FilterWatcher;
import org.opennms.netmgt.dao.api.ServiceRef;
import org.opennms.netmgt.dao.api.SessionUtils;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.annotations.EventHandler;
import org.opennms.netmgt.events.api.annotations.EventListener;
import org.opennms.netmgt.events.api.model.IEvent;
import org.opennms.netmgt.filter.api.FilterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@EventListener(name = "FilterWatcher")
public class DefaultFilterWatcher implements FilterWatcher, InitializingBean, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFilterWatcher.class);

    private static final String MATCH_ANY_RULE = "IPADDR != '0.0.0.0'";

    private static final String REFRESH_RATE_LIMIT_MS_SYS_PROP = "org.opennms.netmgt.dao.support.filterServiceRefreshRateLimitMs";
    private static final long DEFAULT_REFRESH_RATE_LIMIT_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long REFRESH_RATE_LIMIT_MS = SystemProperties.getLong(REFRESH_RATE_LIMIT_MS_SYS_PROP, DEFAULT_REFRESH_RATE_LIMIT_MS);

    @Autowired
    private FilterDao filterDao;

    @Autowired
    private SessionUtils sessionUtils;

    private long refreshRateLimitMs = REFRESH_RATE_LIMIT_MS;

    private final Timer timer = new Timer("FilterService-Timer");

    private final Set<FilterSession> sessions = Sets.newConcurrentHashSet();

    @Override
    public void afterPropertiesSet() {
        long timerIntevalMs = Math.min(refreshRateLimitMs, TimeUnit.SECONDS.toMillis(5));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sessions.forEach(s -> {
                        try {
                            s.refreshIfNeeded();
                        } catch (Exception e) {
                            LOG.warn("Error refreshing filter for rule: {}. Will retry again in {}ms.", s.rules, timerIntevalMs, e);
                        }
                    });
                } catch (Exception e) {
                    LOG.warn("Error refreshing filter results. Will retry again in {}ms.", timerIntevalMs, e);
                }
            }
        }, timerIntevalMs, timerIntevalMs);
    }

    @Override
    public void destroy() {
        timer.cancel();
    }

    @Override
    public Session watch(final Set<String> filterRules,
                         final Consumer<FilterResults> callback) {
        return new FilterSession(filterRules, callback);
    }

    @EventHandler(ueis = {
            EventConstants.NODE_GAINED_SERVICE_EVENT_UEI,
            EventConstants.SERVICE_DELETED_EVENT_UEI,
            EventConstants.NODE_CATEGORY_MEMBERSHIP_CHANGED_EVENT_UEI,
            EventConstants.NODE_LOCATION_CHANGED_EVENT_UEI,
            EventConstants.NODE_ADDED_EVENT_UEI,
            EventConstants.NODE_DELETED_EVENT_UEI,
            EventConstants.NODE_GAINED_INTERFACE_EVENT_UEI,
            EventConstants.INTERFACE_DELETED_EVENT_UEI,
            EventConstants.INTERFACE_REPARENTED_EVENT_UEI,
            // Issued when the status changes on a service
            EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI,
            EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI
    })
    public void inventoryChangeEventHandler(final IEvent event) {
        // Filters can depend on arbitrary node fields & relationships so we need to refresh these periodically
        sessions.forEach(FilterSession::requestRefresh);
    }
    
    private static class FilterResultsImpl implements FilterResults {
        private final Map<String, Map<Integer, Map<InetAddress, Set<String>>>> ruleNodeIpServiceMap;

        public FilterResultsImpl(Map<String, Map<Integer, Map<InetAddress, Set<String>>>> ruleNodeIpServiceMap) {
            this.ruleNodeIpServiceMap = Objects.requireNonNull(ruleNodeIpServiceMap);
        }

        @Override
        public Map<String, Map<Integer, Map<InetAddress, Set<String>>>> getRuleNodeIpServiceMap() {
            return this.ruleNodeIpServiceMap;
        }

        @Override
        public Set<ServiceRef> getServicesNamed(String serviceName) {
            Set<ServiceRef> serviceRefs = new LinkedHashSet<>();
            for (final var rule : this.ruleNodeIpServiceMap.values()) {
                for (final var nodeEntry : rule.entrySet()) {
                    int nodeId = nodeEntry.getKey();
                    for (final var interfaceEntry : nodeEntry.getValue().entrySet()) {
                        InetAddress interfaceAddress = interfaceEntry.getKey();
                        if (interfaceEntry.getValue().contains(serviceName)) {
                            serviceRefs.add(new ServiceRef(nodeId, interfaceAddress, serviceName));
                        }
                    }
                }
            }
            return serviceRefs;
        }
    }

    private class FilterSession implements Session {
        private Set<String> rules;
        private final Consumer<FilterResults> callback;
        private final AtomicReference<FilterResults> lastFilterResultsRef = new AtomicReference<>();
        private long lastRefreshedMs;
        private long lastRefreshRequestMs;

        public FilterSession(final Set<String> rules, final Consumer<FilterResults> callback) {
            this.rules = normalizeFilters(Objects.requireNonNull(rules));
            this.callback = Objects.requireNonNull(callback);

            // Initially request a refresh
            requestRefresh();

            // Register ourself for updates
            DefaultFilterWatcher.this.sessions.add(this);
        }

        public synchronized void refreshNow() {
            lastRefreshedMs = System.currentTimeMillis();
            LOG.debug("Refreshing results for filter rules: {}", rules);
            FilterResults newFilterResults = sessionUtils.withReadOnlyTransaction(() ->
                    new FilterResultsImpl(rules.stream()
                            .collect(Collectors.toMap(Function.identity(),
                                                      (rule) -> filterDao.getNodeIPAddressServiceMap(rule)))));
            LOG.debug("Done refreshing results for rule.");

            final FilterResults lastFilterResults = lastFilterResultsRef.get();
            if (Objects.equals(lastFilterResults, newFilterResults)) {
                // nothing has changed, noop
                return;
            }

            lastFilterResultsRef.set(newFilterResults);
            notifyCallback(newFilterResults);
        }

        public synchronized boolean refreshIfNeeded() {
            if (lastRefreshRequestMs > 0
                    && lastRefreshRequestMs >= lastRefreshedMs
                    && System.currentTimeMillis() - lastRefreshedMs >= refreshRateLimitMs) {
                lastRefreshRequestMs = 0;
                refreshNow();
                return true;
            }
            return false;
        }

        public synchronized boolean requestRefresh() {
            lastRefreshRequestMs = System.currentTimeMillis();
            return refreshIfNeeded();
        }

        private void notifyCallback(FilterResults results) {
            try {
                this.callback.accept(results);
            } catch (Exception e) {
                LOG.warn("Error notifying callback: {} for results of filter rules: {}.", this.callback, rules, e);
            }
        }

        @Override
        public void setFilters(final Set<String> filterRules) {
            this.rules = normalizeFilters(filterRules);
            this.refreshNow();
        }

        @Override
        public void close() {
            DefaultFilterWatcher.this.sessions.remove(this);
        }
    }

    public void setRefreshRateLimitMs(long refreshRateLimitMs) {
        this.refreshRateLimitMs = refreshRateLimitMs;
    }

    public long getRefreshRateLimitMs() {
        return refreshRateLimitMs;
    }

    public void setFilterDao(FilterDao filterDao) {
        this.filterDao = filterDao;
    }

    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    private Set<String> normalizeFilters(final Set<String> filters) {
        if (filters == null) {
            return Set.of(MATCH_ANY_RULE);
        } else {
            // TODO fooker: Return a wildcard if any element is wildcard?
            return filters.stream()
                          .map(filter -> {
                              if (StringUtils.isEmpty(filter)) {
                                  return MATCH_ANY_RULE;
                              } else {
                                  this.filterDao.validateRule(filter);
                                  return filter.trim();
                              }
                          })
                          .collect(Collectors.toSet());
        }
    }
}
