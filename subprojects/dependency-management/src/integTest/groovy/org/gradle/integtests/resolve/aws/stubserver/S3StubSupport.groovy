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

package org.gradle.integtests.resolve.aws.stubserver

import com.github.adrianbk.stubby.standalone.ServerInstance
import groovy.json.JsonBuilder
import groovy.xml.StreamingMarkupBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

class S3StubSupport {
    private static final String STUB_POST_PATH = "/_control/responses"
    public static final String ETAG = 'd41d8cd98f00b204e9800998ecf8427e'
    public static final String X_AMZ_REQUEST_ID = '0A398F9A1BAD4027'
    public static final String X_AMZ_ID_2 = 'nwUZ/n/F2/ZFRTZhtzjYe7mcXkxCaRjfrJSWirV50lN7HuvhF60JpphwoiX/sMnh'
    public static final String DATE_HEADER = 'Mon, 29 Sep 2014 11:04:27 GMT'
    public static final String SERVER_AMAZON_S3 = 'AmazonS3'
    public static final String FILE_ETAG = 'f286bf292bdf35ca883e5aa62a6439b5'
    RESTClient client
    ServerInstance stubServer
    URI endpoint

    S3StubSupport() {
        stubServer = HttpsStubServer.httpsStub()
        endpoint = new URI(HttpsStubServer.endpoint())
        client = new RESTClient("${HttpsStubServer.endpoint()}$STUB_POST_PATH")
        client.ignoreSSLIssues()
    }

    S3StubSupport(String location) {
        client = new RESTClient("${location}$STUB_POST_PATH")
        endpoint = new URI(location)
        client.ignoreSSLIssues()
    }

    def stubPutFile(File file, String url) {
        def json = new JsonBuilder()
        def root = json {
            request(
                    method: "PUT",
                    path: url,
                    headers: [
                            [name: 'Content-Type', value: "application/octet-stream"],
                            [name: 'Connection', value: 'Keep-Alive']
                    ]
            )
            response(
                    status: 200,
                    headers: [
                            [name: 'x-amz-id-2', value: X_AMZ_ID_2],
                            [name: 'x-amz-request-id', value: X_AMZ_REQUEST_ID],
                            [name: 'Date', value: DATE_HEADER],
                            [name: "ETag", value: FILE_ETAG],
                            [name: 'Server', value: SERVER_AMAZON_S3]
                    ]
            )
        }
        client.post(
                body: json.toString(),
                requestContentType: ContentType.JSON
        )
    }

    def stubMetaData(File file, String url) {
        def json = new JsonBuilder()
        def root = json {
            request(
                    method: "HEAD",
                    path: url,
                    headers: [
                            [name: 'Content-Type', value: "application/x-www-form-urlencoded; charset=utf-8"],
                            [name: 'Connection', value: 'Keep-Alive']
                    ]
            )
            response(
                    status: 200,
                    headers: [
                            [name: 'x-amz-id-2', value: X_AMZ_ID_2],
                            [name: 'x-amz-request-id', value: X_AMZ_REQUEST_ID],
                            [name: 'Date', value: DATE_HEADER],
                            [name: "ETag", value: ETAG],
                            [name: 'Server', value: SERVER_AMAZON_S3],
                            [name: 'Accept-Ranges', value: 'bytes'],
                            [name: 'Content-Type', value: 'application/octet-stream'],
                            [name: 'Content-Length', value: "${file.length()}"]
                    ]
            )
        }
        client.post(
                body: json.toString(),
                requestContentType: ContentType.JSON
        )
    }

    def stubGetFile(File file, String url) {
        def json = new JsonBuilder()
        def root = json {
            request(
                    method: "GET",
                    path: url,
                    headers: [
                            [name: 'Content-Type', value: "application/x-www-form-urlencoded; charset=utf-8"],
                            [name: 'Connection', value: 'Keep-Alive']
                    ]
            )
            response(
                    status: 200,
                    headers: [
                            [name: 'x-amz-id-2', value: X_AMZ_ID_2],
                            [name: 'x-amz-request-id', value: X_AMZ_REQUEST_ID],
                            [name: 'Date', value: DATE_HEADER],
                            [name: "ETag", value: FILE_ETAG],
                            [name: 'Server', value: SERVER_AMAZON_S3],
                            [name: 'Accept-Ranges', value: 'bytes'],
                            [name: 'Content-Type', value: 'application/octet-stream'],
                            [name: 'Content-Length', value: "${file.length()}"]
                    ],
                    body: file.text
            )
        }

        client.post(
                body: json.toString(),
                requestContentType: ContentType.JSON
        )
    }

    def stubListFile(File file, String bucketName, prefix = 'maven/release/', delimiter = '/') {

        def xml = new StreamingMarkupBuilder().bind {
            ListBucketResult(xmlns: "http://s3.amazonaws.com/doc/2006-03-01/") {
                Name(bucketName)
                Prefix(prefix)
                Marker()
                MaxKeys('1000')
                Delimiter(delimiter)
                IsTruncated('false')
                Contents {
                    Key(prefix)
                    LastModified('2014-09-21T06:44:09.000Z')
                    ETag(ETAG)
                    Size('0')
                    Owner {
                        ID("${(1..57).collect { 'a' }.join()}")
                        DisplayName('me')
                    }
                    StorageClass('STANDARD')
                }
                Contents {
                    Key(prefix + file.name)
                    LastModified('2014-10-01T13:03:29.000Z')
                    ETag(ETAG)
                    Size('19')
                    Owner {
                        ID("${(1..57).collect { 'a' }.join()}")
                        DisplayName('me')
                    }
                    StorageClass('STANDARD')
                }
                CommonPrefixes {
                    Prefix("${prefix}com/")
                }
            }
        }

        def json = new JsonBuilder()
        def root = json {
            request(
                    method: "GET",
                    params: [
                            [name: 'prefix', value: "${prefix}"],
                            [name: 'delimiter', value: "${delimiter}"]
                    ],
                    path: "/${bucketName}/",
                    headers: [
                            [name: 'Content-Type', value: "application/x-www-form-urlencoded; charset=utf-8"],
                            [name: 'Connection', value: 'Keep-Alive']
                    ]
            )
            response(
                    status: 200,
                    headers: [
                            [name: 'x-amz-id-2', value: X_AMZ_ID_2],
                            [name: 'x-amz-request-id', value: X_AMZ_REQUEST_ID],
                            [name: 'Date', value: DATE_HEADER],
                            [name: 'Server', value: SERVER_AMAZON_S3],
                            [name: 'Content-Type', value: 'application/xml']
                    ],
                    body: xml.toString()
            )
        }

        client.post(
                body: json.toString(),
                requestContentType: ContentType.JSON
        )
    }
}
