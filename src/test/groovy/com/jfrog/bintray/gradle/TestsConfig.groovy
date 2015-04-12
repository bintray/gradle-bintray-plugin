package com.jfrog.bintray.gradle

import static java.lang.System.getenv

/**
 * Created by user on 22/12/2014.
 */
class TestsConfig {
    private static TestsConfig instance = new TestsConfig()
    public def confog

    static TestsConfig getInstance() {
        return instance
    }

    private TestsConfig() {
        confog = new ConfigSlurper().parse(this.class.getResource('/gradle/config.groovy')).conf

        def bintrayUser = getenv('BINTRAY_USER')
        if (bintrayUser) {
            confog.bintrayUser = bintrayUser
        }
        def bintrayApiKey = getenv('BINTRAY_KEY')
        if (bintrayApiKey) {
            confog.bintrayApiKey = bintrayApiKey
        }
        if (confog.url == [:]) {
            confog.url = "https://api.bintray.com"
        }
        if (confog.repo == [:]) {
            confog.repo = "maven"
        }
        if (confog.pkgName == [:]) {
            confog.pkgName = "gradle.tests.pkg.name"
        }
        if (confog.pkgDesc == [:]) {
            confog.pkgDesc = "gradle.tests.pkg.description"
        }
        if (confog.versionName == [:]) {
            confog.versionName = "9.8.7"
        }
        if (confog.pkgLabels == [:]) {
            confog.pkgLabels = ['a','b','c']
        }
    }
}