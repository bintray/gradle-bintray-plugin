package com.jfrog.bintray.gradle

import com.jfrog.bintray.client.api.handle.Bintray
import com.jfrog.bintray.client.api.model.Pkg
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Shared
import spock.lang.Specification

class GradleBintrayPluginSpec extends Specification {

    @Rule
    TestName testName = new TestName()
    @Shared
    private Bintray bintray = PluginSpecUtils.getBintrayClient()
    @Shared
    private def config = TestsConfig.getInstance().config

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
    }

    def cleanup() {
        boolean pkgExists = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        if (pkgExists) {
            // Delete the package:
            bintray.currentSubject().repository(config.repo)
                    .pkg(config.pkgName).delete()
        }
    }

    def "create package and version"() {
        when:
        def exitCode = PluginSpecUtils.launchGradle(testName.methodName)

        then:
        // Gradle build finished successfully:
        exitCode == 0

        // Package was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).exists()

        // Version was created:
        bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).version(config.versionName).exists()

        when:
        // Get the created package:
        Pkg pkg = bintray.currentSubject().repository(config.repo)
                .pkg(config.pkgName).get()

        then:
        pkg.name() == config.pkgName
        pkg.description() == config.pkgDesc
        pkg.labels().sort() == config.pkgLabels.sort()
    }
}