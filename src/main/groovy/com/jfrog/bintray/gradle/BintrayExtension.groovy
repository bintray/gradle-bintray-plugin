package com.jfrog.bintray.gradle

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class BintrayExtension {

    Project project

    String apiUrl

    String user

    String key

    PackageConfig pkg = new PackageConfig()

    List<String> configurations = new ArrayList<>()

    List<String> publications = new ArrayList<>()

    RecordingCopyTask filesSpec

    boolean publish

    boolean override

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
        String githubRepo
        String githubReleaseNotesFile
        boolean publicDownloadNumbers
        List<String> licenses = new ArrayList<>()
        List<String> labels = new ArrayList<>()
        Map attributes = new HashMap()

        VersionConfig version = new VersionConfig()
        def version(Closure closure) {
            ConfigureUtil.configure(closure, version)
        }

        DebianConfig debian = new DebianConfig()
        def debian(Closure closure) {
            ConfigureUtil.configure(closure, debian)
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
        def gpg(Closure closure) {
            ConfigureUtil.configure(closure, gpg)
        }

        MavenCentralSyncConfig mavenCentralSync = new MavenCentralSyncConfig()
        def mavenCentralSync(Closure closure) {
            ConfigureUtil.configure(closure, mavenCentralSync)
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