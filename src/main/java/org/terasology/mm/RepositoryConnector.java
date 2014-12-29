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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.mm.aether.LoggingRepositoryListener;
import org.terasology.mm.aether.LoggingTransferListener;

import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

/**
 * A wrapper around eclipse Aether.
 * @author Martin Steiger
 */
public class RepositoryConnector {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryConnector.class);

    private final RepositorySystem system;

    private final List<RemoteRepository> repos = Lists.newArrayList();

    private final RepositorySystemSession session;

    private String groupId = "org.terasology.modules";

    /**
     * @param file
     * @param system
     */
    public RepositoryConnector(File baseDir) {

        /*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
//        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.error("Could not create service {}", type, exception);
            }
        });

        system = locator.getService(RepositorySystem.class);
        session = createSession(system, baseDir);
    }

    public void addRepository(String id, URL url) {
        RemoteRepository repo = new RemoteRepository.Builder(id, "default", url.toExternalForm()).build();
        repos.add(repo);
    }

    public Collection<String> findAvailableVersions(String moduleId) {

        Artifact artifact = new DefaultArtifact(groupId, moduleId, "jar", "[0,)");

        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(repos);

        VersionRangeResult rangeResult;
        try {
            rangeResult = system.resolveVersionRange(session, rangeRequest);
        } catch (VersionRangeResolutionException e) {
            logger.error("The requested range could not be parsed", e);
            return Collections.emptyList();
        }

        // transform List<Version> to a List<String> using toString()
        return FluentIterable.from(rangeResult.getVersions()).transform(Functions.toStringFunction()).toList();
    }

    public File downloadArtifact(String moduleId, String version) throws ArtifactResolutionException {
        Artifact artifact = new DefaultArtifact(groupId, moduleId, "jar", version);

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(repos);

        ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);

        Artifact resolved = artifactResult.getArtifact();

        return resolved.getFile();
    }

    public Collection<String> getDependencies(String moduleId, String version) throws ArtifactDescriptorException {
        Artifact artifact = new DefaultArtifact(groupId, moduleId, "jar", version);

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(artifact);
        descriptorRequest.setRepositories(repos);

        ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);

        List<String> deps = Lists.newArrayList();
        for (Dependency dep : descriptorResult.getDependencies()) {
            deps.add(dep.getArtifact().getArtifactId());
        }
        return deps;
    }

    private static RepositorySystemSession createSession(RepositorySystem system, File baseDir) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(baseDir);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        session.setTransferListener(new LoggingTransferListener());
        session.setRepositoryListener(new LoggingRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
   }
}
