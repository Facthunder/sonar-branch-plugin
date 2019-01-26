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
package io.facthunder.sonar.branch;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

/**
 * Implementation of {@link ProjectBranchesLoader}.
 */
public class ProjectBranchesLoaderImpl implements ProjectBranchesLoader {

    private static final Logger LOGGER = Loggers.get(ProjectBranchesLoaderImpl.class);
    private final ScannerWsClient scannerWsClient;

    public ProjectBranchesLoaderImpl(final ScannerWsClient wsClient) {
        this.scannerWsClient = wsClient;
    }

    /**
     * @see ProjectBranchesLoader
     */
    @Override
    public ProjectBranches load(final String projectKey) {
        return new ProjectBranches(this.getBranchesInfo(projectKey));
    }

    private List<BranchInfo> getBranchesInfo(final String projectKey) {
        List<BranchInfo> branches = Collections.emptyList();
        final GetRequest request = new GetRequest(prepareRequest(projectKey));

        try {
            WsResponse response = this.scannerWsClient.call(request);
            branches = getBranchesInfo(response);
        } catch (final RuntimeException runtimeExtension) {
            LOGGER.debug("Could not process project branches - continuing without it");
        } catch (final IOException ioException) {
            LOGGER.debug("Could not parse project branches - continuing without it");
        }

        return branches;
    }

    private static String prepareRequest(final String var0) {
        return "/api/project_branches/list?project=" + ScannerUtils.encodeForUrl(var0);
    }

    private static List<BranchInfo> getBranchesInfo(final WsResponse response) throws IOException {
        final Reader responseReader = response.contentReader();
        Throwable throwable = null;

        List<BranchInfo> branchesInfo;
        try {
            ProjectBranchesLoaderImpl.WsProjectBranchesResponse wsProjectBranchesResponse = GsonHelper.create().fromJson(responseReader, WsProjectBranchesResponse.class);
            branchesInfo = wsProjectBranchesResponse.branches.stream().map(branch -> new BranchInfo(branch.name, getBranchType(branch.type), branch.isMain, branch.mergeBranch)).collect(Collectors.toList());
        } catch (final Throwable caughtThrowable) {
            throwable = caughtThrowable;
            throw caughtThrowable;
        } finally {
            if (responseReader != null) {
                if (throwable != null) {
                    try {
                        responseReader.close();
                    } catch (final Throwable caughtThrowable) {
                        throwable.addSuppressed(caughtThrowable);
                    }
                } else {
                    responseReader.close();
                }
            }

        }

        return branchesInfo;
    }

    private static BranchType getBranchType(final String branchType) {
        if ("LONG".equals(branchType)) {
            return BranchType.LONG;
        } else if ("SHORT".equals(branchType)) {
            return BranchType.SHORT;
        } else if ("PULL_REQUEST".equals(branchType)) {
            return BranchType.PULL_REQUEST;
        } else {
            throw new UnsupportedOperationException("Unsupported branch type: " + branchType);
        }
    }

    private static class WsProjectBranchesResponse {
        private List<ProjectBranchesLoaderImpl.WsProjectBranch> branches = new ArrayList();

        private WsProjectBranchesResponse() {
        }
    }

    private static class WsProjectBranch {
        private String name;
        private String type;
        private boolean isMain;
        private String mergeBranch;

        private WsProjectBranch() {
        }
    }
}
