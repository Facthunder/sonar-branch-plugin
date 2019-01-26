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

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

/**
 * Implementation of BranchConfiguration.
 *
 * @see BranchConfiguration
 */
public class BranchConfigurationImpl implements BranchConfiguration {

    private final BranchType branchType;
    private final String branchName;
    private final String branchTarget;
    private final String branchBase;
    private final String pullRequestKey;

    /**
     * Basic constructor.
     *
     * @param branchType     Type of the branch @see {@link BranchType}.
     * @param branchName     Name of the branch.
     * @param branchTarget   Branch name of the target.
     * @param branchBase     Base branch for pull request.
     * @param pullRequestKey Pull request key.
     */
    BranchConfigurationImpl(final BranchType branchType, final String branchName, final String branchTarget,
                            final String branchBase, final String pullRequestKey) {
        this.branchType = branchType;
        this.branchName = branchName;
        this.branchTarget = branchTarget;
        this.branchBase = branchBase;
        this.pullRequestKey = pullRequestKey;
    }

    /**
     * @see BranchConfiguration
     */
    @Override
    public BranchType branchType() {
        return this.branchType;
    }

    /**
     * @see BranchConfiguration
     */
    @Override
    public String branchName() {
        return this.branchName;
    }

    /**
     * @see BranchConfiguration
     */
    @Override
    public String branchTarget() {
        return this.branchTarget;
    }

    /**
     * @see BranchConfiguration
     */
    @Override
    public String branchBase() {
        return this.branchBase;
    }

    /**
     * @see BranchConfiguration
     */
    @Override
    public String pullRequestKey() {
        if (this.branchType != BranchType.PULL_REQUEST) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request key.");
        } else {
            return this.pullRequestKey;
        }
    }

}
