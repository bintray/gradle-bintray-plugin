package com.jfrog.bintray.gradle

import org.gradle.api.Action
import org.gradle.api.Project

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

    boolean override

    boolean dryRun

    BintrayExtension(Project project) {
        this.project = project
    }

    def pkg(Action<? super PackageConfig> action) {
        action.execute(pkg)
    }

    def filesSpec(Action<? super RecordingCopyTask> action) {
        filesSpec = project.tasks.create(RecordingCopyTask.NAME, RecordingCopyTask)
        action.execute(filesSpec)
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
        String githubRepo
        String githubReleaseNotesFile
        boolean publicDownloadNumbers
        String[] licenses
        String[] labels
        Map attributes

        VersionConfig version = new VersionConfig()
        def version(Action<? super VersionConfig> action) {
            action.execute(version)
        }

        DebianConfig debian = new DebianConfig()
        def debian(Action<? super DebianConfig> action) {
            action.execute(debian)
        }
    }

    class DebianConfig {
        String distribution
        String component
        String architecture
    }

    class VersionConfig {
        String name
        String desc
        String released
        String vcsTag
        Map attributes

        GpgConfig gpg = new GpgConfig()
        def gpg(Action<? super GpgConfig> action) {
            action.execute(gpg)
        }

        MavenCentralSyncConfig mavenCentralSync = new MavenCentralSyncConfig()
        def mavenCentralSync(Action<? super MavenCentralSyncConfig> action) {
            action.execute(mavenCentralSync)
        }
    }

    class GpgConfig {
        boolean sign
        String passphrase
    }

    class MavenCentralSyncConfig {
        Boolean sync
        String user
        String password
        String close
    }
}
