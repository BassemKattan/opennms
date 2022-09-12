/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.core.criteria;

import java.util.Objects;

public class Order {
    public static interface OrderVisitor {
        public void visitAttribute(final String attribute);

        public void visitAscending(final boolean ascending);
    }

    private final String m_attribute;

    private final boolean m_ascending;

    public Order(final String attribute, boolean ascending) {
        m_attribute = attribute;
        m_ascending = ascending;
    }

    public void visit(final OrderVisitor visitor) {
        visitor.visitAttribute(getAttribute());
        visitor.visitAscending(asc());
    }

    public String getAttribute() {
        return m_attribute;
    }

    public boolean asc() {
        return m_ascending;
    }

    public boolean desc() {
        return !m_ascending;
    }

    public static Order asc(final String attribute) {
        return new Order(attribute, true);
    }

    public static Order desc(final String attribute) {
        return new Order(attribute, false);
    }

    /*
     * we don't include m_ascending since a single order attribute should only
     * be used once
     */
    @Override
    public int hashCode() {
        return Objects.hash(m_attribute);
    }

    /*
     * we don't include m_ascending since a single order attribute should only
     * be used once
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Order)) return false;
        final Order that = (Order) obj;
        return Objects.equals(this.m_attribute, that.m_attribute);
    }

    @Override
    public String toString() {
        return "Order [attribute=" + m_attribute + ", ascending=" + m_ascending + "]";
    }

}
