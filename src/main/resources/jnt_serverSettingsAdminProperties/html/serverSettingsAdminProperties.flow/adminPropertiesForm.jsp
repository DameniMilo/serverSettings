<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<jcr:node path="/users/root" var="adminUser"/>

<div class="page-header">
    <h2><fmt:message key="serverSettings.adminProperties"/></h2>
</div>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-danger' : 'alert-success'}"><button type="button" class="close" data-dismiss="alert">&times;</button>${fn:escapeXml(msg.text)}</div>
</c:forEach>

<div class="row">
    <div class="col-md-offset-2 col-md-8">
        <div class="panel panel-default">
            <div class="panel-heading">
                <fmt:message key="label.username"/>:&nbsp;${adminUser.name}
            </div>
            <div class="panel-body">
                <form:form modelAttribute="adminProperties" class="form" autocomplete="off">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="firstName">
                                    <fmt:message key="label.firstName"/>
                                </label>
                                <form:input class="form-control" type="text" id="firstName" path="firstName"/>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="lastName">
                                    <fmt:message key="label.lastName"/>
                                </label>
                                <form:input class="form-control" type="text" id="lastName" path="lastName"/>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="email">
                                    <fmt:message key="label.email"/>
                                </label>
                                <form:input class="form-control" type="text" id="email" path="email"/>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="organization">
                                    <fmt:message key="label.organization"/>
                                </label>
                                <form:input type="text" class="form-control" id="organization" path="organization" autocomplete="off"/>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="checkbox">
                                <label for="emailNotifications">
                                    <form:checkbox cssClass="input-sm" id="emailNotifications" path="emailNotificationsDisabled" />
                                    <fmt:message key="siteSettings.user.emailNotifications"/>
                                </label>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="preferredLanguage">
                                    <fmt:message key="siteSettings.user.preferredLanguage"/>
                                </label>
                                <select class="form-control" id="preferredLanguage" name="preferredLanguage" size="1">
                                    <c:forEach items="${functions:availableAdminBundleLocale(renderContext.UILocale)}" var="uiLanguage">
                                        <option value="${uiLanguage}" <c:if test="${uiLanguage eq adminProperties.preferredLanguage}">selected="selected" </c:if>>${functions:displayLocaleNameWith(uiLanguage, renderContext.UILocale)}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="password">
                                    <fmt:message key="label.password"/>
                                </label>
                                <form:input class="form-control" type="password" id="password" path="password" autocomplete="off"/>
                                <span class="help-block text-info"><fmt:message key="siteSettings.user.edit.password.no.change"/></span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="form-group form-group-sm label-floating">
                                <label class="control-label" for="passwordConfirm">
                                    <fmt:message key="label.confirmPassword"/>
                                </label>
                                <form:input type="password" class="form-control" id="passwordConfirm" path="passwordConfirm" autocomplete="off"/>
                                <span class="help-block text-info"><fmt:message key="siteSettings.user.edit.password.no.change"/></span>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-md-12">
                            <div class="form-group form-group-sm" style="margin-top:15px;">
                                <button class="btn btn-sm btn-primary pull-right" id="submit" type="submit" name="_eventId_submit">
                                    <i class="material-icons">save</i>
                                    <fmt:message key='label.save'/>
                                </button>
                            </div>
                        </div>
                    </div>
                </form:form>

                <hr/>
                <fieldset id="groupsFields" title="<fmt:message key="siteSettings.user.groups.list"/>">
                    <div class="form-group">
                        <label class="col-md-2 control-label" for="groupsFields">
                            <fmt:message key="siteSettings.user.groups.list"/>
                        </label>
                        <div class="col-md-10">
                            <select class="form-control" name="selectMember" size="6" multiple>
                                <c:forEach items="${userGroups}" var="group">
                                    <option value="${user:formatUserValueOption(group)}">${user:formatUserTextOption(group, 'Name, 20;SiteTitle, 15;Properties, 20')}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                </fieldset>
            </div>
        </div>
    </div>
</div>
