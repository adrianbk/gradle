/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publication.maven.internal.aws.s3;

import com.google.common.base.Optional;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransport;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.publication.maven.internal.AbstractDeployDelegate;
import org.gradle.api.publication.maven.internal.ant.CustomDeployTask;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transport.aws.s3.S3Exception;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

public class S3DeployDelegate extends AbstractDeployDelegate {

    private final RepositoryTransport transport;
    private final MavenArtifactRepository artifactRepository;

    public S3DeployDelegate(String protocol, CustomDeployTask deployTask, MavenArtifactRepository artifactRepository, RepositoryTransportFactory repositoryTransportFactory) {
        super(protocol, deployTask, artifactRepository, repositoryTransportFactory);
        this.artifactRepository = artifactRepository;
        transport = repositoryTransportFactory.createTransport(protocol, artifactRepository.getName(), artifactRepository.getAlternativeCredentials());
    }

    @Override
    public boolean remoteResourceExist(String resourceName) {
        try {
            transport.getRepository().getResourceMetaData(getUriForResource(resourceName));
            return true;
        } catch (S3Exception e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Optional<Date> lastModifiedDateOfRemote(String remoteResource) {
        try {
            ExternalResourceMetaData resourceMetaData = transport.getRepository().getResourceMetaData(getUriForResource(remoteResource));
            return Optional.fromNullable(resourceMetaData.getLastModified());
        } catch (S3Exception e) {
            return Optional.absent();
        } catch (IOException e) {
            return Optional.absent();
        }
    }

    @Override
    public boolean getAndWriteFile(File destination, String resourceName) throws IOException, ResourceDoesNotExistException {
        URI uriForResource = getUriForResource(resourceName);
        try {
            transport.getRepository().getResource(uriForResource).writeTo(destination);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public void putFile(File file, String resourceName) throws IOException {
        transport.getRepository().put(file, getUriForResource(resourceName));
    }

    private URI getUriForResource(String resource) {
        return this.artifactRepository.getUrl().resolve(resource);
    }
}
