package com.jfrog.bintray.gradle

import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Upload

class BintrayPlugin implements Plugin<Project> {

    private Project project

    public void apply(Project project) {
        this.project = project;
        def extension = project.extensions.create("bintray", BintrayExtension, project)
        extension.with {
            apiUrl = BintrayUploadTask.API_URL_DEFAULT
        }

        //Create and configure the task
        BintrayUploadTask bintrayUpload = project.tasks.create(BintrayUploadTask.NAME, BintrayUploadTask)
        //Depend on tasks in sub-projects
        project.subprojects.each {
            def subTask = project.tasks.withType(BintrayUploadTask)
            if (subTask) {
                dependsOn(subTask)
            }
        }

        def projectAdapter = [
                bintrayUpload: bintrayUpload,
                projectsEvaluated: { Gradle gradle ->
                    bintrayUpload.with {
                        apiUrl = extension.apiUrl
                        user = extension.user
                        apiKey = extension.key
                        configurations = extension.configurations
                        publications = extension.publications
                        dryRun = extension.dryRun
                        userOrg = extension.pkg.userOrg ?: extension.user
                        repoName = extension.pkg.repo
                        packageName = extension.pkg.name
                        packageDesc = extension.pkg.desc
                        packageLicense = extension.pkg.license
                        packageLabels = extension.pkg.labels
                        versionName = extension.pkg.version.name ?: project.version
                    }
                    if (extension.configurations?.length) {
                        Upload installTask = project.tasks.withType(Upload).findByName('install')
                        if (installTask) {
                            bintrayUpload.dependsOn(installTask)
                        }
                    }
                    if (extension.publications?.length) {
                        extension.publications.each {
                            Publication publication = project.extensions.getByType(PublishingExtension).publications.findByName(it)
                            if (publication instanceof MavenPublication) {
                                def taskName = "generatePomFileFor${it[0].toUpperCase()}${it.substring(1)}Publication"
                                Task publishToLocalTask = project.tasks.findByName(taskName)
                                bintrayUpload.dependsOn(publishToLocalTask)
                                /*bintrayUpload.dependsOn(publication.publishableFiles)*/
                            } else {
                                project.logger.warn "{} can only use maven publications - skipping {}.",
                                        bintrayUpload.path, publication.name
                            }
                        }
                    }
                }
        ] as BuildAdapter
        project.gradle.addBuildListener(projectAdapter)
    }
}