package com.jfrog.bintray.gradle

import com.jfrog.bintray.gradle.tasks.BintrayPublishTask
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.*

class BintrayPlugin implements Plugin<Project> {

    public void apply(Project project) {
        if (project.getTasks().findByName(BintrayUploadTask.TASK_NAME) == null) {
            //Create and configure the task
            BintrayUploadTask bintrayUpload = project.task(type: BintrayUploadTask, BintrayUploadTask.TASK_NAME)
            bintrayUpload.project = project
            project.gradle.addListener(new ProjectsEvaluatedBuildListener(bintrayUpload))
        }

        addBintrayUploadTask(project)
        if (isRootProject(project)) {
            addBintrayPublishTask(project)
        } else {
            if (!project.getRootProject().getPluginManager().hasPlugin("com.jfrog.bintray")) {
                // Reached this state means that the plugin is not enabled on the root project
                BintrayUploadTask bintrayUploadRoot = project.getRootProject().task(type: BintrayUploadTask, BintrayUploadTask.TASK_NAME)
                bintrayUploadRoot.setEnabled(false)
                bintrayUploadRoot.project = project.getRootProject()
                project.getRootProject().gradle.addListener(new ProjectsEvaluatedBuildListener(bintrayUploadRoot))
                project.getRootProject().getPluginManager().apply(BintrayPlugin.class)
            }
        }
    }

    private BintrayUploadTask addBintrayUploadTask(Project project) {
        BintrayUploadTask bintrayUploadTask = project.tasks.findByName(BintrayUploadTask.TASK_NAME)
        if (bintrayUploadTask == null) {
            bintrayUploadTask = createBintrayUploadTask(project)
            bintrayUploadTask.setGroup(BintrayUploadTask.GROUP)
        }
        return bintrayUploadTask
    }

    BintrayUploadTask createBintrayUploadTask(Project project) {
        def result = project.getTasks().create(BintrayUploadTask.TASK_NAME, BintrayUploadTask.class)
        result.setDescription('''AddS bintray closure''')
        return result
    }

    private static boolean isRootProject(Project project) {
        project.equals(project.getRootProject())
    }

    private BintrayPublishTask addBintrayPublishTask(Project project) {
        Task signAndPublish = project.tasks.findByName(BintrayPublishTask.TASK_NAME)
        if (signAndPublish == null) {
            project.getLogger().info("Configuring signAndPublish task for project ${project.path}")
            signAndPublish = createBintraySignAndPublishTask(project)
            signAndPublish.setGroup(BintrayUploadTask.GROUP)
        }
    }

    protected BintrayPublishTask createBintraySignAndPublishTask(Project project) {
        def result = project.getTasks().create(BintrayPublishTask.TASK_NAME, BintrayPublishTask.class)
        result.setDescription('''Bintray Sign, publish and Maven central sync task''')
        return result
    }
}
