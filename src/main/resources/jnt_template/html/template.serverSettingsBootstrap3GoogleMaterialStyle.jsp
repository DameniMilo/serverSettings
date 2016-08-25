<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<html lang="${fn:substring(renderContext.request.locale,0,2)}">
<head>
    <meta charset="UTF-8">
    <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:description" inherited="true" var="description"/>
    <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:createdBy" inherited="true" var="author"/>
    <c:set var="keywords" value="${jcr:getKeywords(renderContext.mainResource.node, true)}"/>
    <c:if test="${!empty description}"><meta name="description" content="${description.string}" /></c:if>
    <c:if test="${!empty author}"><meta name="author" content="${author.string}" /></c:if>
    <c:if test="${!empty keywords}"><meta name="keywords" content="${keywords}" /></c:if>
    <title>${fn:escapeXml(renderContext.mainResource.node.displayableName)}</title>
    <template:addResources type="css" resources="bootstrap3.serverSettings.min.css"/>
    <template:addResources type="css" resources="bootstrap-material-design.css"/>
    <template:addResources type="css" resources="ripples.css"/>
    <template:addResources type="css" resources="roboto-fonts.css"/>
    <template:addResources type="css" resources="material-icons.css"/>
    <template:addResources type="css" resources="serverSettings.css"/>
    <template:addResources type="css" resources="snackbar.css"/>
    <template:addResources type="css" resources="snackbar-material.css"/>
    <template:addResources>
        <script>
            $.material.init();
            $(document).ready(function() {
                $.material.init();
                $('[data-toggle="tooltip"]').tooltip()
            });
        </script>
    </template:addResources>
</head>

<body id="serverSettings">
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <template:area path="pagecontent"/>
        </div>
    </div>
</div>
<div class="clearfix">
    <p class="text-center text-muted"><fmt:message key="jahia.copyright" />&nbsp;-&nbsp;<fmt:message key="jahia.company" /></p>
</div>
<template:addResources type="javascript" resources="jquery.min.js"/>
<template:addResources type="javascript" resources="bootstrap3.serverSettings.min.js"/>
<template:addResources type="javascript" resources="material.js"/>
<template:addResources type="javascript" resources="ripples.js"/>
<template:addResources type="javascript" resources="snackbar.min.js"/>
<template:theme/>

</body>
</html>
