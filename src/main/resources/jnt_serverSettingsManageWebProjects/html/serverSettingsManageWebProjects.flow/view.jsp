<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="webprojectHandler" type="org.jahia.modules.serversettings.flow.WebprojectHandler"--%>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<jcr:node var="sites" path="/sites"/>
<jcr:nodeProperty name="j:defaultSite" node="${sites}" var="defaultSite"/>
<c:set var="defaultPrepackagedSite" value="acmespaceelektra.zip"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,workInProgress.js"/>
<template:addResources type="javascript" resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js,dataTables.serverSettings.js"/>
<jsp:useBean id="nowDate" class="java.util.Date" />
<fmt:formatDate value="${nowDate}" pattern="yyyy-MM-dd-HH-mm" var="now"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
<fmt:message key="serverSettings.manageWebProjects.noWebProjectSelected" var="i18nNoSiteSelected"/>
<c:set var="i18nNoSiteSelected" value="${functions:escapeJavaScript(i18nNoSiteSelected)}"/>
<c:set var="exportAllowed" value="${renderContext.user.root}"/>
<script type="text/javascript">
    function submitSiteForm(act, site) {
    	if (typeof site != 'undefined') {
    		$("<input type='hidden' name='sitesKey' />").attr("value", site).appendTo('#sitesForm');
    	} else {
    		$("#sitesForm input:checkbox[name='selectedSites']:checked").each(function() {
    			$("<input type='hidden' name='sitesKey' />").attr("value", $(this).val()).appendTo('#sitesForm');
    		});
    	}
        if (act == 'exportToFile' || act == 'exportToFileStaging') {
            workInProgress('${i18nWaiting}');
        }
        $('#sitesFormAction').val(act);
    	$('#sitesForm').submit();
    }

    $(document).ready(function () {
    	$("a.sitesAction").click(function () {
    		var act=$(this).attr('id');
    		if (act != 'createSite' && $("#sitesForm input:checkbox[name='selectedSites']:checked").length == 0) {
        		alert("${i18nNoSiteSelected}");
    			return false;
    		}
    		submitSiteForm(act);
    		return false;
    	});
    	<c:if test="${exportAllowed}">
        $("#exportSites").click(function (){
            var selectedSites = [];
            var checkedSites = $("input[name='selectedSites']:checked");
            checkedSites.each(function(){
                selectedSites.push($(this).val());
            });
            if(selectedSites.length==0) {
                alert("${i18nNoSiteSelected}");
                return false;
            }
            var name = selectedSites.length>1?"sites":selectedSites;
            var sitebox = "";
            for (i = 0; i < selectedSites.length; i++) {
                sitebox = sitebox + "&sitebox=" + selectedSites[i];
            }
            $(this).target = "_blank";
            window.open("${url.context}/cms/export/default/"+name+ '_export_${now}.zip?exportformat=site&live=true'+sitebox);
        });

        $("#exportStagingSites").click(function (){
            var selectedSites = [];
            var checkedSites = $("input[name='selectedSites']:checked");
            checkedSites.each(function(){
                selectedSites.push($(this).val());
            });
            if(selectedSites.length==0) {
                alert("${i18nNoSiteSelected}");
                return false;
            }
            var name = selectedSites.length>1?"sites":selectedSites;
            var sitebox = "";
            for (i = 0; i < selectedSites.length; i++) {
                sitebox = sitebox + "&sitebox=" + selectedSites[i];
            }
            $(this).target = "_blank";
            window.open("${url.context}/cms/export/default/"+name+ '_staging_export_${now}.zip?exportformat=site&live=false'+sitebox);
        });
        </c:if>
    })
</script>
<script type="text/javascript" charset="utf-8">
    $(document).ready(function() {
        dataTablesServerSettings.init($('#sitesTable'));
    });
</script>

<div class="page-header">
    <h2>
        <fmt:message key="serverSettings.manageWebProjects.virtualSitesListe"/>
    </h2>
</div>

