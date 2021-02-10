/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.server;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.opennms.netmgt.provision.server.exchange.LineConversation;

/**
 * <p>AsyncSimpleServer class.</p>
 *
 * @author thedesloge
 * @version $Id: $
 */
public class AsyncSimpleServer {

    private LineConversation m_lineConversation;
    private int m_port = 9123;
    private int m_bufferSize = 2048;
    private int m_idleTime = 10;
    
    /**
     * <p>init</p>
     *
     * @throws java.lang.Exception if any.
     */
    public final void init() throws Exception {
        m_lineConversation = new LineConversation();
        onInit();
    }

    /**
     * <p>onInit</p>
     */
    protected void onInit() {
        // Do nothing by default
    }
    
    /**
     * <p>startServer</p>
     *
     * @throws java.lang.Exception if any.
     */
    public void startServer() throws Exception {

        
    }
    
    /**
     * <p>stopServer</p>
     *
     * @throws java.lang.Exception if any.
     */
    public void stopServer() throws Exception{
    }
    
    /**
     * <p>addRequestHandler</p>
     *
     * @param request a {@link java.lang.String} object.
     * @param response a {@link java.lang.String} object.
     */
    public void addRequestHandler(String request, String response) {
        m_lineConversation.addRequestHandler(request, response);
    }
    
    /**
     * <p>setBanner</p>
     *
     * @param banner a {@link java.lang.String} object.
     */
    public void setBanner(String banner) {
        m_lineConversation.setBanner(banner);
    }
    
    /**
     * <p>setExpectedClose</p>
     *
     * @param closeRequest a {@link java.lang.String} object.
     */
    public void setExpectedClose(String closeRequest) {
        m_lineConversation.setExpectedClose(closeRequest);
    }
    
    /**
     * <p>setExpectedClose</p>
     *
     * @param closeRequest a {@link java.lang.String} object.
     * @param closeResponse a {@link java.lang.String} object.
     */
    public void setExpectedClose(String closeRequest, String closeResponse) {
        m_lineConversation.setExpectedClose(closeRequest, closeResponse);
    }
    

    /**
     * <p>setPort</p>
     *
     * @param port a int.
     */
    public void setPort(int port) {
        m_port = port;
    }

    /**
     * <p>getPort</p>
     *
     * @return a int.
     */
    public int getPort() {
        return m_port;
    }

    /**
     * <p>setBufferSize</p>
     *
     * @param bufferSize a int.
     */
    public void setBufferSize(int bufferSize) {
        m_bufferSize = bufferSize;
    }

    /**
     * <p>getBufferSize</p>
     *
     * @return a int.
     */
    public int getBufferSize() {
        return m_bufferSize;
    }

    /**
     * <p>setIdleTime</p>
     *
     * @param idleTime a int.
     */
    public void setIdleTime(int idleTime) {
        m_idleTime = idleTime;
    }

    /**
     * <p>getIdleTime</p>
     *
     * @return a int.
     */
    public int getIdleTime() {
        return m_idleTime;
    }
}
