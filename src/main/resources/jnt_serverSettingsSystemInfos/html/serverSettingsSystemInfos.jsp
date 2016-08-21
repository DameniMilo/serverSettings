<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.TreeMap"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="page-header">
    <h2><fmt:message key="serverSettings.systemInfos"/></h2>
</div>

<div class="panel panel-default">
    <div class="panel-body">
        <% pageContext.setAttribute("systemProperties", new TreeMap(System.getProperties())); %>
        <table class="table table-bordered table-hover table-striped" >
            <c:forEach items="${systemProperties}" var="prop" varStatus="loopStatus">
                <tr class="${(loopStatus.index + 1) % 2 == 0 ? 'evenLine' : 'oddLine'}">
                    <td style="width: 30%;" title="${fn:escapeXml(prop.key)}">
                        <strong>${fn:escapeXml(prop.key)}</strong>
                    </td>
                    <td style="width: 70%; word-break: break-all;" title="${fn:escapeXml(prop.value)}">
                            ${fn:escapeXml(prop.value)}
                    </td>
                </tr>
            </c:forEach>
        </table>
    </div>
</div>

<div class="panel panel-default">
    <div class="panel-heading">
        <fmt:message key="serverSettings.systemInfo.environmentVariables"/>
    </div>
    <div class="panel-body">
        <% pageContext.setAttribute("envVariables", new TreeMap(System.getenv())); %>
        <table class="table table-bordered table-hover table-striped">
            <tbody>
                <c:forEach items="${envVariables}" var="prop" varStatus="loopStatus">
                    <tr class="${(loopStatus.index + 1) % 2 == 0 ? 'evenLine' : 'oddLine'}">
                        <td style="width: 30%;" title="${fn:escapeXml(prop.key)}">
                            <strong>${fn:escapeXml(prop.key)}</strong>
                        </td>
                        <td style="width: 70%; word-break: break-all;" title="${fn:escapeXml(prop.value)}">
                                ${fn:escapeXml(prop.value)}
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
