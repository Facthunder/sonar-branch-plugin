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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.BranchLoaderDelegate;
import org.sonar.server.project.Project;

import java.util.Optional;

/**
 * Implementation of BranchLoaderDelegate.
 *
 * @see BranchLoaderDelegate
 */
public class BranchLoaderDelegateImpl implements BranchLoaderDelegate {
    private final DbClient dbClient;
    private final MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder;

    /**
     * Constructor.
     *
     * @param dbClient                      Client to join the database.
     * @param mutableAnalysisMetadataHolder Service for handle metadata of analysis.
     */
    public BranchLoaderDelegateImpl(final DbClient dbClient, final MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder) {
        this.dbClient = dbClient;
        this.mutableAnalysisMetadataHolder = mutableAnalysisMetadataHolder;
    }

    /**
     * Implement BranchLoaderDelegate.load(Metadata).
     *
     * @param metadata Analysis metadata.
     * @see BranchLoaderDelegate
     */
    @Override
    public void load(final Metadata metadata) {
        Branch branch = this.getBranch(metadata);
        this.mutableAnalysisMetadataHolder.setBranch(branch);
        this.mutableAnalysisMetadataHolder.setPullRequestId(metadata.getPullRequestKey());
    }

    /**
     * Get a branch through metadata.
     *
     * @param metadata Information provided by SonarQube analysis.
     * @return The branch asked in metadata.
     */
    private Branch getBranch(final Metadata metadata) {
        final String branchName = StringUtils.trimToNull(metadata.getBranchName());
        if (branchName == null) {
            return this.getMainBranch();
        } else {
            final String mergeBranchName = StringUtils.trimToNull(metadata.getMergeBranchName());
            final BranchType branchType = this.getBranchType(metadata.getBranchType());
            return branchType == BranchType.PULL_REQUEST ? this.getBranch(StringUtils.trimToNull(metadata.getPullRequestKey()), branchName, mergeBranchName) : this.getBranch(branchName, branchType, mergeBranchName);
        }
    }

    /**
     * Get correct branch type in the context.
     *
     * @param branchType BranchType as defined in scanner.
     * @return BranchType for database.
     */
    private BranchType getBranchType(final org.sonar.scanner.protocol.output.ScannerReport.Metadata.BranchType branchType) {
        switch (branchType.ordinal()) {
            case 1:
                return BranchType.LONG;
            case 2:
                return BranchType.SHORT;
            case 3:
                return BranchType.PULL_REQUEST;
            case 4:
            default:
                throw new IllegalStateException("Invalid branch type: " + branchType);
        }
    }

    /**
     * Get main branch.
     *
     * @return The main branch.
     */
    private Branch getMainBranch() {
        final Project project = this.mutableAnalysisMetadataHolder.getProject();
        final Optional<BranchDto> optionalBranchDto = this.selectMainBranch(project.getUuid());
        Preconditions.checkState(optionalBranchDto.isPresent(), "Couldn't find main branch for project '%s'", new Object[]{project.getKey()});
        final String branchKey = (optionalBranchDto.get()).getKey();
        return new BranchImpl(BranchType.LONG, true, branchKey);
    }

    /**
     * Get a specific branch.
     *
     * @param branchKey      Key of the wanted branch.
     * @param branchType     Type of the wanted branch.
     * @param mergeBranchKey Merge target of the wanted branch.
     * @return A server side Branch.
     */
    private Branch getBranch(final String branchKey, final BranchType branchType, final String mergeBranchKey) {
        boolean isMasterBranch = false;
        final Optional<BranchDto> branchDto = this.selectBranchByKey(branchKey);
        if (branchDto.isPresent()) {
            checkBranchTypeValidity(branchDto.get(), branchType);
            isMasterBranch = (branchDto.get()).isMain();
        }

        final String mergeBranchUuid = this.getMergeBranchUuidByKey(mergeBranchKey);
        return new BranchImpl(branchType, isMasterBranch, branchKey, mergeBranchUuid, null);
    }

