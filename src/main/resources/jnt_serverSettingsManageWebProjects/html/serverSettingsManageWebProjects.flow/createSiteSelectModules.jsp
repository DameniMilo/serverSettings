<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>

<template:addResources>
    <script type="application/javascript">
        var manageSelectedModules = {
            selectAll: function() {
                $('#unselectedModules option').detach().appendTo($('#selectedModules'));
            },
            select: function() {
                $('#unselectedModules option:selected').detach().appendTo($('#selectedModules'));
                $('#selectedModules option:selected').prop('selected', false);
            },
            deselectAll: function() {
                $('#selectedModules option').detach().appendTo($('#unselectedModules'));
            },
            deselect: function() {
                $('#selectedModules option:selected').detach().appendTo($('#unselectedModules'));
                $('#unselectedModules option:selected').prop('selected', false);
            },
            selectValue: function() {
                $('#selectedModules option').prop('selected', true);
            }
        };
    </script>
</template:addResources>


<div class="page-header">
    <c:set var="editingModules" value="${siteBean.editModules}"/>
    <h2><c:choose>
        <c:when test="${not editingModules}">
            <fmt:message key="serverSettings.manageWebProjects.createWebProject"/>
        </c:when>
        <c:otherwise>
            <fmt:message key="serverSettings.manageWebProjects.webProject.selectModules"/>
        </c:otherwise>
    </c:choose>
    </h2>
</div>

<c:if test="${!empty flowRequestContext.messageContext.allMessages}">
    <c:forEach var="error" items="${flowRequestContext.messageContext.allMessages}">
        <div class="alert alert-danger">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
                ${error.text}
        </div>
    </c:forEach>
</c:if>

<form action="${flowExecutionUrl}" method="POST">
    <div class="panel panel-default">
        <div class="panel-body">
            <c:if test="${not editingModules}">
                <div class="form-group form-group-sm label-floating">
                    <label class="control-label" for="templateSet"><fmt:message key="serverSettings.webProjectSettings.pleaseChooseTemplateSet"/></label>

                    <select class="form-control" name="templateSet" id="templateSet">
                        <c:forEach items="${templateSets}" var="module">
                            <option value="${module.id}" ${siteBean.templateSet eq module.id || empty siteBean.templateSet && module.id eq defaultTemplateSetId ? 'selected="true"' : ''}>${module.name}&nbsp;(${module.id})</option>
                        </c:forEach>
                    </select>
                </div>
            </c:if>

            <p><strong><fmt:message key="serverSettings.manageWebProjects.webProject.selectModules"/></strong></p>
            <div class="row">
                <div class="col-md-5">
                    <div class="form-group form-group-sm label-floating">
                        <label><fmt:message key="jnt_serverSettingsManageWebProjects.createSiteSelectModules.label.unselectedModules"/></label>
                        <select id="unselectedModules" class="form-control" multiple>
                            <c:forEach items="${modules}" var="module">
                                <c:if test="${not functions:contains(siteBean.modules,module.id)}">
                                    <option value="${module.id}">${module.name} (${module.id})</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="col-md-2">
                    <div class="row text-center">
                        <div class="btn-group-vertical">
                            <button type="button" class="btn btn-xs btn-default"
                                    title="<fmt:message key='jnt_serverSettingsManageWebProjects.createSiteSelectModules.title.selectAll'/>"
                                    data-toggle="tooltip" data-placement="top"
                                    onclick="manageSelectedModules.selectAll()">
                                <i class="material-icons">last_page</i>
                            </button>
                            <button type="button" class="btn btn-xs btn-primary"
                                    title="<fmt:message key='jnt_serverSettingsManageWebProjects.createSiteSelectModules.title.select'/>"
                                    data-toggle="tooltip" data-placement="top"
                                    onclick="manageSelectedModules.select()">
                                <i class="material-icons">chevron_right</i>
                            </button>
                            <button type="button" class="btn btn-xs btn-primary"
                                    title="<fmt:message key='jnt_serverSettingsManageWebProjects.createSiteSelectModules.title.deselect'/>"
                                    data-toggle="tooltip" data-placement="bottom"
                                    onclick="manageSelectedModules.deselect()">
                                <i class="material-icons">chevron_left</i>
                            </button>
                            <button type="button" class="btn btn-xs btn-default"
                                    title="<fmt:message key='jnt_serverSettingsManageWebProjects.createSiteSelectModules.title.deselectAll'/>"
                                    data-toggle="tooltip" data-placement="bottom"
                                    onclick="manageSelectedModules.deselectAll()">
                                <i class="material-icons">first_page</i>
                            </button>
                        </div>
                    </div>
                </div>
                <div class="col-md-5">
                    <div class="form-group form-group-sm label-floating">
                        <label><fmt:message key="jnt_serverSettingsManageWebProjects.createSiteSelectModules.label.selectedModules"/></label>
                        <select id="selectedModules" class="form-control" name="modules" multiple>
                            <c:forEach items="${modules}" var="module">
                                <c:if test="${functions:contains(siteBean.modules,module.id)}">
                                    <option value="${module.id}">${module.name} (${module.id})</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                </div>
            </div>


            <c:if test="${not editingModules}">
                <div class="form-group form-group-sm label-floating">
                    <label class="control-label" for="language"><fmt:message key="serverSettings.manageWebProjects.webProject.selectLanguage"/></label>

                    <select class="form-control" name="language" id="language">
                        <c:forEach items="${allLocales}" var="locale">
                            <option value="${locale}" ${siteBean.language eq locale ? 'selected="true"' : ''}>${locale.displayName}</option>
                        </c:forEach>
                    </select>
                </div>
            </c:if>

            <button class="btn default" type="submit" name="_eventId_previous">
                <i class="material-icons">navigate_before</i>
                <fmt:message key='label.previous'/>
            </button>
            <button class="btn btn-primary" type="submit" name="_eventId_next" onclick="manageSelectedModules.selectValue()">
                <i class="material-icons">navigate_next</i>
                <fmt:message key='label.next'/>
            </button>
        </div>
    </div>
</form>