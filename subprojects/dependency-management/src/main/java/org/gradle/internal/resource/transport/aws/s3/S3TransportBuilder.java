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

package org.gradle.internal.resource.transport.aws.s3;

import org.gradle.api.artifacts.repositories.AwsCredentials;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.internal.resource.cached.CachedExternalResourceIndex;
import org.gradle.logging.ProgressLoggerFactory;
import org.gradle.util.BuildCommencedTimeProvider;

public class S3TransportBuilder {
    private String name;
    private AwsCredentials awsCredentials;
    private ProgressLoggerFactory progressLoggerFactory;
    private TemporaryFileProvider temporaryFileProvider;
    private CachedExternalResourceIndex<String> cachedExternalResourceIndex;
    private BuildCommencedTimeProvider timeProvider;
    private CacheLockingManager cacheLockingManager;

    public S3TransportBuilder name(String name) {
        this.name = name;
        return this;
    }

    public S3TransportBuilder awsCredentials(AwsCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
        return this;
    }

    public S3TransportBuilder progressLoggerFactory(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLoggerFactory = progressLoggerFactory;
        return this;
    }

    public S3TransportBuilder temporaryFileProvider(TemporaryFileProvider temporaryFileProvider) {
        this.temporaryFileProvider = temporaryFileProvider;
        return this;
    }

    public S3TransportBuilder cachedExternalResourceIndex(CachedExternalResourceIndex<String> cachedExternalResourceIndex) {
        this.cachedExternalResourceIndex = cachedExternalResourceIndex;
        return this;
    }

    public S3TransportBuilder timeProvider(BuildCommencedTimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        return this;
    }

    public S3TransportBuilder cacheLockingManager(CacheLockingManager cacheLockingManager) {
        this.cacheLockingManager = cacheLockingManager;
        return this;
    }

    public S3Transport build() {
        return new S3Transport(name,
                awsCredentials,
                progressLoggerFactory,
                temporaryFileProvider,
                cachedExternalResourceIndex,
                timeProvider,
                cacheLockingManager);
    }
}