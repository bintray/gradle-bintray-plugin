package com.jfrog.bintray.gradle

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class BintrayExtension {

    Project project

    String apiUrl

    String user

    String key

    PackageConfig pkg = new PackageConfig()

    String[] configurations

    String[] publications

    RecordingCopyTask filesSpec

    boolean publish

    boolean dryRun

    BintrayExtension(Project project) {
        this.project = project
    }

    def pkg(Closure closure) {
        ConfigureUtil.configure(closure, pkg)
    }

    def filesSpec(Closure closure) {
        filesSpec = project.task(type: RecordingCopyTask, RecordingCopyTask.NAME)
        ConfigureUtil.configure(closure, filesSpec)
        filesSpec.outputs.upToDateWhen { false }
    }

    class PackageConfig {
        String repo
        //An alternative user for the package
        String userOrg
        String name
        String desc
        String websiteUrl
        String issueTrackerUrl
        String vcsUrl
        boolean publicDownloadNumbers
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
        String vcsTag
    }
}
