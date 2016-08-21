<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="memoryInfo" type="org.jahia.modules.serversettings.memoryThread.MemoryThreadInformationManagement"--%>
<template:addResources type="javascript" resources="jquery.min.js,jquery.blockUI.js,workInProgress.js"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
<template:addResources>
    <script type="text/javascript">
        $(document).ready(function() {
            $('button.blockUI').click(function() {workInProgress('${i18nWaiting}');});
        });
    </script>
</template:addResources>

<div class="page-header">
    <h2><fmt:message key="serverSettings.manageMemory"/></h2>
</div>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-danger' : 'alert-success'}">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
        ${fn:escapeXml(msg.text)}
    </div>
</c:forEach>

<div class="panel-group" id="accordion2">
    <div class="panel panel-default">
        <div class="panel-heading" role="tab" >
            <a data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
                <strong><fmt:message key="serverSettings.manageMemory.memory"/>&nbsp;(${memoryInfo.memoryUsage}%&nbsp;<fmt:message
                        key="serverSettings.manageMemory.used"/>)</strong>
            </a>
        </div>
        <div id="collapseOne" class="panel-collapse collapse${memoryInfo.mode == 'memory' ? ' in' : ''}" role="tabpanel">
            <div class="panel-body">
                <table class="table table-striped table-bordered table-hover">
                    <c:url value="/engines/images/about.gif" var="infoIcon"/>
                    <tr>
                        <td>
                            <strong><fmt:message key="serverSettings.manageMemory.memory.used"/></strong>
                            <img src="${infoIcon}" alt="(i)" width="16" height="16" title="<fmt:message key='serverSettings.manageMemory.memory.used.tooltip'/>">
                        </td>
                        <td>
                            ${memoryInfo.usedMemory}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="serverSettings.manageMemory.memory.committed"/></strong>
                            <img src="${infoIcon}" alt="(i)" width="16" height="16" title="<fmt:message key='serverSettings.manageMemory.memory.committed.tooltip'/>">
                        </td>
                        <td>
                            ${memoryInfo.committedMemory}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="serverSettings.manageMemory.memory.max"/></strong>
                            <img src="${infoIcon}" alt="(i)" width="16" height="16" title="<fmt:message key='serverSettings.manageMemory.memory.max.tooltip'/>">
                        </td>
                        <td>
                            ${memoryInfo.maxMemory}
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_refresh">
                                    <i class="material-icons">sync</i>
                                    <fmt:message key='label.refresh'/>
                                </button>
                            </form>

                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_gc">
                                    <i class="material-icons">delete</i>
                                    <fmt:message key='serverSettings.manageMemory.memory.gc'/>
                                </button>
                            </form>

                            <c:if test="${heapDumpSupported}">
                                <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                    <button class="btn btn-default btn-sm" type="submit" name="_eventId_heapDump">
                                        <i class="material-icons">settings</i>
                                        <fmt:message key='serverSettings.manageMemory.memory.heapDump'/>
                                    </button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading">
            <a data-toggle="collapse" data-parent="#accordion2" href="#collapseTwo">
                <strong><fmt:message key="serverSettings.manageMemory.threads"/></strong></a>
        </div>
        <div id="collapseTwo" class="panel-collapse collapse${memoryInfo.mode == 'threads' ? ' in' : ''}">
            <div class="panel-body">
                <table class="table table-striped table-bordered table-hover">
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_showTD">
                                    <i class="material-icons">settings</i>
                                    <fmt:message key='serverSettings.manageMemory.threads.performThreadDump.page'/>
                                </button>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <a class="btn btn-default btn-sm" href="<c:url value='/tools/threadDump.jsp?file=true'/>" target="_blank">
                                <i class="material-icons">settings</i>
                                <fmt:message key="serverSettings.manageMemory.threads.performThreadDump.file.download"/>
                            </a>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <input type="hidden" name="threadDump" value="sysout"/>
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_performTD">
                                    <i class="material-icons">settings</i>
                                    <fmt:message key='serverSettings.manageMemory.threads.performThreadDump.system.out'/>
                                </button>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <input type="hidden" name="threadDump" value="file"/>
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_performTD">
                                    <i class="material-icons">settings</i>
                                    <fmt:message key='serverSettings.manageMemory.threads.performThreadDump.file'/>
                                </button>
                                <a href="#threadshint">*</a>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" class="form-inline">
                                <input type="hidden" name="threadDump" value="file"/>
                                <button class="btn btn-default btn-sm" type="submit" name="_eventId_scheduleTD">
                                    <i class="material-icons">settings</i>
                                    <fmt:message key='serverSettings.manageMemory.threads.performThreadDump.multiple'/>
                                </button>
                                <a href="#threadshint">*</a>

                                <div class="form-group">
                                    <label class="control-label" for="threadDumpCount">
                                        <fmt:message key="column.count.label"/>
                                    </label>
                                    <input class="form-control" type="text" id="threadDumpCount" name="threadDumpCount" size="2" value="10"/>
                                </div>

                                <div class="form-group">
                                    <label class="control-label" for="threadDumpInterval">
                                        <fmt:message key="label.interval"/>
                                    </label>
                                    <div class="input-group">
                                        <input class="form-control" type="text" id="threadDumpInterval" name="threadDumpInterval" size="2" value="10"/>
                                        <span class="input-group-addon">
                                            <fmt:message key="label.seconds"/>
                                        </span>
                                    </div>
                                </div>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">

                                <c:choose>
                                    <c:when test="${memoryInfo.threadMonitorActivated}">
                                        <button class="btn btn-danger btn-sm" type="submit" name="_eventId_toggleTD">
                                            <i class="material-icons">stop</i>
                                            <fmt:message key="serverSettings.manageMemory.threads.monitor.stop"/>
                                        </button>
                                    </c:when>
                                    <c:otherwise>
                                        <button class="btn btn-success btn-sm" type="submit" name="_eventId_toggleTD">
                                            <i class="material-icons">play_arrow</i>
                                            <fmt:message key="serverSettings.manageMemory.threads.monitor.start"/>
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <form action="${flowExecutionUrl}" method="POST" style="display: inline;">
                                <c:choose>
                                    <c:when test="${memoryInfo.errorFileDumperActivated}">
                                        <button class="btn btn-danger btn-sm" type="submit" name="_eventId_toggleEFD">
                                            <i class="material-icons">stop</i>
                                            <fmt:message key="serverSettings.manageMemory.errors.dumper.stop"/>
                                        </button>
                                    </c:when>
                                    <c:otherwise>
                                        <button class="btn btn-success btn-sm" type="submit" name="_eventId_toggleEFD">
                                            <i class="material-icons">play_arrow</i>
                                            <fmt:message key="serverSettings.manageMemory.errors.dumper.start"/>
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                                <a href="#errorshint">**</a>
                            </form>
                        </td>
                    </tr>
                </table>
                <hr/>
                <p>
                    <a name="threadshint" id="threadshint">*</a> - <fmt:message key="serverSettings.manageMemory.threads.folder"/>:
                <pre><%= SettingsBean.getThreadDir() %></pre>
                <fmt:message key="serverSettings.manageMemory.threads.folder.overrideHint"/>
                </p>
                <p>
                    <a name="errorshint" id="errorshint">**</a> - <fmt:message key="serverSettings.manageMemory.errors.dumper.hint"/><br/>
                    <fmt:message key="serverSettings.manageMemory.errors.dumper.folder"/>:
                <pre><%= SettingsBean.getErrorDir() %></pre>
                <fmt:message key="serverSettings.manageMemory.errors.dumper.folder.overrideHint"/>
                </p>
            </div>
        </div>
    </div>
</div>
