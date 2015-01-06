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

package org.gradle.api.publication.maven.internal;

import com.google.common.base.Optional;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.publication.maven.internal.ant.CustomDeployTask;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public abstract class AbstractDeployDelegate {
    private String protocol;

    public AbstractDeployDelegate(String protocol, CustomDeployTask deployTask, MavenArtifactRepository artifactRepository, RepositoryTransportFactory repositoryTransportFactory) {
        this.protocol = protocol;
    }

    public abstract Optional<Date> lastModifiedDateOfRemote(String remoteResource);

    public abstract boolean remoteResourceExist(String resourceName);

    public abstract void putFile(File file, String resourceName) throws IOException;

    public abstract boolean getAndWriteFile(File destination, String resourceName) throws ResourceDoesNotExistException, IOException;

    public String getProtocol() {
        return protocol;
    }
}
