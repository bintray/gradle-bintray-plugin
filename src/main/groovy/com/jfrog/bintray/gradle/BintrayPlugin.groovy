package com.jfrog.bintray.gradle

import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.GradleBuild
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
        bintrayUpload.dependsOn(GradleBuild)

        def projectAdapter = [
                bintrayUpload: bintrayUpload,
                projectsEvaluated: { Gradle gradle ->
                    bintrayUpload.with {
                        apiUrl = extension.apiUrl
                        user = extension.user
                        apiKey = extension.key
                        configurations = extension.configurations
                        publications = extension.publications
                        userOrg = extension.pkg.userOrg
                        repoName = extension.pkg.repo
                        packageName = extension.pkg.name ?: extension.user
                        packageDesc = extension.pkg.desc
                        packageLabels = extension.pkg.labels
                    }
                    if (extension.configurations?.length) {
                        Upload installTask = project.tasks.withType(Upload).findByName('install')
                        if (installTask) {
                            bintrayUpload.dependsOn(installTask)
                        }
                    }
                    if (extension.publications?.length) {
                        extension.publications.each {
                            def taskName = "publish${it[0].toUpperCase()}${it.substring(1)}PublicationToMavenLocal"
                            Task publishToLocalTask = project.tasks.findByName(taskName)
                            bintrayUpload.dependsOn(publishToLocalTask)
                        }
                    }
                }
        ] as BuildAdapter
        project.gradle.addBuildListener(projectAdapter)
    }
}