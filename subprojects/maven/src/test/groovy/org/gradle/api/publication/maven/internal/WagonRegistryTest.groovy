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

package org.gradle.api.publication.maven.internal

import org.apache.maven.artifact.manager.WagonManager
import org.codehaus.plexus.PlexusContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory
import org.gradle.api.publication.maven.internal.ant.CustomDeployTask
import spock.lang.Specification

class WagonRegistryTest extends Specification {

    def "should contain an S3 wagon"() {
        expect:
        new WagonRegistry().waggonContexts.find { it.value.getProtocol() == 's3' }
    }

    def "should add a new wagon"() {
        WagonRegistry registry = new WagonRegistry()
        WagonContext wagonContext = Mock()
        wagonContext.getProtocol() >> 'https'

        when:
        registry.add(wagonContext)

        then:
        registry.waggonContexts.find { it.key == 'https' }
    }


    def "should override an existing wagon with the same protocol"() {
        WagonRegistry registry = new WagonRegistry()
        int sizeBefore = registry.waggonContexts.size()
        WagonContext wagonContext1 = Mock()
        wagonContext1.getProtocol() >> 'https'


        WagonContext wagonContext2 = Mock()
        wagonContext2.getProtocol() >> 'https'

        when:
        registry.add(wagonContext1)
        registry.add(wagonContext2)

        then:
        registry.waggonContexts.size() == sizeBefore + 1
    }

    def "should throw when adding a context with an invalid protocol"() {
        given:
        WagonRegistry registry = new WagonRegistry()
        WagonContext wagonContext = Mock()
        wagonContext.getProtocol() >> protocol

        when:
        registry.add(wagonContext)

        then:
        thrown(IllegalArgumentException)

        where:
        protocol << [null, '', '  ']
    }

    def "should create the deploy delegate for a custom wagon"() {
        given:
        DelegatingDeployWagon wagon = Mock()

        WagonManager wagonManager = Mock()
        wagonManager.getWagon(wagonProtocol) >> wagon

        PlexusContainer container = Mock()
        container.lookup(WagonManager.ROLE) >> wagonManager

        CustomDeployTask task = Mock()
        task.getContainer() >> container

        MavenArtifactRepository artifactRepository = Mock()
        artifactRepository.getUrl() >> new URI(artifactUri)

        DeployerDelegateFactory deployerDelegateFactory = Mock()
        WagonContext wagonContext = Mock()
        wagonContext.getProtocol() >> wagonProtocol
        wagonContext.getDelegateFactory() >> deployerDelegateFactory

        WagonRegistry registry = new WagonRegistry()
        registry.add(wagonContext)
        when:
        registry.register(task, artifactRepository, Mock(RepositoryTransportFactory))

        then:
        1 * wagon.createDelegate(deployerDelegateFactory, task, artifactRepository, _)

        where:
        artifactUri             | wagonProtocol
        "testScheme://somerepo" | 'testScheme'
        "TESTSCHEME://somerepo" | 'testScheme'
        "testScheme://somerepo" | 'TESTSCHEME'
    }

    def "should not create a deploy delegate when protocols do not match"() {
        given:
        PlexusContainer container = Mock()
        container.lookup(WagonManager.ROLE) >> { throw new RuntimeException("should not happen") }

        CustomDeployTask task = Mock()
        task.getContainer() >> container

        MavenArtifactRepository artifactRepository = Mock()
        artifactRepository.getUrl() >> new URI('testScheme://somerepo')

        WagonContext wagonContext = Mock()
        wagonContext.getProtocol() >> 'notTestScheme'
        wagonContext.getDelegateFactory() >> null

        WagonRegistry registry = new WagonRegistry()
        registry.add(wagonContext)
        expect:
        registry.register(task, artifactRepository, Mock(RepositoryTransportFactory))
    }
}
