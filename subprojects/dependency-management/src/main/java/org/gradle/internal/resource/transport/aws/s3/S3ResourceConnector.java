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

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.gradle.api.artifacts.repositories.AwsCredentials;
import org.gradle.internal.Factory;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceAccessor;
import org.gradle.internal.resource.transfer.ExternalResourceLister;
import org.gradle.internal.resource.transfer.ExternalResourceUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public class S3ResourceConnector implements ExternalResourceLister, ExternalResourceAccessor, ExternalResourceUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ResourceConnector.class);
    private final S3Client s3Client;

    public S3ResourceConnector(AwsCredentials awsCredentials) {
        this.s3Client = new S3Client(awsCredentials);
    }

    public List<String> list(URI parent) throws IOException {
        LOGGER.debug("Listing parent resources: {}", parent);
        List<String> list = s3Client.list(parent);
        return list;
    }

    public ExternalResource getResource(URI location) throws IOException {
        LOGGER.debug("Attempting to get resource: {}", location);
        S3Object resource = s3Client.getResource(location);
        return new S3Resource(resource, location);
    }

    public HashValue getResourceSha1(URI location) {
        return null;
    }

    public ExternalResourceMetaData getMetaData(URI location) throws IOException {
        LOGGER.debug("Attempting to get resource metadata: {}", location);
        ObjectMetadata metaData = s3Client.getMetaData(location);
        DefaultExternalResourceMetaData defaultExternalResourceMetaData = new DefaultExternalResourceMetaData(location,
                metaData.getLastModified().getTime(),
                metaData.getContentLength(),
                metaData.getETag(),
                null);

        return defaultExternalResourceMetaData;
    }

    public void upload(Factory<InputStream> sourceFactory, Long contentLength, URI destination) throws IOException {
        LOGGER.debug("Attempting to get upload stream to : {}", destination);
        s3Client.put(sourceFactory.create(), contentLength, destination);
    }
}
