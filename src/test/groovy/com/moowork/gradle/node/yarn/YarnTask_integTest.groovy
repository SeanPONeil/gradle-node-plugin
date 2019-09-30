package com.moowork.gradle.node.yarn

import com.moowork.gradle.AbstractIntegTest
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables

class YarnTask_integTest
        extends AbstractIntegTest {
    @Rule
    EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def 'download node and yarn, execute yarn command with a package.json file and check inputs up-to-date detection'() {
        given:
        copyResources('fixtures/yarn/', '')
        copyResources('fixtures/javascript-project/', '')

        when:
        def result1 = build(":test")

        then:
        result1.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":yarnSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":yarn").outcome == TaskOutcome.SUCCESS
        result1.task(":test").outcome == TaskOutcome.SUCCESS
        result1.output.contains("1 passing")

        when:
        def result2 = build(":test")

        then:
        result2.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":yarn").outcome == TaskOutcome.SUCCESS
        result2.task(":test").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result3 = build(":test", "-DchangeInputs=true")

        then:
        result3.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":test").outcome == TaskOutcome.SUCCESS
    }

    def 'execute yarn command with custom execution configuration and check up-to-date-detection'() {
        given:
        copyResources('fixtures/yarn-env/', '')
        copyResources('fixtures/env/', '')

        when:
        def result1 = build(":env")

        then:
        result1.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result1.task(":yarnSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":yarn").outcome == TaskOutcome.SUCCESS
        result1.task(":env").outcome == TaskOutcome.SUCCESS
        result1.output.contains("PATH=")

        when:
        def result2 = build(":env", "-DcustomEnv=true")

        then:
        result2.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result2.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":yarn").outcome == TaskOutcome.SUCCESS
        result2.task(":env").outcome == TaskOutcome.SUCCESS
        result2.output.contains("CUSTOM=custom value")

        when:
        environmentVariables.set("NEW_ENV_VARIABLE", "Let's make the whole environment change")
        def result3 = build(":env", "-DcustomEnv=true")

        then:
        result3.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result3.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":env").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result4 = build(":env", "-DignoreExitValue=true", "-DnotExistingCommand=true")

        then:
        result4.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result4.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result4.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result4.task(":env").outcome == TaskOutcome.SUCCESS
        result4.output.contains("error Command \"notExistingCommand\" not found.")

        when:
        def result5 = buildAndFail(":env", "-DnotExistingCommand=true")

        then:
        result5.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result5.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result5.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result5.task(":env").outcome == TaskOutcome.FAILED
        result5.output.contains("error Command \"notExistingCommand\" not found.")

        when:
        def result6 = build(":pwd")

        then:
        result6.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result6.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result6.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result6.task(":pwd").outcome == TaskOutcome.SUCCESS
        result6.output.contains("Working directory is '${projectDir}'")

        when:
        def result7 = build(":pwd", "-DcustomWorkingDir=true")

        then:
        result7.task(":nodeSetup").outcome == TaskOutcome.SKIPPED
        result7.task(":yarnSetup").outcome == TaskOutcome.UP_TO_DATE
        result7.task(":yarn").outcome == TaskOutcome.UP_TO_DATE
        result7.task(":pwd").outcome == TaskOutcome.SUCCESS
        def expectedWorkingDirectory = "${projectDir}${File.separator}build${File.separator}customWorkingDirectory"
        result7.output.contains("Working directory is '${expectedWorkingDirectory}'")
    }
}
