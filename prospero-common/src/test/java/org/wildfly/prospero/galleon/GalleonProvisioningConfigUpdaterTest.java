/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.prospero.galleon;

import org.jboss.galleon.config.FeaturePackConfig;
import org.jboss.galleon.config.ProvisioningConfig;
import org.jboss.galleon.universe.FeaturePackLocation;
import org.jboss.galleon.universe.maven.MavenArtifact;
import org.jboss.galleon.universe.maven.repo.MavenRepoManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GalleonProvisioningConfigUpdaterTest {

    public static final FeaturePackLocation TEST_FPL = FeaturePackLocation.fromString("org.wildfly:wildfly-ee-galleon-pack:1.2.2:zip");
    @Mock
    private MavenRepoManager mavenRepoManager;

    @Test
    public void testCopyNonMavenFpl() throws Exception {
        final FeaturePackLocation nonMavenFpl = FeaturePackLocation.fromString("wildfly-ee@maven(org.jboss.universe:community-universe):current");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(nonMavenFpl)
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertEquals(nonMavenFpl, updated.getFeaturePackDeps().stream().findFirst().get().getLocation());
        verifyNoInteractions(mavenRepoManager);
    }

    @Test
    public void testParseFplToArtifact() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).then((Answer<String>) invocationOnMock -> {
            final MavenArtifact artifact = invocationOnMock.getArgument(0);
            assertEquals("1.2.2", artifact.getVersion());
            assertEquals("org.wildfly", artifact.getGroupId());
            assertEquals("wildfly-ee-galleon-pack", artifact.getArtifactId());
            assertEquals("zip", artifact.getExtension());
            return "1.2.3";
        });
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(TEST_FPL)
                .build();

        new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
    }

    @Test
    public void testUpdate() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).thenReturn("1.2.3");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(TEST_FPL)
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertEquals("1.2.3", updated.getFeaturePackDeps().stream().findFirst().get().getLocation().getBuild());
    }

    @Test
    public void testUpdateMaintainsInheritPackages() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).thenReturn("1.2.3");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(FeaturePackConfig.builder(TEST_FPL).setInheritPackages(true).build())
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertEquals(true, (boolean) updated.getFeaturePackDeps().stream().findFirst().get().getInheritPackages());
    }

    @Test
    public void testUpdateMaintainsIncludedPackages() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).thenReturn("1.2.3");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(FeaturePackConfig.builder(TEST_FPL).includePackage("foo.bar").build())
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertTrue(updated.getFeaturePackDeps().stream().findFirst().get().getIncludedPackages().contains("foo.bar"));
    }

    @Test
    public void testUpdateMaintainsExcludedPackages() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).thenReturn("1.2.3");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(FeaturePackConfig.builder(TEST_FPL).excludePackage("foo.bar").build())
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertTrue(updated.getFeaturePackDeps().stream().findFirst().get().getExcludedPackages().contains("foo.bar"));
    }

    @Test
    public void testUpdateMaintainsOptions() throws Exception {
        when(mavenRepoManager.getLatestVersion(any())).thenReturn("1.2.3");
        ProvisioningConfig config = ProvisioningConfig.builder()
                .addFeaturePackDep(FeaturePackConfig.builder(TEST_FPL).build())
                .addOption("foo.bar", "true")
                .build();

        final ProvisioningConfig updated = new GalleonProvisioningConfigUpdater(mavenRepoManager).updateFPs(config);
        assertEquals("true", updated.getOption("foo.bar"));
    }
}