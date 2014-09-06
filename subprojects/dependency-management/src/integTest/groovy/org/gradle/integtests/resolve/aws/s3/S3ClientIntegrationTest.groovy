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

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import org.apache.commons.io.IOUtils
import org.gradle.internal.resource.transport.aws.s3.S3Client
import spock.lang.Ignore
import spock.lang.Specification

class S3ClientIntegrationTest extends Specification {

    final String accessKey = System.getenv('AWS_ACCESS_KEY_ID')
    final String secret = System.getenv('AWS_SECRET_ACCESS_KEY')
    final String bucketName = System.getenv('GRADLE_S3_BUCKET')

    //TODO - ak - should we test this at all
    @Ignore
    def "should perform put get and list on an S3 bucket"() {
        setup:
        String tempDir = System.getProperty("java.io.tmpdir")
        def fileContents = 'This is only a test'
        S3Client s3Client = new S3Client(accessKey, secret)
        File file = new File("${tempDir}/mavenTest.txt")

        file.delete()
        file << fileContents

        when:
        def stream = new FileInputStream(file)
        def uri = new URI("s3://${bucketName}/maven/release/mavenTest.txt")
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
}