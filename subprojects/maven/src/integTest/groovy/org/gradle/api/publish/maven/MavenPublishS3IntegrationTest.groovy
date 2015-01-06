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


package org.gradle.api.publish.maven

import spock.lang.Ignore
import spock.lang.IgnoreIf

class MavenPublishS3IntegrationTest extends AbstractMavenPublishIntegTest {

    @IgnoreIf({ System.getProperty("user.name") != "adrianbk" })
    def "can publish to an S3 Maven repository"() {
        given:
        settingsFile << 'rootProject.name = "publish"'
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'maven-publish'

            version = '2'
            group = 'org.group.name'

            publishing {
              repositories {
                    maven {
                        url "s3://${System.getenv('G_S3_BUCKET')}/maven/release/"
                        credentials(AwsCredentials) {
                            accessKey "${System.getenv('G_AWS_ACCESS_KEY_ID')}"
                            secretKey "${System.getenv('G_AWS_SECRET_ACCESS_KEY')}"
                        }
                    }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """


        when:
        executer.withArguments("-d")

        then:
        succeeds 'publish'
    }

    @IgnoreIf({ System.getProperty("user.name") != "adrianbk" })
    @Ignore
    def "can publish to an Maven repository"() {
        given:
        settingsFile << 'rootProject.name = "publish"'
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'maven-publish'

            version = '2'
            group = 'org.group.name'

            publishing {
              repositories {
                    maven {
                        url "http://127.0.0.1:8081/artifactory/libs-release-local"
                        credentials {
                            username "admin"
                            password "password"
                        }
                    }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """

        executer.withArguments("-d")
        expect:
        succeeds 'publish'
    }
}
