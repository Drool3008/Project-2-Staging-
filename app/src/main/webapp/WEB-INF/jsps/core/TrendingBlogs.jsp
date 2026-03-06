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

<h2>&#128293; <s:text name="trendingBlogs.title"/></h2>
<p class="subtitle"><s:text name="trendingBlogs.subtitle"/></p>

<%-- ==================== Top 5 Trending Blog Posts ==================== --%>
<h3>&#9733; <s:text name="trendingBlogs.posts.heading"/></h3>
<s:if test="trendingEntries.isEmpty()">
    <p><em><s:text name="trendingBlogs.noEntries"/></em></p>
</s:if>
<s:else>
    <table class="table table-striped table-hover">
        <thead>
            <tr>
                <th>#</th>
                <th><s:text name="trendingBlogs.col.title"/></th>
                <th><s:text name="trendingBlogs.col.blog"/></th>
                <th><s:text name="trendingBlogs.col.stars"/></th>
            </tr>
        </thead>
        <tbody>
            <%-- trendingEntries is List<Object[]>: [0]=WeblogEntry, [1]=Long starCount --%>
            <s:iterator var="row" value="trendingEntries" status="stat">
                <tr>
                    <td><s:property value="#stat.count"/></td>
                    <td>
                        <a href='<s:property value="#row[0].permalink"/>' target="_blank">
                            <s:property value="#row[0].title"/>
                        </a>
                    </td>
                    <td>
                        <a href='<s:property value="#row[0].website.URL"/>' target="_blank">
                            <s:property value="#row[0].website.name"/>
                        </a>
                    </td>
                    <td>
                        <span class="badge"><s:property value="#row[1]"/></span>
                        &#9733;
                    </td>
                </tr>
            </s:iterator>
        </tbody>
    </table>
</s:else>

<%-- ==================== Top 5 Trending Blog Pages ==================== --%>
<h3>&#9733; <s:text name="trendingBlogs.pages.heading"/></h3>
<s:if test="trendingWeblogs.isEmpty()">
    <p><em><s:text name="trendingBlogs.noWeblogs"/></em></p>
</s:if>
<s:else>
    <table class="table table-striped table-hover">
        <thead>
            <tr>
                <th>#</th>
                <th><s:text name="trendingBlogs.col.blogName"/></th>
                <th><s:text name="trendingBlogs.col.stars"/></th>
            </tr>
        </thead>
        <tbody>
            <%-- trendingWeblogs is List<Object[]>: [0]=Weblog, [1]=Long starCount --%>
            <s:iterator var="row" value="trendingWeblogs" status="stat">
                <tr>
                    <td><s:property value="#stat.count"/></td>
                    <td>
                        <a href='<s:property value="#row[0].URL"/>' target="_blank">
                            <s:property value="#row[0].name"/>
                        </a>
                    </td>
                    <td>
                        <span class="badge"><s:property value="#row[1]"/></span>
                        &#9733;
                    </td>
                </tr>
            </s:iterator>
        </tbody>
    </table>
</s:else>

<p style="margin-top:15px;">
    <a href="<s:url action="menu"/>" class="btn btn-default">
        <span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>
        <s:text name="trendingBlogs.backToMenu"/>
    </a>
</p>
