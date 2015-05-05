package com.jfrog.bintray.gradle

import static java.lang.System.getenv

/**
 * Created by user on 22/12/2014.
 */
class TestsConfig {
    private static TestsConfig instance = new TestsConfig()
    public def config

    static TestsConfig getInstance() {
        return instance
    }

    private TestsConfig() {
        config = new ConfigSlurper().parse(this.class.getResource('/gradle/config.groovy')).conf

        def bintrayUser = getenv('BINTRAY_USER')
        if (!bintrayUser) {
            bintrayUser = System.getProperty('bintrayUser')
        }
        if (bintrayUser) {
            config.bintrayUser = bintrayUser
        }

        def bintrayKey = getenv('BINTRAY_KEY')
        if (!bintrayKey) {
            bintrayKey = System.getProperty('bintrayKey')
        }
        if (bintrayKey) {
            config.bintrayKey = bintrayKey
        }
        if (config.url == [:]) {
            config.url = "https://api.bintray.com"
        }
        if (config.repo == [:]) {
            config.repo = "maven"
        }
        if (config.pkgName == [:]) {
            config.pkgName = "gradle.tests.pkg.name"
        }
        if (config.pkgDesc == [:]) {
            config.pkgDesc = "gradle.tests.pkg.description"
        }
        if (config.versionName == [:]) {
            config.versionName = "9.8.7"
        }
        if (config.pkgLabels == [:]) {
            config.pkgLabels = ['a','b','c']
        }
    }
}