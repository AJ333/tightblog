<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
  under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.  For additional information regarding
  copyright in this work, please see the NOTICE file in the top level
  directory of this distribution.
--%>
<%@ include file="/WEB-INF/jsps/tightblog-taglibs.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>
<link rel="stylesheet" media="all" href='<s:url value="/tb-ui/jquery-ui-1.11.4/jquery-ui.min.css"/>' />
<script src='<s:url value="/tb-ui/scripts/jquery-2.2.3.min.js" />'></script>
<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular.min.js"></script>
<script src='<s:url value="/tb-ui/jquery-ui-1.11.4/jquery-ui.min.js"/>'></script>
<script>
var contextPath = "${pageContext.request.contextPath}";
var msg= {
    deleteLabel: '<fmt:message key="generic.delete"/>',
    cancelLabel: '<fmt:message key="generic.cancel"/>'
};
</script>

<script src="<s:url value='/tb-ui/scripts/commonjquery.js'/>"></script>
<script src="<s:url value='/tb-ui/scripts/templates.js'/>"></script>

<div id="templates-list" ng-app="TemplatesApp" ng-controller="TemplatesController as ctrl">

    <p class="subtitle">
       <fmt:message key="templates.subtitle" >
           <fmt:param value="${actionWeblog.handle}"/>
       </fmt:message>
    </p>
    <p class="pagetip">
       <fmt:message key="templates.tip" />
    </p>

    <div id="errorMessageDiv" class="errors" style="display:none">
      <b>{{ctrl.errorObj.errorMessage}}</b>
      <ul>
         <li ng-repeat="em in ctrl.errorObj.errors">{{em}}</li>
      </ul>
    </div>

    <div id="successMessageDiv" class="messages" style="display:none">
      <s:if test="weblogId != null">
        <p><fmt:message key="generic.changes.saved"/></p>
      </s:if>
    </div>

    <input id="refreshURL" type="hidden" value="<s:url action='templates'/>?weblogId=<s:property value='%{#parameters.weblogId}'/>"/>
    <input type="hidden" id="actionWeblogId" value="<s:property value='%{#parameters.weblogId}'/>"/>

    <div>

    <fmt:message key="generic.date.toStringFormat" var="dateFormat"/>
    <table class="rollertable">

      <thead>
        <tr>
          <th width="4%"><input type="checkbox" onclick="toggleFunction(this.checked,'idSelections');"
            title="<fmt:message key="templates.selectAllLabel"/>"/></th>
          <th width="17%"><fmt:message key="generic.name"/></th>
          <th width="20%"><fmt:message key="templates.path"/></th>
          <th width="34%"><fmt:message key="templates.role"/></th>
          <th width="8%"><fmt:message key="templates.source"/></th>
          <th width="13%"><fmt:message key="generic.lastModified"/></th>
          <th width="4%"><fmt:message key="generic.view"/></th>
        </tr>
      </thead>
      <tbody>
        <tr ng-repeat="tpl in ctrl.weblogTemplateData.templates" ng-class-even="'altrow'">
            <td class="center" style="vertical-align:middle">
              <span ng-if="tpl.derivation != 'Default'">
                  <input type="checkbox" name="idSelections" value="{{tpl.id}}" />
              </span>
            </td>

            <td style="vertical-align:middle">
                <s:url var="edit" action="templateEdit">
                    <s:param name="weblogId" value="%{actionWeblog.id}" />
                </s:url>
                <span ng-if="tpl.derivation != 'Default'">
                    <s:a href="%{edit}&templateId={{tpl.id}}">{{tpl.name}}</s:a>
                </span>
                <span ng-if="tpl.derivation == 'Default'">
                    <s:a href="%{edit}&templateName={{tpl.name}}">{{tpl.name}}</s:a>
                </span>
            </td>

            <td style="vertical-align:middle">
                <span ng-if="tpl.role.accessibleViaUrl == true">
                  {{tpl.relativePath}}
                </span>
            </td>

            <td style="vertical-align:middle">
              <span ng-if="tpl.role.singleton == true || tpl.description == null || tpl.description == ''">
                {{tpl.role.readableName}}
              </span>
              <span ng-if="tpl.role.singleton != true && tpl.description != null && tpl.description != ''">
                {{tpl.role.readableName}}: {{tpl.description}}
              </span>
            </td>

            <td style="vertical-align:middle">
              {{tpl.derivation}}
            </td>

            <td>
              <span ng-if="tpl.lastModified != null">
                {{tpl.lastModified | date:'short' }}
              </span>
            </td>

            <td align="center" style="vertical-align:middle">
                <span ng-if="tpl.role.accessibleViaUrl && tpl.relativePath != null && tpl.relativePath != ''">
                    <a target="_blank" href="<s:property value='actionWeblog.absoluteURL'/>page/{{tpl.relativePath}}">
                      <img src='<s:url value="/images/world_go.png"/>' border="0" alt="icon"/>
                    </a>
                </span>
            </td>
        </tr>
      </tbody>
    </table>

    <div class="control">
    	<input id="delete-link" type="button" value="<fmt:message key='templates.deleteselected'/>" />

      <span style="float:right">
          <s:form>
            <sec:csrfInput/>
            <s:hidden name="weblogId" value="%{actionWeblog.id}" />
            <s:submit id="switch-theme-button" action="themeEdit" namespace="/tb-ui/authoring" value="%{getText('templates.switchTheme')}" />
          </s:form>
      </span>
    </div>

    <form name="myform">
      <table cellpadding="0" cellspacing="6">
          <caption><fmt:message key="templates.addNewPage" /></caption>
          <tr>
              <td><fmt:message key="generic.name"/></td>
              <td><input type="text" ng-model="ctrl.newTemplateName" maxlength="40" required/></td>
          </tr>
          <tr>
              <td><fmt:message key="templates.role"/></td>
              <td>
                  <select ng-model="ctrl.selectedRole" size="1" required>
                    <option ng-repeat="option in ctrl.weblogTemplateData.availableTemplateRoles" value="{{option.name}}">{{option.readableName}}</option>
                  </select>
              </td>
          </tr>
          <tr>
              <td colspan="2" class="field">
                  <p>{{ description }}</p>
              </td>
          </tr>
          <tr>
              <td><input ng-click="myform.$valid && ctrl.addTemplate()" type="button" value="<fmt:message key='templates.add'/>" required></td>
          </tr>
      </table>
    </form>
  </div>

</div>

<br/>

<div id="confirm-delete" title="<fmt:message key='generic.confirm'/>" style="display:none">
   <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><fmt:message key="templateRemoves.youSure" />
	<br/>
	<br/>
	<span class="warning">
		<fmt:message key="templateRemoves.youSureWarning" />
	</span>
  </p>
</div>
