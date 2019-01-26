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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of Branch.
 *
 * @see Branch
 */
public class BranchImpl implements Branch {

    private static final String BRANCH_KEY_ATTRIBUTE = ":BRANCH:";
    private static final String PR_KEY_ATTRIBUTE = ":PULL_REQUEST:";
    private static final Pattern BRANCH_PATTERN = Pattern.compile(BRANCH_KEY_ATTRIBUTE + "|" + PR_KEY_ATTRIBUTE);
    private final BranchType branchType;
    private final boolean isMasterBranch;
    private final String pullRequestId;
    private final String mergeBranchUuid;
    private final String branchKey;

    /**
     * Constructor for trivial branch request.
     *
     * @param branchType     Type of the branch.
     * @param isMasterBranch True if the branch is the main one (master).
     * @param branchKey      Key of the branch.
     */
    public BranchImpl(final BranchType branchType, final boolean isMasterBranch, final String branchKey) {
        this(branchType, isMasterBranch, branchKey, null, null);
    }

    /**
     * Constructor for complete branch request.
     *
     * @param branchType      Type of the branch.
     * @param isMasterBranch  True if the branch is the main one (master).
     * @param branchKey       Key of the branch.
     * @param mergeBranchUuid UUID of the target merge branch.
     * @param pullRequestId   ID of the pull request.
     */
    public BranchImpl(final BranchType branchType, final boolean isMasterBranch, final String branchKey, final String mergeBranchUuid, final String pullRequestId) {
        this.branchType = branchType;
        this.isMasterBranch = isMasterBranch;
        this.branchKey = branchKey;
        this.mergeBranchUuid = mergeBranchUuid;
        if (branchKey == null && !isMasterBranch) {
            throw new IllegalArgumentException("Branch name must be set");
        } else if (branchKey != null && branchKey.length() > 255) {
            throw MessageException.of(String.format("'%s' is not a valid branch name. Max length is %d characters.", branchKey, 255));
        } else if (branchKey != null && BRANCH_PATTERN.matcher(branchKey).find()) {
            final String branchTypeString = branchType == BranchType.PULL_REQUEST ? "pull request id" : "branch name";
            throw MessageException.of(String.format("'%s' is not a valid %s. It can't contain the sequence '%s' or '%s'.",
                    branchKey, branchTypeString, BRANCH_KEY_ATTRIBUTE, PR_KEY_ATTRIBUTE));
        } else {
            this.pullRequestId = pullRequestId;
        }
    }

    /**
     * @see Branch
     */
    @Override
    public BranchType getType() {
        return this.branchType;
    }

    /**
     * @see Branch
     */
    @Override
    public boolean isMain() {
        return this.isMasterBranch;
    }

    /**
     * @see Branch
     */
    @Override
    public boolean isLegacyFeature() {
        return false;
    }

    /**
     * @see Branch
     */
    @Override
    public String getName() {
        return this.branchKey;
    }

    /**
     * @see Branch
     */
    @Override
    public Optional<String> getMergeBranchUuid() {
        return Optional.ofNullable(this.mergeBranchUuid);
    }

    /**
     * @see Branch
     */
    @Override
    public boolean supportsCrossProjectCpd() {
        return this.isMasterBranch;
    }

    /**
     * @see Branch
     */
    @Override
    public String getPullRequestId() {
        if (this.branchType != BranchType.PULL_REQUEST) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request id.");
        } else {
            return this.pullRequestId;
        }
    }

    /**
     * Generate a key for a component (module, dir or file).
     *
     * @param module    Module object.
     * @param fileOrDir File or Directory object.
     * @return A unique key defined as "componentKey":"branchOrPR":"branchKeyOrPRId".
     */
    @Override
    public String generateKey(final Component module, final Component fileOrDir) {
        String key;
        if (fileOrDir == null) {
            key = module.getKey();
        } else {
            key = ComponentKeys.createEffectiveKey(module.getKey(), StringUtils.trimToNull(fileOrDir.getPath()));
        }

        if (this.isMasterBranch) {
            return key;
        } else {
            String prOrBranch;
            String prIdOrBranchKey;
            if (this.branchType == BranchType.PULL_REQUEST) {
                prOrBranch = PR_KEY_ATTRIBUTE;
                prIdOrBranchKey = this.pullRequestId;
            } else {
                prOrBranch = BRANCH_KEY_ATTRIBUTE;
                prIdOrBranchKey = this.branchKey;
            }

            return String.format("%s%s%s", key, prOrBranch, prIdOrBranchKey);
        }
    }
}
