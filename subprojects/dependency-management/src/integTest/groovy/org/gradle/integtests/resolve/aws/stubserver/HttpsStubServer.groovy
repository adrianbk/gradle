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

import com.github.adrianbk.stubby.standalone.ServerHandler
import com.github.adrianbk.stubby.standalone.ServerInstance
import com.github.adrianbk.stubby.standalone.TrustedHttpsServerInstance

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A HTTP stub server for stubbing requests
 *
 * To view stubbed responses see: https://localhost:9443/_control/responses
 * To view stubbed responses see: https://localhost:9443/_control/requests
 */
class HttpsStubServer {
    static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    static ServerInstance httpsServer

    static ServerInstance httpsStub() {
        httpsServer = httpsServer ?: initHttps()
        httpsServer
    }

    static initHttps() {
        ServerHandler handler = new ServerHandler();
        TrustedHttpsServerInstance trustedHttpsServerInstance = new TrustedHttpsServerInstance(9443, handler, EXECUTOR);
        trustedHttpsServerInstance
    }

    static endpoint = {
        "https://localhost:${httpsServer.getAddress().getPort()}"
    }
}
