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
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.*;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.publication.maven.internal.ant.CustomDeployTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.apache.maven.wagon.events.SessionEvent.*;
import static org.apache.maven.wagon.events.TransferEvent.*;

/**
 * A maven wagon intended to work with {@link org.apache.maven.artifact.manager.DefaultWagonManager} Maven uses reflection to initialize instances of this wagon see: {@link
 * org.codehaus.plexus.component.factory.java.JavaComponentFactory#newInstance(org.codehaus.plexus.component.repository.ComponentDescriptor, org.codehaus.classworlds.ClassRealm,
 * org.codehaus.plexus.PlexusContainer)}
 */
public abstract class DelegatingDeployWagon implements Wagon {

    private SessionEventSupport sessionEventSupport = new SessionEventSupport();
    private TransferEventSupport transferEventSupport = new TransferEventSupport();
    private Repository mutatingRepository;
    protected AbstractDeployDelegate delegate;
    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingDeployWagon.class);

    public void createDelegate(DeployerDelegateFactory<? extends AbstractDeployDelegate> deployerDelegateFactory,
                               CustomDeployTask task,
                               MavenArtifactRepository artifactRepository,
                               RepositoryTransportFactory repositoryTransportFactory) {

        delegate = deployerDelegateFactory.create(getProtocol(), task, artifactRepository, repositoryTransportFactory);
    }

    public abstract String getProtocol();

    @Override
    public final void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);
        this.transferEventSupport.fireTransferInitiated(transferEvent(resource, TRANSFER_INITIATED, REQUEST_GET));
        this.transferEventSupport.fireTransferStarted(transferEvent(resource, TRANSFER_STARTED, REQUEST_GET));
        try {
            if (!destination.exists()) {
                LOGGER.info("Wagon deployment supplied a file [{}] which does not exist, forcing create.", destination.getAbsolutePath());
                destination.getParentFile().mkdirs();
                destination.createNewFile();
            }
            if (!delegate.getAndWriteFile(destination, resourceName)) {
                throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName));
            }
            this.transferEventSupport.fireTransferCompleted(transferEvent(resource, TRANSFER_COMPLETED, REQUEST_GET));
        } catch (IOException e) {
            this.transferEventSupport.fireTransferError(transferEvent(resource, TRANSFER_ERROR, REQUEST_GET));
            throw new TransferFailedException(String.format("Cannot get and write file '%s'", resourceName), e);
        }
    }

    @Override
    public final void put(File file, String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);
        this.transferEventSupport.fireTransferInitiated(transferEvent(resource, TRANSFER_INITIATED, REQUEST_PUT));
        this.transferEventSupport.fireTransferStarted(transferEvent(resource, TRANSFER_STARTED, REQUEST_PUT));
        try {
            delegate.putFile(file, resourceName);
        } catch (IOException e) {
            this.transferEventSupport.fireTransferError(transferEvent(resource, e, REQUEST_PUT));
            throw new GradleException(String.format("Could not put file to remote location: %s", resourceName), e);
        }
        this.transferEventSupport.fireTransferCompleted(transferEvent(resource, TRANSFER_COMPLETED, REQUEST_PUT));
    }

    @Override
    public final boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        return delegate.remoteResourceExist(resourceName);
    }

    @Override
    public final boolean getIfNewer(String resourceName, File file, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (timestamp > 0) {
            Date localTimestamp = new Date(timestamp);
            Optional<Date> dateOptional = delegate.lastModifiedDateOfRemote(resourceName);
            if (dateOptional.isPresent()) {
                Date lastModified = dateOptional.get();
                if (lastModified.compareTo(localTimestamp) > 0) {
                    get(resourceName, file);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final void putDirectory(File file, String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throwNotImplemented("putDirectory(File file, String resourceName)");
    }

    @Override
    public final List getFileList(String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throwNotImplemented("getFileList(String resourceName)");
        return null;
    }

    @Override
    public final boolean supportsDirectoryCopy() {
        return false;
    }

    @Override
    public final Repository getRepository() {
        return this.mutatingRepository;
    }

    @Override
    public final void openConnection() throws ConnectionException, AuthenticationException {
        resetState();
    }

    @Override
    public final void connect(Repository repository) throws ConnectionException, AuthenticationException {
        this.mutatingRepository = repository;
        this.sessionEventSupport.fireSessionLoggedIn(sessionEvent(SESSION_LOGGED_IN));
        this.sessionEventSupport.fireSessionOpened(sessionEvent(SESSION_OPENED));
    }

    @Override
    public final void connect(Repository repository, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(repository);

    }

    @Override
    public final void connect(Repository repository, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void disconnect() throws ConnectionException {
        this.sessionEventSupport.fireSessionDisconnecting(sessionEvent(SESSION_DISCONNECTING));
        this.sessionEventSupport.fireSessionLoggedOff(sessionEvent(SESSION_LOGGED_OFF));
        this.sessionEventSupport.fireSessionDisconnected(sessionEvent(SESSION_LOGGED_OFF));
    }

    private void removeAllListeners() {
        this.transferEventSupport = null;
        this.sessionEventSupport = null;
    }


    @Override
    public final void addSessionListener(SessionListener sessionListener) {
        this.sessionEventSupport.addSessionListener(sessionListener);
    }

    @Override
    public final void removeSessionListener(SessionListener sessionListener) {
        this.sessionEventSupport.removeSessionListener(sessionListener);
    }

    @Override
    public final boolean hasSessionListener(SessionListener sessionListener) {
        return this.sessionEventSupport.hasSessionListener(sessionListener);
    }

    @Override
    public final void addTransferListener(TransferListener transferListener) {
        this.transferEventSupport.addTransferListener(transferListener);
    }

    @Override
    public final void removeTransferListener(TransferListener transferListener) {
        this.transferEventSupport.addTransferListener(transferListener);
    }

    @Override
    public final boolean hasTransferListener(TransferListener transferListener) {
        return this.transferEventSupport.hasTransferListener(transferListener);
    }

    @Override
    public final boolean isInteractive() {
        return false;
    }

    @Override
    public final void setInteractive(boolean b) {

    }

    @Override
    public final void setTimeout(int i) {

    }

    @Override
    public final int getTimeout() {
        return 0;
    }

    private SessionEvent sessionEvent(int e) {
        return new SessionEvent(this, e);
    }

    private void throwNotImplemented(String s) {
        throw new GradleException("This wagon does not yet support the method:" + s);
    }

    private TransferEvent transferEvent(Resource resource, int eventType, int requestType) {
        return new TransferEvent(this, resource, eventType, requestType);
    }

    private TransferEvent transferEvent(Resource resource, Exception e, int requestType) {
        return new TransferEvent(this, resource, e, requestType);
    }

    /**
     * Dereference and reset all mutable state - not cool but consequence of implementing a maven wagon to work with ants org.apache.maven.artifact.manager.DefaultWagonManager
     */
    private void resetState() {
        this.mutatingRepository = null;
        removeAllListeners();
    }
}
