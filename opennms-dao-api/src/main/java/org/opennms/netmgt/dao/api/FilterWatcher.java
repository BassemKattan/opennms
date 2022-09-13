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

package org.opennms.netmgt.dao.api;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Evaluates filter rule and issues callback when the results change.
 */
public interface FilterWatcher {

    interface FilterResults {
        Map<String, Map<Integer, Map<InetAddress, Set<String>>>> getRuleNodeIpServiceMap();

        Set<ServiceRef> getServicesNamed(String serviceName);
    }

    interface Session extends Closeable {
        @Override
        void close();

        /**
         * Alter the filter rule used in this watcher session.
         * This will trigger a re-evaluation of the filters and may trigger the callback if the filter output changed.
         * Changing the filter will always trigger the callback assigned to the session.
         * @param filterRules see {@link FilterWatcher#watch(Set, Consumer)} for how filters behave
         */
        void setFilters(Set<String> filterRules);
    }

    /**
     * Issues callbacks to the given consumer when the results of the filter change.
     *
     * A callback is expected to be issued immediately when the watch session is started.
     *
     * Additional callback will be made if/when the results change.
     *
     * @param filterRules a set of valid filter rule
     *                    if null the filter will match everything
     *                    if empty, the filter will match nothing
     *                    if any element is null or empty the filter will match everything
     * @param callback used for callbacks
     * @return close when done watching
     */
    Session watch(Set<String> filterRules, Consumer<FilterResults> callback);

    default Session watch(String filterRule, Consumer<FilterResults> callback) {
        return this.watch(filterRule == null ? null : Set.of(filterRule), callback);
    }

}
