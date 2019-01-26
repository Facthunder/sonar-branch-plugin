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
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of {@link BranchParamsValidator}.
 */
public class BranchParamsValidatorImpl implements BranchParamsValidator {

    private static Pattern pattern = Pattern.compile("^\\.|/\\.|\\.lock$|\\.lock/|\\.\\.|//|[\\00-\\0040]|[?*\\[ ~^:\\0177\\\\]|^/|/$|\\.$|@\\{|^@$");
    private final GlobalConfiguration globalConfiguration;

    public BranchParamsValidatorImpl(final GlobalConfiguration var1) {
        this.globalConfiguration = var1;
    }


    /**
     * @see BranchParamsValidator
     */
    @Override
    public void validate(final List<String> validationMessages, final String deprecatedBranchName) {

        final String branchName = this.globalConfiguration.get("sonar.branch.name").orElse(null);
        validateBranchName(validationMessages, branchName);
        this.validatePropertyUsage(validationMessages, deprecatedBranchName, branchName);

    }

    private static void validateBranchName(final List<String> var0, final String branchName) {

        if (StringUtils.isNotEmpty(branchName) && !isValidBranchName(branchName)) {
            var0.add(String.format("\"%s\" is not a valid branch name. The allowed format is the same as for Git branches. See https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html", branchName));
        }

    }

    static boolean isValidBranchName(final String branchName) {

        return !pattern.matcher(branchName).find();

    }

    private void validatePropertyUsage(final List<String> validationMessages, final String deprecatedBranchName, final String branchName) {

        if (StringUtils.isNotEmpty(deprecatedBranchName) && StringUtils.isNotEmpty(branchName)) {
            validationMessages.add(String.format("The property \"%s\" must not be used together with the deprecated \"sonar.branch\"", "sonar.branch.name"));
        }

        final String branchTarget = this.globalConfiguration.get("sonar.branch.target").orElse(null);
        if (StringUtils.isNotEmpty(deprecatedBranchName) && StringUtils.isNotEmpty(branchTarget)) {
            validationMessages.add(String.format("The property \"%s\" must not be used together with the deprecated \"sonar.branch\"", "sonar.branch.target"));
        }

    }
}
