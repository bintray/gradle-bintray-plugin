package com.jfrog.bintray.gradle

import com.jfrog.bintray.gradle.tasks.BintrayPublishTask
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Upload

class ProjectsEvaluatedBuildListener implements ProjectEvaluationListener {
    private Project project
    private BintrayUploadTask bintrayUpload
    private BintrayExtension extension

    ProjectsEvaluatedBuildListener(Project project, BintrayUploadTask bintrayUpload) {
        this.project = project
        this.bintrayUpload = bintrayUpload
        extension = project.extensions.create("bintray", BintrayExtension, project)
        extension.with {
            apiUrl = BintrayUploadTask.API_URL_DEFAULT
        }
    }

    @Override
    void beforeEvaluate(Project project) {
    }

    @Override
    void afterEvaluate(Project proj, ProjectState state) {

        bintrayUpload.with {
            apiUrl = extension.apiUrl
            user = extension.user
            apiKey = extension.key
            configurations = extension.configurations
            publications = extension.publications
            filesSpec = extension.filesSpec
            publish = extension.publish
            override = extension.override
            dryRun = extension.dryRun
            userOrg = extension.pkg.userOrg ?: extension.user
            repoName = extension.pkg.repo
            packageName = extension.pkg.name
            packageDesc = extension.pkg.desc
            packageWebsiteUrl = extension.pkg.websiteUrl
            packageIssueTrackerUrl = extension.pkg.issueTrackerUrl
            packageVcsUrl = extension.pkg.vcsUrl
            packageGithubRepo = extension.pkg.githubRepo
            packageGithubReleaseNotesFile = extension.pkg.githubReleaseNotesFile
            packageLicenses = extension.pkg.licenses
            packageLabels = extension.pkg.labels
            packageAttributes = extension.pkg.attributes
            packagePublicDownloadNumbers = extension.pkg.publicDownloadNumbers
            debianDistribution = extension.pkg.debian.distribution
            debianComponent = extension.pkg.debian.component
            debianArchitecture = extension.pkg.debian.architecture
            versionName = extension.pkg.version.name ?: project.version
            versionDesc = extension.pkg.version.desc
            versionReleased = extension.pkg.version.released
            versionVcsTag = extension.pkg.version.vcsTag ?: project.version
            versionAttributes = extension.pkg.version.attributes
            signVersion = extension.pkg.version.gpg.sign
            gpgPassphrase = extension.pkg.version.gpg.passphrase
            syncToMavenCentral = extension.pkg.version.mavenCentralSync.sync == null ?
                    true : extension.pkg.version.mavenCentralSync.sync
            ossUser = extension.pkg.version.mavenCentralSync.user
            ossPassword = extension.pkg.version.mavenCentralSync.password
            ossCloseRepo = extension.pkg.version.mavenCentralSync.close
        }

        if (extension.configurations?.length) {
            extension.configurations.each {
                def configuration = project.configurations.findByName(it)
                if (!configuration) {
                    project.logger.warn "Configuration ${it} specified but does not exist in project {}.",
                            project.path
                } else {
                    bintrayUpload.dependsOn(configuration.allArtifacts)
                }
            }
            Upload installTask = project.tasks.withType(Upload)?.findByName('install')
            if (installTask) {
                bintrayUpload.dependsOn(installTask)
            } else {
                project.logger.warn "Configuration(s) specified but the install task does not exist in project {}.",
                        project.path
            }
        }
        if (extension.publications?.length) {
            def publicationExt = project.extensions.findByType(PublishingExtension)
            if (!publicationExt) {
                project.logger.warn "Publications(s) specified but no publications exist in project {}.",
                        project.path
            } else {
                extension.publications.each {
                    Publication publication = publicationExt?.publications?.findByName(it)
                    if (!publication) {
                        project.logger.warn 'Publication {} not found in project {}.', it, project.path
                    } else if (publication instanceof MavenPublication) {
                        def taskName =
                                "publish${it[0].toUpperCase()}${it.substring(1)}PublicationToMavenLocal"
                        bintrayUpload.dependsOn(taskName)
                    } else {
                        project.logger.warn "{} can only use maven publications - skipping {}.",
                                bintrayUpload.path, publication.name
                    }
                }
            }
        }

        Task deployTask = project.getRootProject().getTasks().findByName(BintrayPublishTask.TASK_NAME)
        if (deployTask == null) {
            throw new IllegalStateException(String.format("Could not find %s in the root project", BintrayPublishTask.TASK_NAME))
        }
        bintrayUpload.finalizedBy(deployTask)
        // Depend on tasks in sub-projects
        project.subprojects.each {
            Task subTask = it.tasks.findByName(BintrayUploadTask.TASK_NAME)
            if (subTask) {
                bintrayUpload.dependsOn(subTask)
            }
        }
        if (extension.filesSpec) {
            bintrayUpload.dependsOn(extension.filesSpec)
        }
    }
}
