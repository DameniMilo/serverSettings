<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="page-header">
    <h2><fmt:message key="serverSettings.manageWebProjects.createWebProject"/></h2>
</div>

<c:if test="${!empty flowRequestContext.messageContext.allMessages}">
    <c:forEach var="error" items="${flowRequestContext.messageContext.allMessages}">
        <div class="alert alert-danger">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
                ${error.text}
        </div>
    </c:forEach>
</c:if>

<div class="col-md-6 col-md-offset-3">
    <div class="panel panel-default">
        <div class="panel-body">
            <form action="${flowExecutionUrl}" method="POST">
                <div class="form-group form-group-sm label-floating">
                    <label class="control-label" for="username"><fmt:message key="label.username"/></label>
                    <input class="form-control" type="text" id="username" value="${siteBean.adminProperties.username}" name="username"/>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="firstName"><fmt:message key="label.firstName"/></label>
                            <input class="form-control" type="text" id="firstName" value="${siteBean.adminProperties.firstName}" name="firstName"/>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="lastName"><fmt:message key="label.lastName"/></label>
                            <input class="form-control" type="text" id="lastName" value="${siteBean.adminProperties.lastName}" name="lastName"/>
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="email"><fmt:message key="label.email"/></label>
                            <input class="form-control" type="text" id="email" value="${siteBean.adminProperties.email}" name="email"/>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="organization"><fmt:message key="label.organization"/></label>
                            <input class="form-control" type="text" id="organization" value="${siteBean.adminProperties.organization}" name="organization" />
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="password"><fmt:message key="label.password"/></label>
                            <input class="form-control" type="password" id="password" name="password" autocomplete="off"/>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="form-group form-group-sm label-floating is-empty">
                            <label class="control-label" for="passwordConfirm"><fmt:message key="label.confirmPassword"/></label>
                            <input class="form-control" type="password" id="passwordConfirm" name="passwordConfirm" autocomplete="off"/>
                        </div>
                    </div>
                </div>

                <div class="form-group form-group-sm">
                    <button class="btn btn-sm btn-default" type="submit" name="_eventId_previous">
                        <i class="material-icons">navigate_before</i>
                        <fmt:message key='label.previous'/>
                    </button>
                    <button class="btn btn-sm btn-primary pull-right" type="submit" name="_eventId_next">
                        <fmt:message key='label.next'/>
                        <i class="material-icons">navigate_next</i>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>