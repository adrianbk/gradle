/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.play.integtest.continuous

import groovy.transform.NotYetImplemented
import org.gradle.play.integtest.fixtures.AbstractPlayContinuousBuildIntegrationTest
import org.gradle.play.integtest.fixtures.RunningPlayApp
import org.gradle.play.integtest.fixtures.app.BasicPlayApp
import org.gradle.play.integtest.fixtures.app.PlayApp
import spock.util.concurrent.PollingConditions

class PlayContinuousBuildIntegrationTest extends AbstractPlayContinuousBuildIntegrationTest {
    RunningPlayApp runningApp = new RunningPlayApp(testDirectory)
    PlayApp playApp = new BasicPlayApp()

    def "build does not block when running play app with continuous build" () {
        when: "the build runs until it enters continuous build"
        succeeds("runPlayBinary")

        then:
        appIsRunningAndDeployed()

        and: "the build is waiting for changes"
        doesntExit()

        cleanup: "stopping gradle"
        stopGradle()
        appIsStopped()
    }

    def "can run play app multiple times with continuous build" () {
        when:
        succeeds("runPlayBinary")

        then:
        appIsRunningAndDeployed()

        when:
        file("conf/routes") << "\n# changed"

        then:
        succeeds()

        when:
        file("conf/routes") << "\n# changed again"

        then:
        succeeds()

        when:
        file("conf/routes") << "\n# changed yet again"

        then:
        succeeds()

        cleanup: "stopping gradle"
        stopGradle()
        appIsStopped()
    }

    @NotYetImplemented
    def "can modify play app while app is running in continuous build"() {
        when:
        succeeds("runPlayBinary")

        then:
        appIsRunningAndDeployed()

        when:
        addHelloWorld()

        then:
        succeeds()
        runningApp.playUrl('hello').text == 'Hello world'

        cleanup: "stopping gradle"
        stopGradle()
        appIsStopped()
    }

    def "can modify play app before it has been started"() {
        when:
        addHelloWorld()
        succeeds("runPlayBinary")

        then:
        appIsRunningAndDeployed()
        runningApp.playUrl('hello').text == 'Hello world'

        cleanup: "stopping gradle"
        stopGradle()
        appIsStopped()
    }

    private void addHelloWorld() {
        file("conf/routes") << "\nGET     /hello                   controllers.Application.hello"
        file("app/controllers/Application.scala").with {
            text = text.replaceFirst(/(?s)\}\s*$/, '''
  def hello = Action {
    Ok("Hello world")
  }
}
''')
        }
    }

    def "build failure prior to launch does not prevent launch on subsequent build" () {
        def original = file("app/controllers/Application.scala").text

        when: "source file is broken"
        file("app/controllers/Application.scala").text = "object Application extends Controller {"

        then:
        fails("runPlayBinary")

        when: "source file is fixed"
        file("app/controllers/Application.scala").text = original

        then:
        succeeds()

        and:
        appIsRunningAndDeployed()

        cleanup: "stopping gradle"
        stopGradle()
        appIsStopped()
    }

    def "play application is stopped when build is cancelled" () {
        when:
        succeeds("runPlayBinary")

        then:
        appIsRunningAndDeployed()

        when:
        println "sending ctrl-d"
        stdinPipe.write(4) // ctrl-d

        then:
        cancelsAndExits()

        and:
        appIsStopped()
    }

    def "play run task blocks when not using continuous build" () {
        when:
        executer.withStdIn(System.in)
        gradle = executer.withTasks("runPlayBinary").withForceInteractive(true).start()

        then:
        appIsRunningAndDeployed()

        and:
        buildIsBlocked()

        when:
        println "sending ctrl-d"
        stdinPipe.write(4) // ctrl-d

        then:
        buildFinishes()

        and:
        appIsStopped()
    }

    def buildFinishes() {
        new PollingConditions().within(shutdownTimeout) {
            assert !gradle.running
        }
        true
    }

    def buildIsBlocked() {
        doesntExit()
        assert ! buildOutputSoFar().contains("BUILD SUCCESSFUL")
        assert ! buildOutputSoFar().contains("BUILD FAILED")
        true
    }
}
