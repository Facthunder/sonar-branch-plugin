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

import org.sonar.plugin.ce.ReportAnalysisComponentProvider;

/**
 * Implementation of ReportAnalysisComponentProvider.
 *
 * @see ReportAnalysisComponentProvider
 */
public final class ReportAnalysisComponentProviderImpl implements ReportAnalysisComponentProvider {

    /**
     * Default empty constructor.
     */
    public ReportAnalysisComponentProviderImpl() {

    }

    /**
     * Get the list of component to send to the compute engine.
     *
     * @return Components to attach to the analysis report for compute engine.
     */
    @Override
    public List<Object> getComponents() {
        return Arrays.asList(BranchLoaderDelegateImpl.class);
    }
}