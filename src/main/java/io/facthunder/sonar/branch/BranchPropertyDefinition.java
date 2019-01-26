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

import java.util.Arrays;
import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

/**
 * Define specific properties for branch management.
 */
public class BranchPropertyDefinition {

    /**
     * Private constructor.
     */
    private BranchPropertyDefinition() {

    }

    /**
     * Declare properties linked to the plugin.
     *
     * @return List of property definitions.
     */
    public static List<PropertyDefinition> getPropertiesDefinition() {
        return Arrays.asList(
                PropertyDefinition.builder("sonar.branch.longLivedBranches.regex")
                        .name("Detection of long lived branches")
                        .description("Regular expression used to detect whether a branch is a long living branch (as opposed to short living branch), based on its name. This applies only during first analysis, the type of a branch cannot be changed later.")
                        .category("general")
                        .subCategory("Branches")
                        .defaultValue("(branch|release)-.*")
                        .onQualifiers("TRK", new String[0])
                        .build(),
                PropertyDefinition.builder("sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches")
                        .name("Number of days before purging inactive short living branches")
                        .description("Short living branches are permanently deleted when there are no analysis for the configured number of days.")
                        .category("general")
                        .subCategory("Branches")
                        .type(PropertyType.INTEGER)
                        .defaultValue("30")
                        .build());
    }
}
