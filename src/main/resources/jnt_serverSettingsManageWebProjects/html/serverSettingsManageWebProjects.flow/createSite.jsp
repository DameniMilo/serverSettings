<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<template:addResources type="javascript" resources="jquery.min.js,admin-bootstrap.js"/>

<c:if test="${!empty flowRequestContext.messageContext.allMessages}">
            <c:forEach var="error" items="${flowRequestContext.messageContext.allMessages}">
                <div class="alert alert-error">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                        ${fn:escapeXml(error.text)}
                </div>
            </c:forEach>
</c:if>
<div class="page-header">
    <h2><fmt:message key="serverSettings.manageWebProjects.createWebProject"/></h2>
</div>

<div class="row">
    <div class="col-md-6 col-md-offset-3">
        <form id="createSiteForm" action="${flowExecutionUrl}" method="POST">
            <div class="panel panel-default">
                <div class="panel-body">

                    <div class="form-group label-floating">
                        <label class="control-label" for="title">
                            <fmt:message key="label.name"/><strong class="text-danger">*</strong>
                        </label>
                        <input class="form-control" type="text" id="title" name="title" value="${fn:escapeXml(siteBean.title)}"/>
                    </div>

                    <div class="form-group label-floating">
                        <label class="control-label" for="serverName">
                            <fmt:message key="serverSettings.manageWebProjects.webProject.serverName"/> <strong class="text-danger">*</strong>
                        </label>
                        <input class="form-control" type="text" id="serverName" name="serverName" value="${fn:escapeXml(siteBean.serverName)}"/>
                    </div>

                    <div class="form-group label-floating">
                        <label class="control-label" for="siteKey">
                            <fmt:message key="serverSettings.manageWebProjects.webProject.siteKey"/> <strong class="text-danger">*</strong>
                        </label>
                        <input class="form-control" type="text" id="siteKey" name="siteKey" value="${fn:escapeXml(siteBean.siteKey)}"/>
                    </div>

                    <div class="form-group label-floating is-empty">
                        <label class="control-label" for="description"><fmt:message key="label.description"/></label>
                        <textarea class="form-control" id="description" name="description">${fn:escapeXml(siteBean.description)}</textarea>
                    </div>

                    <div class="form-group">
                        <div class="checkbox">
                            <label for="defaultSite">
                                <c:if test="${numberOfSites > 0}">
                                    <input type="checkbox" name="defaultSite" id="defaultSite" <c:if test="${siteBean.defaultSite}">checked="checked"</c:if> /> <fmt:message key="serverSettings.manageWebProjects.webProject.defaultSite"/>
                                </c:if>
                                <c:if test="${numberOfSites == 0}">
                                    <input type="checkbox" name="defaultSite" id="defaultSite" disabled="disabled" checked="checked"/> <fmt:message key="serverSettings.manageWebProjects.webProject.isDefault"/>
                                </c:if>
                            </label>
                        </div>
                        <input type="hidden" name="_defaultSite"/>
                    </div>

                    <div class="form-group">
                        <div class="checkbox">
                            <label for="createAdmin">
                                <input type="checkbox" name="createAdmin" id="createAdmin" <c:if test="${siteBean.createAdmin}">checked="checked"</c:if> /> <fmt:message key="serverSettings.manageWebProjects.webProject.createAdmin"/>
                            </label>
                        </div>
                        <input type="hidden" name="_createAdmin"/>
                    </div>

                    <div class="form-group">
                        <button class="btn btn-primary" type="submit" name="_eventId_next">
                            <i class="icon-chevron-right icon-white"></i>
                            &nbsp;<fmt:message key='label.next'/>
                        </button>
                        <button class="btn" type="submit" name="_eventId_cancel">
                            <i class="icon-ban-circle"></i>
                            &nbsp;<fmt:message key='label.cancel' />
                        </button>
                    </div>
                </div>
            </div>
        </form>
    </div>
</div>