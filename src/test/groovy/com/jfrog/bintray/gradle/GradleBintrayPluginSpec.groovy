package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.api.model.Pkg
import groovyx.net.http.HTTPBuilder
import org.gradle.api.GradleException
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
    // Saves the version to test the version override functionality:
    private savedVersion = null
    @Shared
    private List<String> versions = new ArrayList<>()

    def setupSpec() {
        assert config
        assert config.url
        assert config.bintrayUser
        assert config.bintrayKey

        File gradleCommand = new File(PluginSpecUtils.getGradleCommandPath())
        assert gradleCommand.isFile()

        def config = TestsConfig.getInstance().config
        assert config.bintrayUser
        assert config.bintrayKey

        cleanupSpec()
    }

    def cleanupSpec() {
        String[] repositories = [config.mavenRepo, config.debianRepo]
        for (String repo : repositories) {
            cleanupRepo(repo)
        }
    }

    def cleanupRepo(String repoName) {
        for (String v : versions) {
            if (bintray.subject(config.bintrayOrg).repository(repoName)
                    .pkg(config.pkgName).version(v).exists()) {
                bintray.subject(config.bintrayOrg).repository(repoName)
                        .pkg(config.pkgName).version(v).delete()
            }
        }

        boolean pkgExists = bintray.subject(config.bintrayOrg).repository(repoName)
                .pkg(config.pkgName).exists()

        if (pkgExists) {
            // Delete the package:
            bintray.subject(config.bintrayOrg).repository(repoName)
                    .pkg(config.pkgName).delete()
        }
    }

    def "[fileSpec]create debian package and version with fileSpec"() {
        when:
        String version = PluginSpecUtils.createVersion()
        versions.add(version)
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.subject(config.bintrayOrg).repository(config.debianRepo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.subject(config.bintrayOrg).repository(config.debianRepo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(config.bintrayOrg).repository(config.debianRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[fileSpec]create package and version with fileSpec"() {
        when:
        String version = PluginSpecUtils.createVersion()
        versions.add(version)
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[configuration]create package and version with configuration"() {
        when:
        String version = PluginSpecUtils.createVersion()
        versions.add(version)
        String[] tasks = ["clean", "install", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[publication]create package and version with publication"() {
        when:
        String version = PluginSpecUtils.createVersion()
        versions.add(version)
        savedVersion = version
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
                .pkg(config.pkgName).version(version).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(config.bintrayOrg).repository(config.mavenRepo)
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

    def "debian package indexed"() {
        // Check that the file uploaded in the
        // "[fileSpec]create debian package and version with fileSpec" test
        // was indeed accepted by Bintray as a debian package.
        // In case it was, then Bintray had successfully indexed the debian package and
        // therefore the following path should exist on Bintray.

        expect:
        fileExistsOnBintray(config.bintrayOrg as String, config.debianRepo as String,
                '/dists/squeeze/main/binary-amd64/Packages')
    }

    private boolean fileExistsOnBintray(String org, String repo, String path) {
        HTTPBuilder http = BintrayHttpClientFactory.create('https://dl.bintray.com',
                config.bintrayUser, config.bintrayKey)
        boolean exists
        http.request(HEAD) {
            Utils.addHeaders(headers)
            uri.path = "/$org/$repo/$path"
            response.success = { resp ->
                exists = true
            }
            response.'404' = { resp ->
                exists = false
            }
            response.failure = { resp, reader ->
                throw new GradleException(
                        "Received unexpected response from Bintray while trying to determine if $path exists.: $resp.statusLine $reader")
            }
        }
        return exists
    }
}