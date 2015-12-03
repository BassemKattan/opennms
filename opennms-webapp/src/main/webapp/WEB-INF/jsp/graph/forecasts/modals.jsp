<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
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

--%>

<%@page language="java"
        contentType="text/html"
        session="true"
        %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
    Verify the forecasting dependencies.
--%>
<%
boolean canForecast = true;
try {
    // org.opennms.netmgt.jasper.analytics.HWForecast.checkForecastSupport();
} catch (Throwable t) {
    canForecast = false;
}
%>

<c:set var="canForecast" value="<%= canForecast %>"/>

<%--
    Interface Utilization Forecast
--%>
<c:import url="forecasts/modal.jsp">
    <c:param name="title" value="Interface Utilization Forecast" />
    <c:param name="prefix" value="iuf" />
    <c:param name="reportId" value="InterfaceUtilizationForecast" />
    <c:param name="graphNames" value="mib2.traffic-inout" />
    <c:param name="jsimpl" value="forecasts/interfaceUtilizationForecast.jsp" />
    <c:param name="showNetworkTab" value="true" />
    <c:param name="depsSatisfied" value="${canForecast}" />
</c:import>

<%--
    Interface Utilization Forecast (HC)
--%>
<c:import url="forecasts/modal.jsp">
    <c:param name="title" value="Interface Utilization Forecast (High Speed)" />
    <c:param name="prefix" value="iufhc" />
    <c:param name="reportId" value="InterfaceUtilizationForecastHC" />
    <c:param name="graphNames" value="mib2.HCtraffic-inout" />
    <c:param name="jsimpl" value="forecasts/interfaceUtilizationForecast.jsp" />
    <c:param name="showNetworkTab" value="true" />
    <c:param name="depsSatisfied" value="${canForecast}" />
</c:import>

<%--
    Disk Utilization Forecast
--%>
<c:import url="forecasts/modal.jsp">
    <c:param name="title" value="Disk Utilization Forecast" />
    <c:param name="prefix" value="nsduf" />
    <c:param name="reportId" value="NetSnmpDiskUtilizationForecast" />
    <c:param name="graphNames" value="netsnmp.diskpercent" />
    <c:param name="jsimpl" value="forecasts/diskUtilizationForecast.jsp" />
    <c:param name="depsSatisfied" value="${canForecast}" />
</c:import>
