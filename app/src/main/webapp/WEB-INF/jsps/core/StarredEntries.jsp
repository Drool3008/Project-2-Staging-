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
  limitations under the License.
--%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<h2>&#9733; Your Starred Blogs</h2>

<s:if test="totalWeblogs == 0">
    <p>You have not starred any blogs yet.
       Browse a weblog and click the <strong>&#9734; Star this blog</strong>
       button to add it here.</p>
</s:if>
<s:else>
    <p>You have starred <strong><s:property value="totalWeblogs"/></strong> blog(s).</p>
    
    <table class="table table-striped">
        <thead>
            <tr>
                <th>Blog Name</th>
                <th>Last Updated</th>
            </tr>
        </thead>
        <tbody>
            <s:iterator var="item" value="starredWeblogs">
                <tr>
                    <td>
                        <a href='<s:property value="#item.weblog.URL" />'>
                            <s:property value="#item.weblog.name" />
                        </a>
                    </td>
                    <td>
                        <s:if test="#item.latestPostTime != null">
                            <s:date name="#item.latestPostTime" format="MMM dd, yyyy HH:mm" />
                        </s:if>
                        <s:else>
                            <em>No posts yet</em>
                        </s:else>
                    </td>
                </tr>
            </s:iterator>
        </tbody>
    </table>
</s:else>

<p style="margin-top:20px;">
    <a href="<s:url action="menu"/>">&laquo; Back to My Weblogs</a>
</p>
