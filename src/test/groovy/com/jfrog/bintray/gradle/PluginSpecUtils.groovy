package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.impl.BintrayClient

/**
 * Created by user on 20/11/2014.
 */
class PluginSpecUtils {
    private static Bintray bintrayClient
    private static def config = TestsConfig.getInstance().confog

    def static getGradleCommandPath() {
        System.getenv("GRADLE_HOME") + File.separator + "bin" + File.separator + "gradle.bat"
    }

    private static def getGradleProjectFile() {
        def resource = getClass().getResource("/gradle/build.gradle")
        new File(resource.toURI())
    }

    def static GradleLauncher createGradleLauncher() {
        File projectFile = getGradleProjectFile()
        def gradleLogPath = projectFile.getParentFile().getCanonicalPath()
        GradleLauncher launcher = new GradleLauncher(
                getGradleCommandPath(), projectFile.getCanonicalPath(), gradleLogPath)
                .addTask("clean")
                .addTask("bintrayUpload")
                .addEnvVar("bintrayUser", config.bintrayUser)
                .addEnvVar("bintrayKey", config.bintrayApiKey)
                .addEnvVar("repoName", config.repo)
                .addEnvVar("pkgName", config.pkgName)
                .addEnvVar("pkgDesc", config.pkgDesc)
                .addEnvVar("versionName", config.versionName)
                .addSwitch("stacktrace")

        int i=1
        for(label in config.pkgLabels) {
            launcher.addEnvVar("label${i}", label)
            i++
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
                    config.bintrayUser as String, config.bintrayApiKey as String)
        }
        bintrayClient
    }
}
