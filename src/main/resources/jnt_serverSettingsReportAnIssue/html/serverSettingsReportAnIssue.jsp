<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="page-header">
    <h2><fmt:message key="serverSettings.reportAnIssue"/></h2>
</div>

<div class="row">
    <div class="col-md-offset-2 col-md-8">
        <div class="panel panel-default">
            <div class="panel-heading">
                <fmt:message key="serverSettings.reportAnIssue.description"/>
            </div>
            <div class="panel-body text-center">
                <a class="btn btn-primary" href="http://support.jahia.com" target="_blank">
                    <i class="material-icons">exit_to_app</i>
                    <fmt:message key="serverSettings.reportAnIssue.jira"/>
                </a>
            </div>
        </div>
    </div>
</div>
