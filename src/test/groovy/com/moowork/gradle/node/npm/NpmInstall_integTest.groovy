package com.moowork.gradle.node.npm

import com.moowork.gradle.AbstractIntegTest
import org.gradle.testkit.runner.TaskOutcome

class NpmInstall_integTest
    extends AbstractIntegTest
{
    def 'install packages with npm'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
            }
        ''' )
        writeEmptyPackageJson()

        when:
        def result = build( 'npmInstall', '--stacktrace' )

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result.task(":npmSetup").outcome == TaskOutcome.SUCCESS
        result.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build( 'npmInstall' )

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build( 'npmInstall')

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE
    }

    def 'install packages with npm and postinstall task requiring npm and node'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }
            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
            }
        ''' )
        writePackageJson(""" {
            "name": "example",
            "dependencies": {},
            "versionOutput" : "node --version",
            "postinstall" : "npm run versionOutput"
        }
        """)

        when:
        def result = build( 'npmInstall' )

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result.task(":npmSetup").outcome == TaskOutcome.SUCCESS
        result.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build( 'npmInstall' )

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build( 'npmInstall' )

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE
    }

    def 'install packages with npm in different directory'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
                nodeModulesDir = file('subdirectory')
            }
        ''' )
        writeFile( 'subdirectory/package.json', """{
            "name": "example",
            "dependencies": {
            }
        }""" )

        when:
        def result = build( 'npmInstall' )

        then:
        result.task( ':npmInstall' ).outcome == TaskOutcome.SUCCESS
    }

    def 'configure npm install to use the ci command through extension'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
                npmInstallCommand = 'ci'
            }
        ''' )
        writeEmptyPackageJson()

        when:
        def result = buildAndFail( 'npmInstall' )

        then:
        result.output.contains('can only install packages with an existing package-lock.json')
        result.task(':npmInstall').outcome == TaskOutcome.FAILED

        when:
        writeEmptyLockFile()
        result = buildTask( 'npmInstall' )

        then:
        result.outcome == TaskOutcome.SUCCESS
    }

    def 'verify npm install inputs/outputs'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
                npmInstallCommand = 'install'
            }

            task verifyIO {
                doLast {
                    if (!tasks.named("npmInstall").get().outputs.files.contains(project.file('package-lock.json'))) {
                        throw new RuntimeException("package-lock.json is not in INSTALL'S outputs!")
                    }
                    if (tasks.named("npmInstall").get().inputs.files.contains(project.file('package-lock.json'))) {
                        throw new RuntimeException("package-lock.json is in INSTALL'S inputs!")
                    }
                }
            }
        ''' )
        writeEmptyPackageJson()
        writeFile('package-lock.json', '')

        when:
        def result = buildTask( 'verifyIO' )

        then:
        result.outcome == TaskOutcome.SUCCESS
    }

    def 'verify npm ci inputs/outputs'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
                npmInstallCommand = 'ci'
            }

            task verifyIO {
                doLast {
                    if (tasks.named("npmInstall").get().outputs.files.contains(project.file('package-lock.json'))) {
                        throw new RuntimeException("package-lock.json is in CI'S outputs!")
                    }
                    if (!tasks.named("npmInstall").get().inputs.files.contains(project.file('package-lock.json'))) {
                        throw new RuntimeException("package-lock.json is not in CI'S inputs!")
                    }
                }
            }
        ''' )
        writeEmptyPackageJson()
        writeEmptyLockFile()

        when:
        def result = buildTask( 'verifyIO' )

        then:
        result.outcome == TaskOutcome.SUCCESS
    }

    def 'verity output configuration'()
    {
        given:
        writeBuild( '''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                version = "10.14.0"
                npmVersion = "6.4.1"
                download = true
                workDir = file('build/node')
            }
            
            def changeOutput = System.properties["changeOutput"] ? System.properties["changeOutput"] == "true" : false
            if (changeOutput) {
                npmInstall {
                    nodeModulesOutputFilter = { it.exclude("mocha/package.json") }
                }
            }
        ''' )
        writePackageJson("""
            {
              "name": "hello",
              "dependencies": {
                "mocha": "6.2.0"
              }
            }
        """)

        when:
        def result1 = build("npmInstall")

        then:
        result1.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        def result2 = build("npmInstall")

        then:
        // Because package-lock.json was created
        result2.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        // Let's add a file in the node_modules directory
        writeFile("node_modules/mocha/newFile.txt", "hello")
        def result3 = build("npmInstall")

        then:
        // It should not make the build out-of-date
        result3.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's update a file in the node_modules directory
        writeFile("node_modules/mocha/package.json", "modified package.json")
        def result4 = build("npmInstall")

        then:
        // This time the build should not be up-to-date and the file should be reset
        result4.task(":npmInstall").outcome == TaskOutcome.SUCCESS
        createFile("node_modules/mocha/package.json").text != "modified package.json"

        when:
        // Let's delete a file in the node_modules directory
        createFile("node_modules/mocha/package.json").delete()
        def result5 = build("npmInstall")

        then:
        // This time the build should be up-to-date and the file should be reset
        result5.task(":npmInstall").outcome == TaskOutcome.SUCCESS
        createFile("node_modules/mocha/package.json").exists()

        when:
        createFile("node_modules").deleteDir()
        def result6 = build("npmInstall", "-DchangeOutput=true")

        then:
        result6.task(":npmInstall").outcome == TaskOutcome.SUCCESS

        when:
        // Let's add a file in the node_modules directory
        writeFile("node_modules/mocha/newFile.txt", "hello")
        def result7 = build("npmInstall", "-DchangeOutput=true")

        then:
        // It should not make the build out-of-date
        result7.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's update a file in the node_modules directory
        writeFile("node_modules/mocha/package.json", "modified package.json")
        def result8 = build("npmInstall", "-DchangeOutput=true")

        then:
        // The build should still be up-to-date
        result8.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's delete an excluded file in the node_modules directory
        createFile("node_modules/mocha/package.json").delete()
        def result9 = build("npmInstall", "-DchangeOutput=true")

        then:
        // The build should still be up-to-date
        result9.task(":npmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's delete a not excluded file in the node_modules directory
        createFile("node_modules/mocha/mocha.js").delete()
        def result10 = build("npmInstall", "-DchangeOutput=true")

        then:
        // This time the build should not be up-to-date since not the whole node_modules directory is excluded
        result10.task(":npmInstall").outcome == TaskOutcome.SUCCESS
    }

    protected final void writeEmptyLockFile()
    {
        writeFile('package-lock.json', '''
            {
              "name": "example",
              "lockfileVersion": 1,
              "requires": true,
              "dependencies": {}
            }
        ''')
    }
}
