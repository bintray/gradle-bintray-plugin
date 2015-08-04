package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.impl.BintrayClient
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*

/**
 * Created by Eyal BM on 20/11/2014.
 */
class PluginSpecUtils {
    private static Bintray bintrayClient
    private static def config = TestsConfig.getInstance().config
    public static final OS_NAME = System.getProperty("os.name")
    public static TEST_PACKAGE_VERSION = null

    def static getOrCreatePackageVersion() {
        if (!TEST_PACKAGE_VERSION) {
            TEST_PACKAGE_VERSION = System.currentTimeMillis().toString()
        }
        TEST_PACKAGE_VERSION
    }

    def static getGradleCommandPath() {
        def windows = OS_NAME.contains("Windows")
        if (System.getenv("GRADLE_HOME")) {
            def ext = windows ? ".bat" : ""
            return System.getenv("GRADLE_HOME") + File.separator + "bin" + File.separator + "gradle" + ext
        }
        return windows ? "gradlew.bat" : "./gradlew"
    }

    private static def getGradleProjectDir() {
        def resource = getClass().getResource("/gradle")
        new File(resource.toURI())
    }

    private static def getGradleProjectFile() {
        def resource = getClass().getResource("/gradle/build.gradle")
        new File(resource.toURI())
    }

    def static GradleLauncher createGradleLauncher() {
        File projectFile = getGradleProjectFile()
        GradleLauncher launcher = new GradleLauncher(
                getGradleCommandPath(), projectFile.getCanonicalPath())
                .addTask("clean")
                .addTask("bintrayUpload")
                .addEnvVar("bintrayApiUrl", config.url)
                .addEnvVar("bintrayUser", config.bintrayUser)
                .addEnvVar("bintrayKey", config.bintrayKey)
                .addEnvVar("repoName", config.repo)
                .addEnvVar("pkgName", config.pkgName)
                .addEnvVar("pkgDesc", config.pkgDesc)
                .addEnvVar("versionName", config.versionName)
                .addEnvVar("mavenCentralUser", config.mavenCentralUser)
                .addEnvVar("mavenCentralPassword", config.mavenCentralPassword)
                .addSwitch("stacktrace")

        config.pkgLabels.eachWithIndex { label, index ->
            launcher.addEnvVar("label${index+1}", label)
        }
        launcher
    }

    def static launchGradle(String testMethodName) {
        def testFileName = testMethodName.replaceAll(" ", "_")
        createGradleLauncher().addEnvVar("testName", testFileName).launch()
    }

    def static getBintrayClient() {
        if (bintrayClient == null) {
            bintrayClient = BintrayClient.create(config.url as String,
                    config.bintrayUser as String, config.bintrayKey as String)
        }
        bintrayClient
    }

    def static linkPackageToJCenter() {
        HTTPBuilder http = BintrayHttpClientFactory.create(config.url, config.bintrayAdminUser, config.bintrayAdminKey)

        http.request(PUT) {
            uri.path = "/repository/${config.bintrayAdminUser}/jcenter/links/${config.bintrayUser}/${config.repo}/${config.pkgName}"
            response.success = { resp ->
                println "Package '${config.pkgName}' was linked to JCenter."
            }
        }
    }
}