<form id="sitesForm" action="${flowExecutionUrl}" method="post">
    <div class="panel panel-default">
        <div class="panel-heading">
            <fmt:message key="serverSettings.manageWebProjects.sitesManagement"/>
        </div>
        <div class="panel-body">
            <input type="hidden" id="sitesFormAction" name="_eventId" value="" />
            <a href="#create" id="createSite" class="btn btn-raised btn-primary sitesAction">
                <i class="material-icons">add</i>
                <fmt:message key="serverSettings.manageWebProjects.add"/>
            </a>
            <c:if test="${exportAllowed}">
            <a href="#export" id="exportSites" class="btn btn-raised btn-default sitesAction-hide">
                <i class="material-icons">file_download</i>
                <fmt:message key="label.export"/>
            </a>
            <a href="#exportStaging" id="exportStagingSites" class="btn btn-raised btn-default sitesAction-hide">
                <i class="material-icons">file_download</i>
                <fmt:message key="label.export"/> (<fmt:message key="label.stagingContent"/>)
            </a>
            </c:if>
            <a href="#delete" id="deleteSites" class="btn btn-raised btn-danger sitesAction">
                <i class="material-icons">delete</i>
                <fmt:message key="label.delete"/>
            </a>

            <c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
                <div class="alert ${msg.severity == 'ERROR' ? 'validationError' : ''} ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${fn:escapeXml(msg.text)}
                </div>
            </c:forEach>

            <table id="sitesTable" class="table table-bordered table-striped table-hover">
            <thead>
                    <tr>
                        <th class="{sorter: false}">&nbsp;</th>
                        <th>#</th>
                        <th>
                            <fmt:message key="label.name"/>
                        </th>
                        <th>
                            <fmt:message key="serverSettings.manageWebProjects.webProject.siteKey"/>
                        </th>
                        <th>
                            <fmt:message key="serverSettings.manageWebProjects.webProject.serverName"/>
                        </th>
                        <th>
                            <fmt:message key="serverSettings.manageWebProjects.webProject.templateSet"/>
                        </th>
                        <th class="{sorter: false}">
                            <fmt:message key="label.actions"/>
                        </th>
                    </tr>
                </thead>

                <tbody>
                    <c:forEach items="${webprojectHandler.allSites}" var="site" varStatus="loopStatus">
                        <c:if test="${site.name ne 'systemsite'}">
                            <tr>
                                <td>
                                    <div class="form-group">
                                        <div class="checkbox">
                                            <label>
                                                <input name="selectedSites" type="checkbox" value="${site.name}"/>
                                            </label>
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    ${loopStatus.index + 1}
                                    <c:if test="${site.identifier == defaultSite.string}">
                                        <i class="material-icons">star_rate</i>
                                    </c:if>
                                </td>
                                <td><a href="#edit" onclick="submitSiteForm('editSite', '${site.name}'); return false;">${fn:escapeXml(site.title)}</a></td>
                                <td>${fn:escapeXml(site.name)}</td>
                                <td>${fn:escapeXml(site.serverName)}</td>
                                <td title="${fn:escapeXml(site.templatePackageName)}">${fn:escapeXml(site.templateFolder)}</td>
                                <td>
                                    <div class="btn-group-sm">
                                        <c:set var="i18nExportStaging"><fmt:message key="label.export"/> (<fmt:message key="label.stagingContent"/>)</c:set>
                                        <c:set var="i18nExportStaging" value="${fn:escapeXml(i18nExportStaging)}"/>
                                        <c:if test="${jcr:hasPermission(site,'editModeAccess')}">
                                            <c:choose>
                                                <c:when test="${renderContext.settings.distantPublicationServerMode}">
                                                    <c:url var="editUrl" value="/cms/settings/default/${site.defaultLanguage}${site.path}.manageLanguages.html"/>
                                                    <a style="margin-bottom:0;" class="btn btn-fab btn-default" href="${editUrl}" title="<fmt:message key='serverSettings.manageWebProjects.exitToEdit'/>">
                                                        <i class="material-icons">forward</i>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <jcr:node var="editSite" path="${site.path}"/>
                                                    <c:if test="${not jcr:isNodeType(editSite, 'jmix:remotelyPublished')}">
                                                        <c:url var="editUrl" value="/cms/edit/default/${site.defaultLanguage}${editSite.home.path}.html"/>
                                                        <a style="margin-bottom:0;" class="btn btn-fab btn-default" href="${editUrl}" title="<fmt:message key='serverSettings.manageWebProjects.exitToEdit'/>">
                                                            <i class="material-icons">forward</i>
                                                        </a>
                                                    </c:if>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:if>
                                        <a style="margin-bottom:0;" class="btn btn-fab btn-default" href="#edit" title="<fmt:message key='serverSettings.manageWebProjects.editSite'/>" onclick="submitSiteForm('editSite', '${site.name}'); return false;">
                                            <i class="material-icons">mode_edit</i>
                                        </a>
                                        <%--
                                            <a style="margin-bottom:0;" class="btn btn-fab btn-default" href="#edit" title="<fmt:message key='label.export'/>" onclick="submitSiteForm('exportSites', '${site.name}'); return false;">
                                                <i class="material-icons">file_download</i>
                                            </a>
                                            <a style="margin-bottom:0;" class="btn btn-fab btn-default" href="#edit" title="${i18nExportStaging}" onclick="submitSiteForm('exportStagingSites', '${site.name}'); return false;">
                                                <i class="material-icons">file_download</i>
                                            </a>
                                        --%>
                                        <a style="margin-bottom:0;" class="btn btn-fab btn-danger" title="<fmt:message key='label.delete'/>" href="#delete" onclick="submitSiteForm('deleteSites', '${site.name}'); return false;">
                                            <i class="material-icons">delete</i>
                                        </a>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                </tbody>
            </table>

            <c:if test="${exportAllowed}">
                <div class="form-group label-floating is-empty">
                    <div class="input-group">
                        <label class="control-label"><fmt:message key="serverSettings.manageWebProjects.exportServerDirectory"/></label>
                        <input class="form-control" type="text" name="exportPath"/>
                        <span class="input-group-btn">
                            <a  class="btn btn-default sitesAction" id="exportToFile" href="#exportToFile" title="<fmt:message key='label.export'/>">
                                <i class="material-icons">file_download</i>
                                &nbsp;<fmt:message key='label.export'/>
                            </a>
                            <a  class="btn btn-default sitesAction" id="exportToFileStaging" href="#exportToFileStaging" title="<fmt:message key="label.export"/> (<fmt:message key="label.stagingContent"/>)">
                                <i class="material-icons">file_download</i>
                                &nbsp; <fmt:message key="label.export"/> (<fmt:message key="label.stagingContent"/>)
                            </a>
                        </span>
                    </div>
                </div>
            </c:if>
        </div>
    </div>

    <div class="row">
        <c:if test="${exportAllowed}">
            <div class="col-md-6">
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <fmt:message key="serverSettings.manageWebProjects.systemsite"/>
                    </div>
                    <div class="panel-body">
                        <div class="form-group is-empty">
                            <div class="input-group">
                                <span class="input-group-btn">
                                    <a class="btn btn-default" href="<c:url value='/cms/export/default/systemsite_export_${now}.zip?exportformat=site&live=true&sitebox=systemsite' />">
                                        <i class="material-icons">file_download</i>
                                        <fmt:message key='label.export' />
                                    </a>
                                    <a class="btn btn-default" href="<c:url value='/cms/export/default/systemsite_staging_export_${now}.zip?exportformat=site&live=false&sitebox=systemsite' />">
                                        <i class="material-icons">file_download</i>
                                        <fmt:message key="label.export"/> (<fmt:message key="label.stagingContent"/>)
                                    </a>
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:if>
        <c:set value="${(exportAllowed)?'col-md-6':'col-md-12'}" var="colSizeClass"/>
        <div class="${colSizeClass}">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <fmt:message key="serverSettings.manageWebProjects.importprepackaged"/>
                </div>
                <div class="panel-body">
                    <div class="form-group is-empty">
                        <div class="input-group">
                            <select class="form-control" name="selectedPrepackagedSite">
                                <c:forEach items="${webprojectHandler.prepackagedSites}" var="file">
                                    <option value="${file.key}"${file.value == defaultPrepackagedSite ? ' selected="selected"':''}>${fn:escapeXml(file.value)}</option>
                                </c:forEach>
                            </select>
                            <span class="input-group-btn">
                                <button class="btn btn-raised btn-primary" type="submit" name="importPrepackaged" onclick="submitSiteForm('importPrepackaged'); return false;">
                                    <i class="material-icons">done</i>
                                    &nbsp;<fmt:message key='serverSettings.manageWebProjects.importprepackaged.proceed' />
                                </button>
                            </span>
                        </div>
                        <span class="material-input"></span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</form>

