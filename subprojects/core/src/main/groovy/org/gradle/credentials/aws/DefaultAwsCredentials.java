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

package org.gradle.credentials.aws;

import org.gradle.api.artifacts.repositories.AwsCredentials;

public class DefaultAwsCredentials implements AwsCredentials {

    private String accessKey;
    private String secretKey;

    public DefaultAwsCredentials() {
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultAwsCredentials that = (DefaultAwsCredentials) o;

        if (accessKey != null ? !accessKey.equals(that.accessKey) : that.accessKey != null) {
            return false;
        }
        if (secretKey != null ? !secretKey.equals(that.secretKey) : that.secretKey != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = accessKey != null ? accessKey.hashCode() : 0;
        result = 31 * result + (secretKey != null ? secretKey.hashCode() : 0);
        return result;
    }
}
