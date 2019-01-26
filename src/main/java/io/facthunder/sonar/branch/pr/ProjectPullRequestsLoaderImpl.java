/*
 * This file is part of sonar-branch-plugin.
 *
 * sonar-branch-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sonar-branch-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with sonar-branch-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.facthunder.sonar.branch.pr;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.ProjectPullRequests;
import org.sonar.scanner.scan.branch.ProjectPullRequestsLoader;
import org.sonar.scanner.scan.branch.PullRequestInfo;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectPullRequestsLoaderImpl implements ProjectPullRequestsLoader {
    private static final Logger A = Loggers.get(ProjectPullRequestsLoaderImpl.class);
    private static final String B = "/api/project_pull_requests/list";
    private final ScannerWsClient C;

    public ProjectPullRequestsLoaderImpl(ScannerWsClient var1) {
        this.C = var1;
    }

    public ProjectPullRequests load(String var1) {
        return new ProjectPullRequests(this.B(var1));
    }

    private List<PullRequestInfo> B(String var1) {
        List var2 = Collections.emptyList();
        GetRequest var3 = new GetRequest(A(var1));

        try {
            WsResponse var4 = this.C.call(var3);
            var2 = A(var4);
        } catch (RuntimeException var5) {
            A.debug("Could not process project pull requests - continuing without it");
        } catch (IOException var6) {
            A.debug("Could not parse project pull requests - continuing without it");
        }

        return var2;
    }

    private static String A(String var0) {
        return "/api/project_pull_requests/list?project=" + ScannerUtils.encodeForUrl(var0);
    }

    private static List<PullRequestInfo> A(WsResponse var0) throws IOException {
        Reader var1 = var0.contentReader();
        Throwable var2 = null;

        List var4;
        try {
            ProjectPullRequestsLoaderImpl.WsProjectPullRequestsResponse var3 = GsonHelper.create().fromJson(var1, WsProjectPullRequestsResponse.class);
            var4 = var3.pullRequests.stream().map(var0x -> new PullRequestInfo(var0x.key, var0x.branch, var0x.base, var0x.analysisDate.getTime())).collect(Collectors.toList());
        } catch (Throwable var13) {
            var2 = var13;
            throw var13;
        } finally {
            if (var1 != null) {
                if (var2 != null) {
                    try {
                        var1.close();
                    } catch (Throwable var12) {
                        var2.addSuppressed(var12);
                    }
                } else {
                    var1.close();
                }
            }

        }

        return var4;
    }

    private static class WsProjectPullRequestsResponse {
        private List<ProjectPullRequestsLoaderImpl.WsProjectPullRequest> pullRequests = new ArrayList();

        private WsProjectPullRequestsResponse() {
        }
    }

    private static class WsProjectPullRequest {
        private String key;
        private String branch;
        private String base;
        private Date analysisDate;

        private WsProjectPullRequest() {
        }
    }
}
