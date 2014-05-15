package com.jfrog.bintray.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.util.ConfigureUtil

class BintrayExtension {

    Project project

    String apiUrl

    String user

    String key

    PackageConfig pkg = new PackageConfig()

    String[] configurations

    String[] publications

    FileCollection files

    boolean dryRun

    BintrayExtension(Project project) {
        this.project = project
    }

    def pkg(Closure closure) {
        ConfigureUtil.configure(closure, pkg)
    }

    class PackageConfig {
        String repo
        String userOrg
        //An alternative user for the package
        String name
        String desc
        String[] licenses
        String[] labels

        VersionConfig version = new VersionConfig()

        def version(Closure closure) {
            ConfigureUtil.configure(closure, version)
        }
    }

    class VersionConfig {
        String name
        String desc
    }
}
