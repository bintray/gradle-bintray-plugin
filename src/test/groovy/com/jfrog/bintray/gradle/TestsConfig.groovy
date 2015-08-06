package com.jfrog.bintray.gradle

import static java.lang.System.getenv

/**
 * Created by Eyal BM on 22/12/2014.
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
        if (!bintrayUser) {
            throwMissingConfProp('Bintray user name', 'BINTRAY_USER', 'bintrayUser')
        }
        config.bintrayUser = bintrayUser

        def bintrayKey = getenv('BINTRAY_KEY')
        if (!bintrayKey) {
            bintrayKey = System.getProperty('bintrayKey')
        }
        if (!bintrayKey) {
            throwMissingConfProp('Bintray API key', 'BINTRAY_KEY', 'bintrayKey')
        }
        config.bintrayKey = bintrayKey

        def bintrayAdminUser = getenv('BINTRAY_ADMIN_USER')
        if (!bintrayAdminUser) {
            bintrayAdminUser = System.getProperty('bintrayAdminUser')
        }
        if (!bintrayAdminUser) {
            throwMissingConfProp('Bintray admin user name (used for linking the package to JCenter)', 'BINTRAY_ADMIN_USER', 'bintrayAdminUser')
        }
        config.bintrayAdminUser = bintrayAdminUser

        def bintrayAdminKey = getenv('BINTRAY_ADMIN_KEY')
        if (!bintrayAdminKey) {
            bintrayAdminKey = System.getProperty('bintrayAdminKey')
        }
        if (!bintrayAdminKey) {
            throwMissingConfProp('Bintray admin API key (used for linking the package to JCenter)', 'BINTRAY_ADMIN_KEY', 'bintrayAdminKey')
        }
        config.bintrayAdminKey = bintrayAdminKey

        def mavenCentralUser = getenv('MAVEN_CENTRAL_USER')
        if (!mavenCentralUser) {
            mavenCentralUser = System.getProperty('mavenCentralUser')
        }
        if (!mavenCentralUser) {
            throwMissingConfProp('Maven Central user', 'MAVEN_CENTRAL_USER', 'mavenCentralUser')
        }
        config.mavenCentralUser = mavenCentralUser

        def mavenCentralPassword = getenv('MAVEN_CENTRAL_PASSWORD')
        if (!mavenCentralPassword) {
            mavenCentralPassword = System.getProperty('mavenCentralPassword')
        }
        if (!mavenCentralPassword) {
            throwMissingConfProp('Maven Central password', 'MAVEN_CENTRAL_PASSWORD', 'mavenCentralPassword')
        }
        config.mavenCentralPassword = mavenCentralPassword

        def bintrayApiUrl = getenv('BINTRAY_API_URL')
        if (!bintrayApiUrl) {
            bintrayApiUrl = System.getProperty('bintrayApiUrl')
        }

        if (config.url == [:]) {
            config.url = bintrayApiUrl ?: "https://api.bintray.net"
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
        if (config.pkgLabels == [:]) {
            config.pkgLabels = ['a','b','c']
        }
    }

    private throwMissingConfProp(String name, String envVarName, String sysPropName) {
        throw new Exception("The ${name} was not configured. Please set it using the '${envVarName}' environment variable or the '${sysPropName}' system property.")
    }
}