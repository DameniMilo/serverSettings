<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
<template:addResources type="javascript" resources="jquery.min.js"/>

<c:if test="${not empty threadDumpResult}">
    <div class="panel panel-default">
        <div class="panel-body">
            <form action="${flowExecutionUrl}" method="POST">
                <button class="btn btn-default" type="submit" name="_eventId_back">
                    <i class="material-icons">arrow_back</i>
                    <fmt:message key='serverSettings.manageMemory.memory.back'/>
                </button>
            </form>

            <pre>
                <code class="java">
                    ${fn:escapeXml(threadDumpResult)}
                </code>
            </pre>
        </div>
    </div>
</c:if>