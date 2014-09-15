/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.artifacts.repositories;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.AuthenticationSupported;
import org.gradle.api.artifacts.repositories.AwsCredentials;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.credentials.Credentials;
import org.gradle.internal.credentials.DefaultAwsCredentials;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.WrapUtil;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractAuthenticationSupportedRepository extends AbstractArtifactRepository implements AuthenticationSupported {
    private PasswordCredentials credentials;
    private Credentials alternativeCredentials;
    private final Instantiator instantiator;
    private AtomicBoolean credentialsOverride = new AtomicBoolean(false);

    AbstractAuthenticationSupportedRepository(PasswordCredentials credentials, Instantiator instantiator) {
        this.credentials = credentials;
        this.alternativeCredentials = credentials;
        this.instantiator = instantiator;
    }

    public PasswordCredentials getCredentials() {
        return credentials;
    }

    public void credentials(Closure closure) {
        credentials(new ClosureBackedAction<PasswordCredentials>(closure));
    }

    public void credentials(Action<? super PasswordCredentials> action) {
        action.execute(credentials);
    }

    public <T extends Credentials> void credentials(Class<T> clazz, Action<? super Credentials> action) {
        Credentials instance = null;
        if (clazz == AwsCredentials.class) {
            instance = instantiator.newInstance(DefaultAwsCredentials.class);
        } else if (clazz == PasswordCredentials.class) {
            instance = instantiator.newInstance(DefaultPasswordCredentials.class);
        }
        overrideDefaultCredentials(instance);
        action.execute(instance);
    }

    /**
     * Implies the user has configured the DSL with a specific type: { credentials(PasswordCredentials){ ... }
     */
    private void overrideDefaultCredentials(Credentials instance) {
        alternativeCredentials = instance;
        credentials = null;
        credentialsOverride.getAndSet(true);
    }

    public Credentials getAlternativeCredentials() {
        return this.alternativeCredentials;
    }

    public Credentials getCredentialsForSchemes(Set<String> schemes) {
        if (WrapUtil.toSet("s3").containsAll(schemes)) {
            return getAlternativeCredentials();
        }
        return getAlternativeCredentials();
    }
}