    /**
     * Get a specific branch which could be a pull request.
     *
     * @param pullRequestId  Id of the pull request.
     * @param branchKey      Key of the wanted branch.
     * @param mergeBranchKey Merge target of the wanted branch.
     * @return A server side Branch.
     */
    private Branch getBranch(final String pullRequestId, final String branchKey, final String mergeBranchKey) {
        return new BranchImpl(BranchType.PULL_REQUEST, false, branchKey, this.getMergeBranchUuidByKey(mergeBranchKey), pullRequestId);
    }

    /**
     * Get the branch named 'key' on current project from database.
     *
     * @param branchKey Branch to retrieve.
     * @return The BranchDto in an Optional.
     */
    private Optional<BranchDto> selectBranchByKey(final String branchKey) {
        final Project project = this.mutableAnalysisMetadataHolder.getProject();
        return this.selectBranchByKey(project.getUuid(), branchKey);
    }

    /**
     * Assert that a branch from database has the expected type.
     *
     * @param branchDto          Branch to test.
     * @param expectedBranchType Expected branch type.
     */
    private static void checkBranchTypeValidity(final BranchDto branchDto, final BranchType expectedBranchType) {
        if (branchDto.getBranchType() != expectedBranchType) {
            throw MessageException.of(String.format("Invalid branch type '%s'. Branch '%s' already exists with type '%s'.", branchDto.getBranchType(), branchDto.getKey(), expectedBranchType));
        }
    }

    /**
     * Get merge branch.
     *
     * @param key Key of the merge branch.
     * @return Return main branch uuid if no key is specified and the branch 'key' uuid otherwise.
     */
    private String getMergeBranchUuidByKey(final String key) {
        final Project project = this.mutableAnalysisMetadataHolder.getProject();
        if (key == null) {
            return project.getUuid();
        } else {
            Optional<BranchDto> optionalBranchDto = this.selectBranchByKey(project.getUuid(), key);
            Preconditions.checkState(optionalBranchDto.isPresent(), "Merge branch '%s' does not exist", new Object[]{key});
            BranchDto branchDto = optionalBranchDto.get();
            Preconditions.checkState(branchDto.getBranchType() == BranchType.LONG, "Invalid merge branch '%s': it must be a long branch but it is '%s'", new Object[]{key, branchDto.getBranchType()});
            return branchDto.getUuid();
        }
    }

    /**
     * Get the branch named 'key' on project 'projectUuid' from database.
     *
     * @param projectUuid Project uuid.
     * @param key         Key of the branch.
     * @return The wanted branch in an Optional.
     */
    private Optional<BranchDto> selectBranchByKey(final String projectUuid, final String key) {
        final DbSession dbSession = this.dbClient.openSession(false);
        Throwable throwable = null;

        Optional<BranchDto> optionalBranchDto;

        try {
            optionalBranchDto = this.dbClient.branchDao().selectByBranchKey(dbSession, projectUuid, key);
        } catch (final Throwable caughtThrowable) {
            throwable = caughtThrowable;
            throw caughtThrowable;
        } finally {
            if (dbSession != null) {
                if (throwable != null) {
                    try {
                        dbSession.close();
                    } catch (final Throwable caughtThrowable) {
                        throwable.addSuppressed(caughtThrowable);
                    }
                } else {
                    dbSession.close();
                }
            }

        }

        return optionalBranchDto;
    }

    /**
     * Get the main branch from database by using uuid.
     *
     * @param uuid Project uuid.
     * @return The main branch in an Optional.
     */
    private Optional<BranchDto> selectMainBranch(final String uuid) {
        final DbSession dbSession = this.dbClient.openSession(false);
        Throwable throwable = null;

        Optional<BranchDto> optionalBranchDto;

        try {
            optionalBranchDto = this.dbClient.branchDao().selectByUuid(dbSession, uuid);
        } catch (final Throwable caughtThrowable) {
            throwable = caughtThrowable;
            throw caughtThrowable;
        } finally {
            if (dbSession != null) {
                if (throwable != null) {
                    try {
                        dbSession.close();
                    } catch (final Throwable caughtThrowable) {
                        throwable.addSuppressed(caughtThrowable);
                    }
                } else {
                    dbSession.close();
                }
            }

        }

        return optionalBranchDto;
    }
}