<div class="panel panel-default">
    <div class="panel-heading">
        <fmt:message key="serverSettings.manageWebProjects.multipleimport"/>
    </div>
    <div class="panel-body">
        <form action="${flowExecutionUrl}" method="post" enctype="multipart/form-data">
            <div class="row">
                <div class="col-md-6">
                    <div class="form-group is-empty is-fileinput label-floating">
                        <input type="file" name="importFile"/>
                        <div class="input-group">
                            <span class="input-group-addon"><i class="material-icons">touch_app</i></span>
                            <label class="control-label"><fmt:message key="serverSettings.manageWebProjects.multipleimport.fileselect"/></label>
                            <input class="form-control" type="text" readonly>
                            <span class="input-group-btn">
                                <button class="btn btn-raised btn-primary" type="submit" name="_eventId_import" onclick="">
                                    <i class="material-icons">file_upload</i>
                                    <fmt:message key='serverSettings.manageWebProjects.fileImport'/>
                                </button>
                            </span>
                        </div>
                        <span class="material-input"></span>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="form-group is-empty label-floating">
                        <div class="input-group">
                            <label class="control-label"><fmt:message key="serverSettings.manageWebProjects.multipleimport.fileinput"/></label>
                            <input class="form-control" type="text" name="importPath"/>
                            <span class="input-group-btn">
                                <button class="btn btn-raised btn-primary" type="submit" name="_eventId_import" onclick="">
                                    <i class="material-icons">file_upload</i>
                                    <fmt:message key='serverSettings.manageWebProjects.fileImport'/>
                                </button>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</div>