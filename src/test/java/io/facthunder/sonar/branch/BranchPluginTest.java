package io.facthunder.sonar.branch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

public class BranchPluginTest {

    /**
     * Instance of the plugin to test
     */
    private BranchPlugin branchPlugin;

    /**
     * Prepare each test by creating a new CommunityPlugin
     */
    @Before
    public void prepare() {
        branchPlugin = new BranchPlugin();
    }

    /**
     * Assert that the plugin subscribe correctly to SonarQube
     * by checking the good number of extensions.
     */
    @Test
    public void sonarqubePluginDefinitionTest() {
        final SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(7,1), SonarQubeSide.SERVER);
        final Plugin.Context context = new Plugin.Context(runtime);
        branchPlugin.define(context);
        Assert.assertEquals(3, context.getExtensions().size());
    }

}