package com.jfrog.bintray.gradle.tasks

import com.jfrog.bintray.gradle.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST

/**
 * @author Alexei Vainshtein
 * This task runs as final task after all other bintrayUpload tasks were executed.
 * This task performs the signing, publishing and maven central sync
 */
class BintrayPublishTask extends DefaultTask {
    static final String TASK_NAME = "bintrayPublish"

    @TaskAction
    void taskAction() throws IOException {
        signPublishAndSync()
    }

    private void signPublishAndSync() {
        HashSet<BintrayUploadTask> tasks = project.getTasksByName(BintrayUploadTask.TASK_NAME, true)
        for (BintrayUploadTask task : tasks) {
            if (task.getEnabled()) {
                if (task.getSignVersion()) {
                    gpgSignVersion(task.repoName, task.packageName, task.versionName, task)
                }
                if (task.publish) {
                    publishVersion(task.repoName, task.packageName, task.versionName, task)
                }
                if (task.shouldSyncToMavenCentral()) {
                    mavenCentralSync(task.repoName, task.packageName, task.versionName, task)
                }
            }
        }
    }

    private void gpgSignVersion(String repoName, String pkgName, String versionName, BintrayUploadTask task) {
        def pkgPath = "$task.subject/$repoName/$pkgName"
        if (task.dryRun) {
            logger.info("(Dry run) Signed version '$pkgPath/$versionName'.")
            return
        }
        task.getHttpBuilder().request(POST, JSON) {
            Utils.addHeaders(headers)
            uri.path = "/gpg/$pkgPath/versions/$versionName"
            if (task.gpgPassphrase) {
                body = [passphrase: task.gpgPassphrase]
            }
            response.success = { resp ->
                logger.info("Signed version '$versionName'.")
            }
            response.failure = { resp, reader ->
                throw new GradleException("Could not sign version '$versionName': $resp.statusLine $reader")
            }
        }
    }

    private void publishVersion(String repoName, String pkgName, String versionName, BintrayUploadTask task) {
        def pkgPath = "$task.subject/$repoName/$pkgName"
        def publishUri = "/content/$pkgPath/$versionName/publish"
        if (task.dryRun) {
            logger.info("(Dry run) Published version '$pkgPath/$versionName'.")
            return
        }
        task.getHttpBuilder().request(POST, JSON) {
            Utils.addHeaders(headers)
            uri.path = publishUri
            response.success = { resp ->
                logger.info("Published '$pkgPath/$versionName'.")
            }
            response.failure = { resp, reader ->
                throw new GradleException("Could not publish '$pkgPath/$versionName': $resp.statusLine $reader")
            }
        }
    }

    private void mavenCentralSync(String repoName, String pkgName, String versionName, BintrayUploadTask task) {
        def pkgPath = "$task.subject/$repoName/$pkgName"
        if (task.dryRun) {
            logger.info("(Dry run) Sync to Maven Central performed for '$pkgPath/$versionName'.")
            return
        }
        task.getHttpBuilder().request(POST, JSON) {
            Utils.addHeaders(headers)
            uri.path = "/maven_central_sync/$pkgPath/versions/$versionName"
            body = [username: task.ossUser, password: task.ossPassword]
            if (task.ossCloseRepo != null) {
                body << [close: task.ossCloseRepo]
            }
            response.success = { resp ->
                logger.info("Sync to Maven Central performed for '$pkgPath/$versionName'.")
            }
            response.failure = { resp, reader ->
                throw new GradleException("Could not sync '$pkgPath/$versionName' to Maven Central: $resp.statusLine $reader")
            }
        }
    }
}
