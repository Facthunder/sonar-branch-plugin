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

import io.facthunder.sonar.branch.pr.ProjectPullRequestsLoaderImpl;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;

/**
 * Define extensions implemented by this plugin.
 * This plugin reimplement branch features.
 */
public final class BranchPlugin implements Plugin {

    /**
     * Default empty constructor.
     */
    public BranchPlugin() {

    }

    /**
     * Define extensions implemented by this plugin.
     * @param context Give context information on SonarQube instance.
     */
    public void define(final Context context) {

        final SonarQubeSide sonarQubeSide = context.getRuntime().getSonarQubeSide();

        if (sonarQubeSide == SonarQubeSide.COMPUTE_ENGINE) {
            context.addExtension(ReportAnalysisComponentProviderImpl.class);
        } else if (sonarQubeSide == SonarQubeSide.SCANNER) {
            context.addExtension(BranchParamsValidatorImpl.class);
            context.addExtension(BranchConfigurationLoaderImpl.class);
            context.addExtension(ProjectBranchesLoaderImpl.class);
            context.addExtension(ProjectPullRequestsLoaderImpl.class);
        } else if (sonarQubeSide == SonarQubeSide.SERVER) {
            context.addExtension(BranchFeatureExtensionImpl.class);
        }

        context.addExtensions(BranchPropertyDefinition.getPropertiesDefinition());

    }
}