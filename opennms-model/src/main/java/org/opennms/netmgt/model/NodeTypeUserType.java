/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.CharacterType;
import org.hibernate.type.EnumType;
import org.hibernate.type.StringType;
import org.opennms.netmgt.model.OnmsNode.NodeType;

public class NodeTypeUserType extends EnumType {

    private static final long serialVersionUID = -7390506282856507291L;

    private static final int[] SQL_TYPES = new int[] { java.sql.Types.CHAR };

	/**
     * A public default constructor is required by Hibernate.
     */
    public NodeTypeUserType() {}

    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(final ResultSet rs, final String[] names, SessionImplementor session, final Object owner) throws HibernateException, SQLException {
        Character c = CharacterType.INSTANCE.nullSafeGet(rs, names[0], session);
        if (c == null) {
            return null;
        }
        for (NodeType type : NodeType.values()) {
            if (type.toString().equals(c.toString())) {
                return type;
            }
        }
        throw new HibernateException("Invalid value for NodeUserType: " + c);
    }

    @Override
    public void nullSafeSet(final PreparedStatement st, final Object value, final int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            StringType.INSTANCE.nullSafeSet(st, null, index, session);
        } else if (value instanceof NodeType){
            CharacterType.INSTANCE.nullSafeSet(st, ((NodeType)value).toString().charAt(0), index, session);
        } else if (value instanceof String){
            for (NodeType type : NodeType.values()) {
                if (type.toString().equals(value)) {
                    CharacterType.INSTANCE.nullSafeSet(st, type.toString().charAt(0), index, session);
                }
            }
        }
    }

    @Override
    public Class<NodeType> returnedClass() {
        return NodeType.class;
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public void setParameterValues(Properties parameters) {
    }
}
