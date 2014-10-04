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

package org.gradle.integtests.resolve.aws.s3

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import org.apache.commons.io.IOUtils
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.gradle.integtests.resolve.aws.stubserver.S3StubSupport
import org.gradle.internal.resource.transport.aws.s3.S3Client
import spock.lang.Specification

import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class S3ClientIntegrationTest extends Specification {

    public static final String FILE_NAME = "mavenTest.txt"
    final String accessKey = System.getenv('G_AWS_ACCESS_KEY_ID') ?: 'gradle-access-key'
    final String secret = System.getenv('G_AWS_SECRET_ACCESS_KEY') ?: 'gradle-secret-key'
    final String bucketName = System.getenv('G_S3_BUCKET') ?: 'org.gradle.artifacts'
    S3StubSupport s3StubSupport = new S3StubSupport()

    def "should perform put get and list on an S3 bucket"() {
        setup:
        String tempDir = System.getProperty("java.io.tmpdir")
        def fileContents = 'This is only a test'
        File file = new File("${tempDir}/$FILE_NAME")

        file.delete()
        file << fileContents

        s3StubSupport.with{
            stubPutFile(file, "/${bucketName}/maven/release/$FILE_NAME")
            stubMetaData(file, "/${bucketName}/maven/release/$FILE_NAME")
            stubGetFile(file, "/${bucketName}/maven/release/$FILE_NAME")
            stubListFile(file, bucketName)
        }
        S3Client s3Client = new S3Client(accessKey, secret)
        configureAwsClientForStub(s3Client.amazonS3Client)

        when:
        def stream = new FileInputStream(file)
        def uri = new URI("s3://${bucketName}/maven/release/$FILE_NAME")
        s3Client.put(stream, file.length(), uri)

        then:
        ObjectMetadata data = s3Client.getMetaData(uri)
        data.instanceLength == fileContents.length()
        data.getETag() ==~ /\w{32}/

        and:
        S3Object object = s3Client.getResource(uri)
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        IOUtils.copyLarge(object.getObjectContent(), outStream);
        outStream.toString() == fileContents

        and:
        def files = s3Client.list(new URI("s3://${bucketName}/maven/release/"))
        println files
        files.each {
            assert it.contains(".")
        }
    }

    private void configureAwsClientForStub(AmazonS3Client amazonS3Client) {
        amazonS3Client.setEndpoint(s3StubSupport.endpoint.toString())
        amazonS3Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        Scheme scheme = new Scheme("https", s3StubSupport.endpoint.port, buildSSLSocketFactory());
        amazonS3Client.client.httpClient.getConnectionManager().getSchemeRegistry().register(scheme);
    }

    private SSLSocketFactory buildSSLSocketFactory() {
        TrustStrategy ts = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true
            }
        }
        return new SSLSocketFactory(ts, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }


}
