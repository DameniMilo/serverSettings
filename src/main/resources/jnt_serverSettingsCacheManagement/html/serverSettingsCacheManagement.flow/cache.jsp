<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="cacheManagement" type="org.jahia.modules.serversettings.cache.CacheManagement"--%>
<%--@elvariable id="cacheManager" type="org.jahia.modules.serversettings.flow.CacheManagerHandler"--%>
<template:addResources type="javascript" resources="jquery.js,jquery.fancybox.pack.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js"/>
<template:addResources type="css" resources="jquery.fancybox.css,tablecloth.css"/>

<div class="page-header">
    <h2><fmt:message key="serverSettings.cacheManagement"/></h2>
</div>

<template:addResources type="inline">
    <c:if test="${cacheManagement.showConfig}">
        <script type="text/javascript">
            $(document).ready(function() {
                $('.configLink').fancybox({
                    'hideOnContentClick': false,
                    'titleShow' : false,
                    'transitionOut' : 'none',
                    'autoDimensions' : false,
                    'width' : 800,
                    'height' : 600
                });
            });
        </script>
    </c:if>
    <script type="text/javascript" charset="utf-8">
        $(document).ready(function() {
            $("table").tablecloth({
                theme: "default",
                sortable: true
            });
        });
        function go(id1, value1, id2, value2, id3, value3) {
            if (id1) {
                document.getElementById(id1).value=value1;
            }
            if (id2) {
                document.getElementById(id2).value=value2;
            }
            if (id3) {
                document.getElementById(id3).value=value3;
            }
            document.getElementById('navigateForm').submit();
        }
    </script>
</template:addResources>


