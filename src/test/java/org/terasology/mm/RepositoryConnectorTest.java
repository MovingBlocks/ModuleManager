/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.mm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.Lists;

/**
 * Tests the {@link RepositoryConnector} class.
 * @author Martin Steiger
 */
public class RepositoryConnectorTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private RepositoryConnector connector;

    @Before
    public void setup() throws MalformedURLException {
        connector = new RepositoryConnector(testFolder.getRoot());

        connector.addRepository("terasology", new URL("http://artifactory.terasology.org/artifactory/repo"));
        connector.addRepository("central", new URL("http://central.maven.org/maven2/"));
    }

    @Test
    public void findAvailableVersionsTest() {

        Collection<String> versions = connector.findAvailableVersions("Sample");

        // make sure it's a list
        List<String> list = Lists.newArrayList(versions);
        List<String> expected = Collections.singletonList("0.1.0-SNAPSHOT");
        Assert.assertEquals(expected, list);
    }

    @Test
    public void reuseSessionTest() {

        connector.findAvailableVersions("Sample");
        Collection<String> versions = connector.findAvailableVersions("Sample");

        // make sure it's a list
        List<String> list = Lists.newArrayList(versions);
        List<String> expected = Collections.singletonList("0.1.0-SNAPSHOT");
        Assert.assertEquals(expected, list);
    }

    @Test
    public void downloadArtifactTest() throws ArtifactResolutionException {

        File file = connector.downloadArtifact("Sample", "0.1.0-SNAPSHOT");

        Assert.assertNotNull(file);
    }

    @Test
    public void getDependenciesTest() throws ArtifactDescriptorException {

        Collection<String> file = connector.getDependencies("CopperAndBronze", "0.1.0-SNAPSHOT");

        Assert.assertThat(file, IsCollectionContaining.hasItem("engine"));
    }
}
