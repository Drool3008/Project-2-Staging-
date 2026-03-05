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

<h2>&#9733; Your Starred Blog Posts</h2>

<s:if test="entries.isEmpty() && totalEntries == 0">
    <p>You have not starred any blog posts yet.
       Browse a weblog and click the <strong>&#9734; Star this entry</strong>
       link on a post to add it here.</p>
</s:if>
<s:else>
    <table class="table table-striped">
        <thead>
            <tr>
                <th>Title</th>
                <th>Blog</th>
                <th>Published</th>
            </tr>
        </thead>
        <tbody>
            <s:iterator var="entry" value="entries">
                <tr>
                    <td>
                        <a href='<s:property value="#entry.permalink" />'>
                            <s:property value="#entry.title" />
                        </a>
                    </td>
                    <td>
                        <s:property value="#entry.website.name" />
                    </td>
                    <td>
                        <s:date name="#entry.pubTime" format="MMM dd, yyyy HH:mm" />
                    </td>
                </tr>
            </s:iterator>
        </tbody>
    </table>

    <%-- Pagination --%>
    <div class="pager" style="margin-top:10px">
        <s:if test="hasPrevPage">
            <s:url action="starredEntries" var="prevUrl">
                <s:param name="page" value="prevPage"/>
            </s:url>
            <a href="<s:property value='prevUrl'/>" class="btn btn-default btn-sm">&laquo; Previous</a>
        </s:if>
        <span style="margin:0 10px">
            Page <s:property value="page + 1" />
            of <s:property value="(totalEntries + pageSize - 1) / pageSize" />
            &nbsp;(<s:property value="totalEntries"/> entries)
        </span>
        <s:if test="hasNextPage">
            <s:url action="starredEntries" var="nextUrl">
                <s:param name="page" value="nextPage"/>
            </s:url>
            <a href="<s:property value='nextUrl'/>" class="btn btn-default btn-sm">Next &raquo;</a>
        </s:if>
    </div>
</s:else>

<p style="margin-top:20px">
    <a href="<s:url action="menu"/>">&laquo; Back to My Weblogs</a>
</p>
