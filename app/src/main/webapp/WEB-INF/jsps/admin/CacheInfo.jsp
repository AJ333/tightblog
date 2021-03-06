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

  Source file modified from the original ASF source; all changes made
  are also under Apache License.
--%>
<%@ include file="/WEB-INF/jsps/tightblog-taglibs.jsp" %>
<link rel="stylesheet" media="all" href='<c:url value="/tb-ui/jquery-ui-1.11.4/jquery-ui.min.css"/>'/>
<script src="<c:url value='/tb-ui/scripts/jquery-2.2.3.min.js'/>"></script>
<script src="<c:url value='/tb-ui/jquery-ui-1.11.4/jquery-ui.min.js'/>"></script>
<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.6.1/angular.min.js"></script>

<script>
    var contextPath = "${pageContext.request.contextPath}";
    var msg = {
        confirmLabel: '<fmt:message key="generic.confirm"/>',
        cancelLabel: '<fmt:message key="generic.cancel"/>',
    };
</script>

<script src="<c:url value='/tb-ui/scripts/commonangular.js'/>"></script>
<script src="<c:url value='/tb-ui/scripts/cacheInfo.js'/>"></script>

<input id="refreshURL" type="hidden" value="<c:url value='/tb-ui/app/admin/cacheInfo'/>"/>

<div id="successMessageDiv" class="messages" ng-show="ctrl.successMessage" ng-cloak>
    <p>{{ctrl.successMessage}}</p>
</div>

<div id="errorMessageDiv" class="errors" ng-show="ctrl.errorMessage" ng-cloak>
    <p>{{ctrl.errorMessage}}</p>
</div>

<p class="subtitle">
    <fmt:message key="cacheInfo.subtitle" />
<p>

<p><fmt:message key="cacheInfo.explanation"/></p>

<br style="clear:left"/>

<table class="rollertable">
<thead>
   <tr>
        <th style="width:20%"><fmt:message key="generic.name"/></th>
        <th style="width:20%"><fmt:message key="cacheInfo.startTime"/></th>
        <th style="width:10%"><fmt:message key="cacheInfo.puts"/></th>
        <th style="width:10%"><fmt:message key="cacheInfo.removes"/></th>
        <th style="width:10%"><fmt:message key="cacheInfo.hits"/></th>
        <th style="width:10%"><fmt:message key="cacheInfo.misses"/></th>
        <th style="width:10%"><fmt:message key="cacheInfo.efficiency"/></th>
        <th style="width:10%"></th>
    </tr>
</thead>
<tbody id="tableBody" ng-cloak>
      <tr ng-repeat="(key,item) in ctrl.cacheData" ng-class-even="'altrow'">
        <td>{{key}}</td>
        <td>{{item.startTime | date:'short'}}</td>
        <td>{{item.puts}}</td>
        <td>{{item.removes}}</td>
        <td>{{item.hits}}</td>
        <td>{{item.misses}}</td>
        <td>{{item.efficiency}}</td>
        <td align="center">
            <input type="button" value="<fmt:message key='cacheInfo.clear'/>" ng-click="ctrl.clearCache(key)"/>
        </td>
       </tr>
</tbody>
</table>

<div class="control clearfix">
  <input ng-click="ctrl.loadItems()" type="button" value="<fmt:message key='generic.refresh'/>"/>
  <input confirm-clear-all-dialog="confirm-clearall" type="button" value="<fmt:message key='cacheInfo.clearAll'/>"/>
</div>

<br><br>
<fmt:message key="maintenance.prompt.reset"/>:
<br><br>
<input ng-click="ctrl.resetHitCounts()" type="button" value="<fmt:message key='maintenance.button.reset'/>"/>

<br><br>
<fmt:message key="maintenance.prompt.index"/>:
<br><br>
<select ng-model="ctrl.weblogToReindex" size="1" required>
    <option ng-repeat="(key, value) in ctrl.metadata.weblogList" value="{{value}}">{{value}}</option>
</select>
<input ng-click="ctrl.reindexWeblog()" type="button" value="<fmt:message key='maintenance.button.index'/>"/>
<br><br>

<div id="confirm-clearall" title="<fmt:message key='generic.confirm'/>" style="display:none">
   <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
   <fmt:message key='cacheInfo.confirmResetAll'/></p>
</div>
