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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.scan.branch.*;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Implementation of BranchConfigurationLoader.
 * Load branch configuration.
 *
 * @see BranchConfigurationLoader
 */
public class BranchConfigurationLoaderImpl implements BranchConfigurationLoader {

    /**
     * Logger of this class.
     **/
    private static final Logger LOGGER = Loggers.get(BranchConfigurationLoaderImpl.class);
    /**
     * Set of possible sonar properties for branch management.
     **/
    private static final Set<String> BRANCH_PROPERTIES_SET = ImmutableSet.of("sonar.branch.name", "sonar.branch.target");
    /**
     * Set of possible sonar properties for pull request management.
     **/
    private static final Set<String> PR_PROPERTIES_SET = ImmutableSet.of("sonar.pullrequest.key", "sonar.pullrequest.branch", "sonar.pullrequest.base");

    /**
     * Default empty constructor.
     */
    public BranchConfigurationLoaderImpl() {

    }

    /**
     * @see BranchConfigurationLoader
     */
    @Override
    public BranchConfiguration load(final Map<String, String> map, Supplier<Map<String, String>> supplier,
                                    final ProjectBranches projectBranches,
                                    final ProjectPullRequests projectPullRequests) {
        final boolean var5 = PR_PROPERTIES_SET.stream().anyMatch(var1x -> StringUtils.trimToNull(map.get(var1x)) != null);
        final String branchName;
        final String targetName;
        if (var5) {
            checkPropertiesForPullRequestAnalysis(map);
            final String pullRequestKey = StringUtils.trimToNull(map.get("sonar.pullrequest.key"));
            branchName = StringUtils.trimToNull(map.get("sonar.pullrequest.branch"));
            targetName = StringUtils.trimToNull(map.get("sonar.pullrequest.base"));
            return getBranchConfiguration(pullRequestKey, branchName, targetName, projectBranches, projectPullRequests);
        } else {
            final boolean var6 = BRANCH_PROPERTIES_SET.stream().anyMatch(var1x -> StringUtils.trimToNull(map.get(var1x)) != null);
            if (var6) {
                branchName = StringUtils.trimToNull(map.get("sonar.branch.name"));
                targetName = StringUtils.trimToNull(map.get("sonar.branch.target"));
                checkPropertiesForBranchAnalysis(map);
                return getBranchConfiguration(branchName, targetName, supplier, projectBranches);
            } else {
                return new DefaultBranchConfiguration();
            }
        }
    }

    private static BranchConfiguration getBranchConfiguration(final String branchName, String branchTarget,
                                                              final Supplier<Map<String, String>> supplier, final ProjectBranches projectBranches) {
        if (projectBranches.isEmpty()) {
            throw MessageException.of("Project was never analyzed. A regular analysis is required before a branch analysis");
        } else {
            String branchBase = branchTarget;
            final BranchInfo branchInfo = projectBranches.get(branchName);
            final BranchType branchType;
            if (branchInfo != null) {
                if (branchInfo.isMain() && branchTarget != null) {
                    throw MessageException.of("The main branch must not have a target");
                }

                branchType = branchInfo.type();
                if (branchType == BranchType.LONG) {
                    branchBase = branchName;
                }
            } else {
                branchType = getBranchTypeFromBranchName(supplier, branchName);
            }

            branchTarget = getTargetBranchName(branchTarget, projectBranches);
            return new BranchConfigurationImpl(branchType, branchName, branchTarget, branchBase, null);
        }
    }

    private static BranchConfiguration getBranchConfiguration(final String pullRequestKey, final String branchName, String branchBase,
                                                              final ProjectBranches projectBranches, final ProjectPullRequests projectPullRequests) {
        if (projectBranches.isEmpty()) {
            throw MessageException.of("Project was never analyzed. A regular analysis is required before analyzing a pull request");
        } else {
            final String branchTarget = branchBase;
            final BranchType branchType = BranchType.PULL_REQUEST;
            if (branchBase != null) {
                final boolean branchExists = projectBranches.get(branchBase) != null;
                branchBase = branchExists ? getTargetBranchName(branchBase, projectBranches) : getBaseBranchName(branchBase, projectBranches, projectPullRequests);
            }

            return new BranchConfigurationImpl(branchType, branchName, branchBase, branchTarget, pullRequestKey);
        }
    }

