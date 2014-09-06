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

package org.gradle.api.internal.artifacts.repositories

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.AwsCredentials
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.credentials.Credentials
import org.gradle.internal.credentials.DefaultAwsCredentials
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import spock.lang.Specification
import spock.lang.Unroll

class AbstractAuthenticationSupportedRepositoryTest extends Specification {

    final AbstractAuthenticationSupportedRepository repository = new AuthSupportedRepository(null, new DirectInstantiator())

    def "should configure default password credentials using a closure only"() {
        setup:
        DefaultPasswordCredentials passwordCredentials = new DefaultPasswordCredentials()
        enhanceCredentials(passwordCredentials, 'username', 'password')

        Instantiator instantiator = Mock()
        instantiator.newInstance(DefaultPasswordCredentials) >> passwordCredentials

        AuthSupportedRepository repo = new AuthSupportedRepository(passwordCredentials, instantiator)

        when:
        Closure cls = {
            username "myUsername"
            password "myPassword"
        }
        repo.credentials(cls)

        then:
        repo.credentials
        repo.credentials.username == 'myUsername'
        repo.credentials.password == 'myPassword'
    }

    def "should configure alternative credentials"() {
        setup:
        DefaultAwsCredentials enhancedCredentials = new DefaultAwsCredentials()
        enhanceCredentials(enhancedCredentials, 'accessKey', 'secretKey')

        Instantiator instantiator = Mock()
        instantiator.newInstance(DefaultAwsCredentials) >> enhancedCredentials

        AuthSupportedRepository repo = new AuthSupportedRepository(null, instantiator)

        def action = new ClosureBackedAction<DefaultAwsCredentials>({
            accessKey = 'key'
            secretKey = 'secret'
        })

        when:
        repo.credentials(AwsCredentials.class, action)
        def credentials = repo.getAlternativeCredentials()

        then:
        credentials.accessKey == 'key'
        credentials.secretKey == 'secret'

    }

    @Unroll
    def "should instantiate the correct default credential types "() {
        Instantiator instantiator = Mock()
        Action action = Mock()
        AuthSupportedRepository repo = new AuthSupportedRepository(null, instantiator)

        when:
        repo.credentials(credentialType, action)

        then:
        1 * instantiator.newInstance(_) >> credentials
        1 * action.execute(credentials)
        check.call(repo)

        where:
        credentialType      | credentials               | check
        AwsCredentials      | Mock(AwsCredentials)      | { it.alternativeCredentials instanceof AwsCredentials && it.credentials == null }
        PasswordCredentials | Mock(PasswordCredentials) | { it.alternativeCredentials instanceof PasswordCredentials && it.credentials == null }
    }

    def "should use alternative credentials for s3 scheme"() {
        Instantiator instantiator = Mock()
        AuthSupportedRepository repo = new AuthSupportedRepository(null, instantiator)
        def alternative = Mock(AwsCredentials)
        Action action = Mock()
        1 * instantiator.newInstance(DefaultAwsCredentials) >> alternative
        repo.credentials(AwsCredentials, action)

        when:
        def credentials = repo.getCredentialsForSchemes(["s3"] as Set)

        then:
        credentials == alternative
    }

    def "should default both credentials to PasswordCredentials"() {
        setup:
        PasswordCredentials pCredentials = Mock()
        when:
        def repo = new AuthSupportedRepository(pCredentials, null)
        then:
        repo.getCredentials() == pCredentials
        repo.getAlternativeCredentials() == pCredentials
    }

    private void enhanceCredentials(Credentials credentials, String... props) {
        props.each { prop ->
            credentials.metaClass."$prop" = { String val ->
                delegate."set${prop.capitalize()}"(val)
            }
        }
    }

    class AuthSupportedRepository extends AbstractAuthenticationSupportedRepository {
        def AuthSupportedRepository(Credentials credentials, Instantiator instantiator) {
            super(credentials, instantiator)
        }
    }
}