<div class="row">
    <div class="col-md-12">
        <div class="panel panel-default">
            <div class="panel-body">
                <strong><fmt:message key="serverSettings.cache.settings"/></strong>
                <form action="${flowExecutionUrl}" id="navigateForm" method="POST">
                    <input type="hidden" name="_showActions"/>
                    <input type="hidden" name="_showConfig"/>
                    <input type="hidden" name="_showBytes"/>
                    <input type="hidden" id="action" name="action" value=""/>
                    <input type="hidden" id="name" name="name" value=""/>
                    <input type="hidden" id="propagate" name="propagate" value="false"/>
                    <input type="hidden" name="_eventId" value="submit"/>
                    <div class="form-group form-group-sm" style="margin-top: 0;">
                        <div class="row">
                            <div class="col-md-4">
                                <div class="checkbox">
                                    <label class="input-sm" for="cbActions">
                                        <input id="cbActions" type="checkbox" name="showActions" ${cacheManagement.showActions?" checked":""} onclick="document.getElementById('navigateForm').submit()"/> <fmt:message key="serverSettings.cache.showActions"/>
                                    </label>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="checkbox">
                                    <label class="input-sm" for="cbConfig">
                                        <input id="cbConfig" type="checkbox" name="showConfig" ${cacheManagement.showConfig?" checked":""} onclick="document.getElementById('navigateForm').submit()"/> <fmt:message key="serverSettings.cache.showConfig"/>
                                    </label>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="checkbox">
                                    <label class="input-sm" for="cbBytes">
                                        <input id="cbBytes" type="checkbox" name="showBytes" ${cacheManagement.showBytes?" checked":""} onclick="document.getElementById('navigateForm').submit()"/> <fmt:message key="serverSettings.cache.showBytes"/>
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>


                <div>
                    <div><strong><fmt:message key="label.actions"/></strong></div>
                    <div class="btn-group-sm">
                        <a class="btn btn-default" href="#refresh" onclick="go(); return false;" title="<fmt:message key='label.refresh'/>">
                            <i class="material-icons">sync</i>
                            <fmt:message key="label.refresh"/>
                        </a>
                        <a class="btn btn-default" href="#flushOutputCaches" onclick="go('action', 'flushOutputCaches'); return false;" title="<fmt:message key="serverSettings.cache.flushOutputCaches.title"/>">
                            <i class="material-icons">delete</i>
                            <fmt:message key="serverSettings.cache.flushOutputCaches"/>
                        </a>
                        <c:if test="${cacheManager.clusterActivated}">
                            <a class="btn btn-default" href="#flushOutputCaches" onclick="go('action', 'flushOutputCaches', 'propagate', 'true'); return false;" title="<fmt:message key="serverSettings.cache.flushOutputCaches.cluster.title"/>">
                                <i class="material-icons">delete</i>
                                <fmt:message key="serverSettings.cache.flushOutputCaches.cluster"/>
                            </a>
                        </c:if>
                        <a class="btn btn-default" href="#flushAllCaches" onclick="go('action', 'flushAllCaches'); return false;" title="<fmt:message key="serverSettings.cache.flushAllCaches.title"/>">
                            <i class="material-icons">delete</i>
                            <fmt:message key="serverSettings.cache.flushAllCaches"/>
                        </a>
                        <c:if test="${cacheManager.clusterActivated}">
                            <a class="btn btn-default" href="#flushAllCaches" onclick="go('action', 'flushAllCaches', 'propagate', 'true'); return false;" title="<fmt:message key="serverSettings.cache.flushAllCaches.cluster.title"/>">
                                <i class="material-icons">delete</i>
                                <fmt:message key="serverSettings.cache.flushAllCaches.cluster"/>
                            </a>
                        </c:if>
                    </div>
                </div>

                <c:forEach items="${cacheManagers}" var="entry" varStatus="managerStatus">
                    <c:set var="manager" value="${entry.value}"/>
                    <h2><fmt:message key="serverSettings.cache.cacheManager"/>: ${manager.name}
                        <c:if test="${cacheManagement.showConfig}">
                            <a class="btn btn-info btn-fab btn-fab-xs configLink" title="<fmt:message key='serverSettings.cache.configLink.title'/>" href="#managerconfig-${managerStatus.index}">
                                <i class="material-icons">info</i>
                            </a>
                            <div style="display: none;">
                                <div id="managerconfig-${managerStatus.index}">
                                    <h3>${manager.name}</h3>
                                    <pre>${fn:escapeXml(manager.config)}</pre>
                                </div>
                            </div>
                        </c:if>
                    </h2>
                    <c:if test="${cacheManagement.showActions}">
                        <p>
                            <a class="btn btn-default" href="#flushCaches" onclick="go('action', 'flushCaches', 'name', '${manager.name}'); return false;" title="<fmt:message key="serverSettings.cache.flushCaches.title"/>">
                                <i class="material-icons">delete</i>
                                <fmt:message key="serverSettings.cache.flushCaches"/>&nbsp;${manager.name}
                            </a>
                            <c:if test="${cacheManager.clusterActivated}">
                                <a class="btn btn-default" href="#flushCaches" onclick="go('action', 'flushCaches', 'name', '${manager.name}', 'propagate', 'true'); return false;" title="<fmt:message key="serverSettings.cache.flushCaches.cluster.title"/>">
                                    <i class="material-icons">delete</i>
                                    <fmt:message key="serverSettings.cache.flushCaches.cluster"/>&nbsp;${manager.name}
                                </a>
                            </c:if>
                        </p>
                    </c:if>
                    <table class="table table-bordered table-hover table-striped">
                        <thead>
                        <tr>
                            <th rowspan="2">#</th>
                            <c:if test="${cacheManagement.showConfig}">
                                <th rowspan="2">?</th>
                            </c:if>
                            <th rowspan="2"><fmt:message key="serverSettings.cache.names"/></th>
                            <th colspan="${manager.overflowToOffHeap ? 4 : 3}"><fmt:message key="serverSettings.cache.entries"/></th>
                            <c:if test="${cacheManagement.showBytes}">
                                <th colspan="${manager.overflowToOffHeap ? 3 : 2}"><fmt:message key="serverSettings.cache.size"/></th>
                            </c:if>
                            <th colspan="4"><fmt:message key="serverSettings.cache.stats"/></th>
                            <c:if test="${cacheManagement.showActions}">
                                <th rowspan="2"><fmt:message key="label.actions"/></th>
                            </c:if>
                        </tr>
                        <tr>
                            <th><fmt:message key="serverSettings.cache.total"/></th>
                            <th><fmt:message key="serverSettings.cache.memory"/></th>
                            <th><fmt:message key="serverSettings.cache.disk"/></th>
                            <c:if test="${manager.overflowToOffHeap}">
                                <th><fmt:message key="serverSettings.cache.offHeap"/></th>
                            </c:if>
                            <c:if test="${cacheManagement.showBytes}">
                                <th><fmt:message key="serverSettings.cache.memory"/></th>
                                <th><fmt:message key="serverSettings.cache.disk"/></th>
                                <c:if test="${manager.overflowToOffHeap}">
                                    <th><fmt:message key="serverSettings.cache.offHeap"/></th>
                                </c:if>
                            </c:if>
                            <th><fmt:message key="serverSettings.cache.total"/></th>
                            <th><fmt:message key="serverSettings.cache.hits"/></th>
                            <th><fmt:message key="serverSettings.cache.misses"/></th>
                            <th><fmt:message key="serverSettings.cache.rate"/></th>
                        </tr>
                        </thead>
                        <tbody>

                        <c:forEach items="${manager.caches}" var="cacheEntry" varStatus="status">
                            <c:set var="cache" value="${cacheEntry.value}"/>

                            <tr>
                                <td><strong>${status.index + 1}</strong></td>
                                <c:if test="${cacheManagement.showConfig}">
                                    <td align="center">
                                        <a class="btn btn-info btn-fab btn-fab-xs configLink" title="<fmt:message key='serverSettings.cache.showDetail'/>" href="#config-${managerStatus.index}-${status.index}">
                                            <i class="material-icons">info</i>
                                        </a>
                                        <div style="display: none;">
                                            <div id="config-${managerStatus.index}-${status.index}">
                                                <h3>${cache.name}</h3>
                                                <pre>${fn:escapeXml(cache.config)}</pre>
                                            </div>
                                        </div>
                                    </td>
                                </c:if>

                                <td style="word-break: break-all;">${cache.name}</td>
                                <td align="center">${cache.size}</td>
                                <td align="center">${cache.localHeapSize}</td>
                                <td align="center">${cache.overflowToDisk ? cache.localDiskSize : '-'}</td>
                                <c:if test="${manager.overflowToOffHeap}">
                                    <td align="center">${cache.overflowToOffHeap ? cache.localOffHeapSize : '-'}</td>
                                </c:if>

                                <c:if test="${cacheManagement.showBytes}">
                                    <td align="center">${cache.localHeapSizeInBytesHumanReadable}</td>
                                    <td align="center">
                                            ${cache.overflowToDisk ? cache.localDiskSizeInBytesHumanReadable : '-'}
                                    </td>
                                    <c:if test="${manager.overflowToOffHeap}">
                                        <td align="center">${cache.overflowToOffHeap ? cache.localOffHeapSizeInBytesHumanReadable : '-'}</td>
                                    </c:if>
                                </c:if>

                                <td align="center">${cache.accessCount}</td>
                                <td align="center">${cache.hitCount}</td>
                                <td align="center">${cache.missCount}</td>

                                <c:set var="cacheEfficiency" value="${cache.hitRatio}"/>
                                <c:set var="effColour" value="#222222"/>
                                <c:choose>
                                    <c:when test="${cacheEfficiency > 0 && cacheEfficiency < 30}">
                                        <c:set var="effColour" value="label-warning"/>
                                    </c:when>
                                    <c:when test="${cacheEfficiency >= 30 && cacheEfficiency < 70}">
                                        <c:set var="effColour" value="label-info"/>
                                    </c:when>
                                    <c:when test="${cacheEfficiency >= 70}">
                                        <c:set var="effColour" value="label-success"/>
                                    </c:when>
                                </c:choose>
                                <td align="center"><span class="label ${effColour}"><fmt:formatNumber value="${cacheEfficiency}" pattern="0.00"/></span></td>

                                <c:if test="${cacheManagement.showActions}">
                                    <td align="center">
                                        <a class="btn btn-default btn-fab btn-fab-xs" href="#flush" onclick="go('action', 'flush', 'name', '${cache.name}'); return false;" title="<fmt:message key='serverSettings.cache.flush'/>&nbsp;${cache.name}">
                                            <i class="material-icons">delete</i>
                                        </a>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>

                        <tr class="info">
                            <td colspan="${cacheManagement.showConfig ? '3' : '2'}"><fmt:message key="serverSettings.cache.total"/></td>
                            <td align="center">${manager.size}</td>
                            <td align="center">${manager.localHeapSize}</td>
                            <td align="center">${manager.overflowToDisk ? manager.localDiskSize : '-'}</td>
                            <c:if test="${manager.overflowToOffHeap}">
                                <td align="center">${manager.localOffHeapSize}</td>
                            </c:if>
                            <c:if test="${cacheManagement.showBytes}">
                                <td align="center">${manager.localHeapSizeInBytesHumanReadable}</td>
                                <td align="center">${manager.localDiskSizeInBytesHumanReadable}</td>
                                <c:if test="${manager.overflowToOffHeap}">
                                    <td align="center">${manager.localOffHeapSizeInBytesHumanReadable}</td>
                                </c:if>
                            </c:if>
                            <td align="center">${manager.accessCount}</td>
                            <td align="center">${manager.hitCount}</td>
                            <td align="center">${manager.missCount}</td>
                            <td>&nbsp;</td>
                            <c:if test="${cacheManagement.showActions}">
                                <td align="center">&nbsp;</td>
                            </c:if>
                        </tr>

                        </tbody>
                    </table>
                </c:forEach>
            </div>
        </div>
    </div>
</div>