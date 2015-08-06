package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.impl.BintrayClient
import groovyx.net.http.HTTPBuilder

import java.util.regex.Matcher

import static groovyx.net.http.Method.*

/**
 * Created by Eyal BM on 20/11/2014.
 */
class PluginSpecUtils {
    private static Bintray bintrayClient
    private static def config = TestsConfig.getInstance().config
    public static final OS_NAME = System.getProperty("os.name")

    def static String createVersion() {
        System.currentTimeMillis().toString()
    }

    def static getGradleCommandPath() {
        def windows = OS_NAME.contains("Windows")
        if (System.getenv("GRADLE_HOME")) {
            def ext = windows ? ".bat" : ""
            return System.getenv("GRADLE_HOME") + File.separator + "bin" + File.separator + "gradle" + ext
        }
        return windows ? "gradlew.bat" : "./gradlew"
    }

    private static def getGradleProjectFile(String projectName) {
        def resource = getClass().getResource("/gradle/projects/$projectName/build.gradle")
        new File(resource.toURI())
    }

    def static GradleLauncher createGradleLauncher(String projectName) {
        File projectFile = getGradleProjectFile(projectName)
        GradleLauncher launcher = new GradleLauncher(
                getGradleCommandPath(), projectFile.getCanonicalPath())
                .addTask("clean")
                .addTask("build")
                .addTask("bintrayUpload")
                .addEnvVar("bintrayApiUrl", config.url)
                .addEnvVar("bintrayUser", config.bintrayUser)
                .addEnvVar("bintrayKey", config.bintrayKey)
                .addEnvVar("repoName", config.repo)
                .addEnvVar("pkgName", config.pkgName)
                .addEnvVar("pkgDesc", config.pkgDesc)
                .addEnvVar("mavenCentralUser", config.mavenCentralUser)
                .addEnvVar("mavenCentralPassword", config.mavenCentralPassword)
                .addSwitch("stacktrace")

        config.pkgLabels.eachWithIndex { label, index ->
            launcher.addEnvVar("label${index+1}", label)
        }
        launcher
    }

    def static launchGradle(String testMethodName, String version = null) {
        String[] projectAndMethod = extractFromTestMethodName(testMethodName)
        String projectName = projectAndMethod[0]
        String testFileName = projectAndMethod[1].replaceAll(" ", "_")

        GradleLauncher launcher = createGradleLauncher(projectName).addEnvVar("testName", testFileName)
        if (version) {
            launcher.addEnvVar("versionName", version)
        }
        launcher.launch()
    }

    def static getBintrayClient() {
        if (bintrayClient == null) {
            bintrayClient = BintrayClient.create(config.url as String,
                    config.bintrayUser as String, config.bintrayKey as String)
        }
        bintrayClient
    }

    /**
     * Links the configured package to JCenter on Bintray.
     * @return
     */
    def static linkPackageToJCenter() {
        HTTPBuilder http = BintrayHttpClientFactory.create(config.url, config.bintrayAdminUser, config.bintrayAdminKey)

        http.request(PUT) {
            uri.path = "/repository/${config.bintrayAdminUser}/jcenter/links/${config.bintrayUser}/${config.repo}/${config.pkgName}"
            response.success = { resp ->
                println "Package '${config.pkgName}' was linked to JCenter."
            }
        }
    }

    /**
     * Gets the Spock test method name in the format of '[project name]test name'
     * and extracts from it the project name amd the test name.
     * @param methodName    The Spock test method name
     * @return  String array with two items:
     * [0] - The project name.
     * [1] - The test name.
     */
    def static String[] extractFromTestMethodName(String methodName) {
        Matcher matcher = (methodName =~ /\[(.+)\](.+)/ )
        if (matcher.matches()) {
            return [matcher.group(1), matcher.group(2)]
        }
        throw new Exception("Test method name '$methodName' has an invalid format. Should be '[project name]test name'.")
    }
}
