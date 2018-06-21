package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.api.model.Pkg
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpResponse
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
        String bintraySubject = getSubject()
        for (String v : versions) {
            if (bintray.subject(bintraySubject).repository(repoName)
                    .pkg(config.pkgName).version(v).exists()) {
                bintray.subject(bintraySubject).repository(repoName)
                        .pkg(config.pkgName).version(v).delete()
            }
        }

        boolean pkgExists = bintray.subject(bintraySubject).repository(repoName)
                .pkg(config.pkgName).exists()

        if (pkgExists) {
            // Delete the package:
            bintray.subject(bintraySubject).repository(repoName)
                    .pkg(config.pkgName).delete()
        }
    }

    def "[fileSpec]create debian package and version with fileSpec"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.debianRepo as String, config.pkgName as String, version)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.debianRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[fileSpec]create package and version with fileSpec"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.mavenRepo as String, config.pkgName as String, version)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[configuration]create package and version with configuration"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        String[] tasks = ["clean", "install", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.mavenRepo as String, config.pkgName as String, version)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

    def "[configurationWithSubModules]create package and version with configuration"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        String[] tasks = ["clean", "install", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName + " without username", tasks, version)

        then:
        // Gradle build finished with error:
        exitCode != 0

        when:
        version = PluginSpecUtils.createVersion(versions)
        tasks = ["clean", "install", "bintrayUpload"]
        exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.mavenRepo as String, config.pkgName as String, version)

        // Check that the jar file was uploaded with the right artifactId
        String pathApi = "/$bintraySubject/${config.mavenRepo}/bintray/gradle/test/android-maven-api/0.1/android-maven-api-0.1.jar"
        checkExistenceOnBintray(pathApi)

        String pathShared = "/$bintraySubject/${config.mavenRepo}/bintray/gradle/test/android-maven-shared/0.1/android-maven-shared-0.1.jar"
        checkExistenceOnBintray(pathShared)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()

        // Remove the previously created version
        bintray.subject(bintraySubject).repository(config.mavenRepo)
                .pkg(config.pkgName).version(version).delete()

        when:
        version = PluginSpecUtils.createVersion(versions)
        // Run only the api project
        tasks = ["clean", "install", ":api:bintrayUpload"]
        exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Check that the jar file wasn't uploaded for the shared and root
        !checkExistenceOnBintray(pathShared)
    }

    def "[publication]create package and version with publication"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        savedVersion = version
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.mavenRepo as String, config.pkgName as String, version)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.mavenRepo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }

  def "[publicationWithJavaGradlePlugin]create package and version with publication"() {
        when:
        String version = PluginSpecUtils.createVersion(versions)
        savedVersion = version
        String[] tasks = ["clean", "build", "bintrayUpload"]
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName, tasks, version)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        String bintraySubject = getSubject()
        checkExistence(bintraySubject, config.mavenRepo as String, config.pkgName as String, version)

        when:
        // Get the created package:
        Pkg pkg = bintray.subject(bintraySubject).repository(config.mavenRepo)
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
        String bintraySubject = getSubject()
        checkExistenceOnBintray("/$bintraySubject/${config.debianRepo}/dists/squeeze/main/binary-amd64/Packages")
    }

    private String getSubject() {
        return config.bintrayOrg ?: config.bintrayUser
    }

    private boolean checkExistence(String bintraySubject, String repo, String pkgName, String version) {
        // Package was created:
        if (!bintray.subject(bintraySubject).repository(repo)
                .pkg(pkgName).exists()) {
            return false
        }

        // Version was created:
        if (!bintray.subject(bintraySubject).repository(repo)
                .pkg(pkgName).version(version).exists()) {
            return false
        }
        return true
    }

    private boolean checkExistenceOnBintray(String path) {
        HTTPBuilder httpBuilder = BintrayHttpClientFactory.create("https://dl.bintray.com/", config.bintrayUser, config.bintrayKey)

        HttpResponse httpResponse = httpBuilder.request(HEAD) {
            Utils.addHeaders(headers)
            uri.path = path
            response.success = { resp ->
                return resp
            }
            response.failure = { resp ->
                return resp
            }
        }

        switch (httpResponse.getStatusLine().getStatusCode()) {
            case 200:
                return true
            case 404:
                return false
            default:
                throw new GradleException(
                        "Received unexpected response from Bintray while trying to determine if $path exists.: "
                                + httpResponse.getStatusLine())
        }
    }
}