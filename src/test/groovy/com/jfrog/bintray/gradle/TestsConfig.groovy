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

        config.bintrayUser = readValue('Bintray user name', 'BINTRAY_USER', 'bintrayUser')
        config.bintrayKey = readValue('Bintray API key', 'BINTRAY_KEY', 'bintrayKey')
        config.bintrayOrg = readValue('Bintray organization', 'BINTRAY_ORG', 'bintrayOrg')
        config.bintrayAdminUser = readValue('Bintray admin user name (used for linking the package to JCenter)', 'BINTRAY_ADMIN_USER', 'bintrayAdminUser')
        config.bintrayAdminKey = readValue('Bintray admin API key (used for linking the package to JCenter)', 'BINTRAY_ADMIN_KEY', 'bintrayAdminKey')
        config.mavenCentralUser = readValue('Maven Central user', 'MAVEN_CENTRAL_USER', 'mavenCentralUser')
        config.mavenCentralPassword = readValue('Maven Central password', 'MAVEN_CENTRAL_PASSWORD', 'mavenCentralPassword')

        def bintrayApiUrl = getenv('BINTRAY_API_URL')
        if (!bintrayApiUrl) {
            bintrayApiUrl = System.getProperty('bintrayApiUrl')
        }

        if (config.url == [:]) {
            config.url = bintrayApiUrl ?: "https://api.bintray.com"
        }
        if (config.mavenRepo == [:]) {
            config.mavenRepo = "maven"
        }
        if (config.debianRepo == [:]) {
            config.debianRepo = "debian"
        }
        if (config.pkgName == [:]) {
            config.pkgName = "gradle.tests.pkg.name5"
        }
        if (config.pkgDesc == [:]) {
            config.pkgDesc = "gradle.tests.pkg.description"
        }
        if (config.pkgLabels == [:]) {
            config.pkgLabels = ['a','b','c']
        }
    }

    private String readValue(String displayName, String envVarName, String sysPropName) {
        String value = getenv(envVarName)
        if (!value) {
            value = System.getProperty(sysPropName)
        }
        if (!value) {
            throwMissingConfProp(displayName, envVarName, sysPropName)
        }
        value
    }

    private throwMissingConfProp(String name, String envVarName, String sysPropName) {
        throw new Exception("The ${name} was not configured. Please set it using the '${envVarName}' environment variable or the '${sysPropName}' system property.")
    }
}