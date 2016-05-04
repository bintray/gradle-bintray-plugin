package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.api.model.Pkg
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Shared
import spock.lang.Specification

import static groovyx.net.http.Method.*

class GradleBintrayPluginSpec extends Specification {

    @Rule
    TestName testName = new TestName()
    @Shared
    private Bintray bintray = PluginSpecUtils.getBintrayClient()
    @Shared
    private def config = TestsConfig.getInstance().config
    @Shared
    private savedVersion = null

    def setupSpec() {
        assert config
        assert config.url
        assert config.bintrayUser
        assert config.bintrayKey

        File gradleCommand = new File(PluginSpecUtils.getGradleCommandPath())
        assert gradleCommand.exists() && !gradleCommand.isDirectory()

        def config = TestsConfig.getInstance().config
        assert config.bintrayUser
        assert config.bintrayKey

        cleanupSpec()
    }

    def cleanupSpec() {
        if (savedVersion) {
            if (bintray.currentSubject().repository(config.repo)
                    .pkg(config.pkgName).version(savedVersion).exists()) {
                bintray.currentSubject().repository(config.repo)
                        .pkg(config.pkgName).version(savedVersion).delete()
            }
        }

        boolean pkgExists = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        if (pkgExists) {
            // Delete the package:
            bintray.currentSubject().repository(config.repo)
                    .pkg(config.pkgName).delete()
        }
    }

    def "[configuration]create package and version with configuration"() {
        when:
        String version = PluginSpecUtils.createVersion()
        String[] tasks = ["clean", "install", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[fileSpec]create package and version with fileSpec"() {
        when:
        String version = PluginSpecUtils.createVersion()
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[publication]create package and version with publication"() {
        when:
        String version = PluginSpecUtils.createVersion()
        savedVersion = version
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[publication]override"() {
        setup:
        String[] tasks = ["clean", "build", "bintrayUpload"]

        when:
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks,
            savedVersion)

        then:
        // Gradle build fails, because override is blocked by default:
        exitCode != 0

        when:
        exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks,
            savedVersion, false)

        then:
        // Gradle build still fails, because we set override to false:
        exitCode != 0

        when:
        exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks,
                savedVersion, true)

        then:
        // Gradle build finished successfully:
        exitCode == 0
    }

    /* This test is disabled for now
    def "[publication]maven central sync"() {
        when:
        println("Waiting 60 seconds before linking the package to jcenter...")
        Thread.sleep(60000)
        PluginSpecUtils.linkPackageToJCenter()
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks,
            savedVersion)

        then:
        // Gradle build finished successfully:
        exitCode == 0
    }
    */
}