    private static void checkPropertiesForBranchAnalysis(Map<String, String> config) {
        final Stream propertyKeys = config.keySet().stream();
        final Set<String> pullRequestsPropertiesSet = PR_PROPERTIES_SET;
        propertyKeys.filter(pullRequestsPropertiesSet::contains).findAny().ifPresent(parameter -> {
            throw MessageException.of(String.format("A branch analysis cannot have the pull request analysis parameter '%s'", parameter));
        });
        if (StringUtils.trimToNull(config.get("sonar.branch.name")) == null) {
            throw MessageException.of(String.format("Parameter '%s' is mandatory for a branch analysis", "sonar.branch.name"));
        }
    }

    private static void checkPropertiesForPullRequestAnalysis(Map<String, String> config) {
        final Stream propertyKeys = config.keySet().stream();
        final Set<String> branchPropertiesSet = BRANCH_PROPERTIES_SET;
        propertyKeys.filter(branchPropertiesSet::contains).findAny().ifPresent(property -> {
            throw MessageException.of(String.format("A pull request analysis cannot have the branch analysis parameter '%s'", property));
        });
        Stream.of("sonar.pullrequest.key", "sonar.pullrequest.branch").filter(var1 -> StringUtils.trimToNull(config.get(var1)) == null)
                .findAny().ifPresent(property -> {
            throw MessageException.of(String.format("Parameter '%s' is mandatory for a pull request analysis", property));
        });
    }

    private static String getTargetBranchName(final String branchName, final ProjectBranches projectBranches) {
        if (branchName == null) {
            return null;
        } else {
            BranchInfo branchInfo = getBranchInfo(projectBranches, branchName);
            if (branchInfo.type() == BranchType.LONG) {
                return branchName;
            } else {
                LOGGER.info("The merge branch is not a long branch. Resolving to its base instead: '{}'", branchInfo.name());
                String branchTargetName = branchInfo.branchTargetName();
                if (branchTargetName == null) {
                    throw MessageException.of("Illegal state: the merge branch was expected to have a base: " + branchInfo.name());
                } else {
                    branchInfo = getBranchInfo(projectBranches, branchTargetName);
                    if (branchInfo.type() != BranchType.LONG) {
                        throw MessageException.of("Illegal state: the base of the merge branch was expected to be long: " + branchName);
                    } else {
                        return branchInfo.name();
                    }
                }
            }
        }
    }

    private static String getBaseBranchName(final String var0, final ProjectBranches projectBranches, final ProjectPullRequests pullRequests) {
        final PullRequestInfo pullRequestInfo = getPullRequestInfo(pullRequests, var0);
        LOGGER.info("The merge branch is a pull request. Resolving to its base instead: '{}'", pullRequestInfo.getBranch());
        final String baseBranch = pullRequestInfo.getBase();
        if (baseBranch == null) {
            throw MessageException.of("Illegal state: the pull request was expected to have a base: " + pullRequestInfo.getBranch());
        } else {
            final BranchInfo baseBranchInfo = getBranchInfo(projectBranches, baseBranch);
            if (baseBranchInfo.type() != BranchType.LONG) {
                throw MessageException.of("Illegal state: the base of the merge branch was expected to be long: " + var0);
            } else {
                return baseBranchInfo.name();
            }
        }
    }

    private static BranchInfo getBranchInfo(final ProjectBranches projectBranches, final String branchName) {
        final BranchInfo branchInfo = projectBranches.get(branchName);
        if (branchInfo == null) {
            throw MessageException.of("Branch does not exist on server: " + branchName);
        } else {
            return branchInfo;
        }
    }

    private static PullRequestInfo getPullRequestInfo(final ProjectPullRequests projectPullRequests, final String pullRequestName) {
        final PullRequestInfo pullRequestInfo = projectPullRequests.get(pullRequestName);
        if (pullRequestInfo == null) {
            throw MessageException.of("Pull request with branch does not exist on server: " + pullRequestName);
        } else {
            return pullRequestInfo;
        }
    }

    private static BranchType getBranchTypeFromBranchName(final Supplier<Map<String, String>> supplier, final String branchName) {
        final Map<String, String> config = supplier.get();
        String longBranchRegex = config.get("sonar.branch.longLivedBranches.regex");
        if (longBranchRegex == null) {
            longBranchRegex = "(branch|release)-.*";
        }

        return branchName.matches(longBranchRegex) ? BranchType.LONG : BranchType.SHORT;
    }
}
