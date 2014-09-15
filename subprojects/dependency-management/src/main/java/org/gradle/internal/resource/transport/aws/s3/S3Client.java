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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.gradle.api.artifacts.repositories.AwsCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);
    private static final Pattern FILENAME_PATTERN = Pattern.compile("[^\\/]+\\.*$");
    private final AmazonS3Client amazonS3Client;

    public S3Client(AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    public S3Client(AwsCredentials awsCredentials) {
        S3CredentialsProvider s3CredentialsProvider = new S3CredentialsProvider(awsCredentials.getAccessKey(), awsCredentials.getSecretKey());
        amazonS3Client = createClient(s3CredentialsProvider);
    }

    public S3Client(String accessKey, String secret) {
        S3CredentialsProvider s3CredentialsProvider = new S3CredentialsProvider(accessKey, secret);
        amazonS3Client = createClient(s3CredentialsProvider);
    }

    private AmazonS3Client createClient(S3CredentialsProvider s3CredentialsProvider) {
        AmazonS3Client client = new AmazonS3Client(s3CredentialsProvider.getChain());
        return client;
    }

    public void put(InputStream inputStream, Long contentLength, URI destination) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);
        String bucketName = getBucketName(destination);
        String s3Key = getS3BucketKey(destination);
        LOGGER.debug("Attempting to put resource:[{}] into bucket [{}]", s3Key, bucketName);
        PutObjectResult putObjectResult = amazonS3Client.putObject(bucketName, s3Key, inputStream, objectMetadata);
    }

    public ObjectMetadata getMetaData(URI uri) {
        String bucketName = getBucketName(uri);
        String s3Key = getS3BucketKey(uri);
        ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(bucketName, s3Key);
        return objectMetadata;
    }

    public S3Object getResource(URI uri) {
        S3Object object = amazonS3Client.getObject(getBucketName(uri), getS3BucketKey(uri));
        return object;
    }

    private String getS3BucketKey(URI destination) {
        String path = destination.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String getBucketName(URI uri) {
        return uri.getHost();
    }

    public List<String> list(URI parent) {
        List<String> results = new ArrayList<String>();
        String bucketName = getBucketName(parent);
        String s3Key = getS3BucketKey(parent);
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(s3Key)
                .withDelimiter("/");
        ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);
        results.addAll(resolveResourceNames(objectListing));

        while (objectListing.isTruncated()) {
            objectListing = amazonS3Client.listNextBatchOfObjects(objectListing);
            results.addAll(resolveResourceNames(objectListing));
        }
        return results;
    }

    private List<String> resolveResourceNames(ObjectListing objectListing) {
        List<String> results = new ArrayList<String>();
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        if (null != objectSummaries) {
            for (S3ObjectSummary objectSummary : objectSummaries) {
                String key = objectSummary.getKey();
                String fileName = extractResourceName(key);
                if (null != fileName) {
                    results.add(fileName);
                }
            }
        }
        return results;
    }

    private String extractResourceName(String key) {
        Matcher matcher = FILENAME_PATTERN.matcher(key);
        if (matcher.find()) {
            String group = matcher.group(0);
            return group.contains(".") ? group : null;
        }
        return null;
    }
}
