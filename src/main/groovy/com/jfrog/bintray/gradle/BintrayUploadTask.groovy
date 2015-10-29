package com.jfrog.bintray.gradle

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Upload

import java.util.concurrent.ConcurrentHashMap

import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

class BintrayUploadTask extends DefaultTask {

    static final String NAME = 'bintrayUpload'
    static final String GROUP = 'publishing'
    static final String DESCRIPTION = 'Publishes artifacts to bintray.com.'
    static final String API_URL_DEFAULT = 'https://api.bintray.com'

    List<BintrayUploadTask> bintrayUploadTasks = null
    private static ConcurrentHashMap<String, Package> packagesCreated = new ConcurrentHashMap<>()

    @Input
    @Optional
    String apiUrl

    @Input
    String user

    @Input
    String apiKey

    @Input
    @Optional
    Object[] configurations

    @Input
    @Optional
    Object[] publications

    @Input
    @Optional
    CopySpec filesSpec

    @Input
    boolean publish

    @Input
    boolean dryRun

    @Input
    @Optional
    String userOrg

    @Input
    String repoName

    @Input
    String packageName

    @Input
    @Optional
    String packageDesc

    @Input
    @Optional
    String packageWebsiteUrl

    @Input
    @Optional
    String packageIssueTrackerUrl

    @Input
    @Optional
    String packageVcsUrl

    @Input
    @Optional
    String[] packageLicenses

    @Input
    @Optional
    String[] packageLabels

    @Input
    @Optional
    Map packageAttributes

    @Input
    @Optional
    boolean packagePublicDownloadNumbers

    @Input
    @Optional
    String versionName

    @Input
    @Optional
    String versionDesc

    @Input
    @Optional
    String versionReleased

    @Input
    @Optional
    boolean signVersion

    @Input
    @Optional
    String gpgPassphrase

    @Input
    @Optional
    String versionVcsTag

    @Input
    @Optional
    Map versionAttributes

    @Input
    @Optional
    boolean syncToMavenCentral

    @Input
    @Optional
    String ossUser

    @Input
    @Optional
    String ossPassword

    @Input
    @Optional
    String ossCloseRepo

    Artifact[] configurationUploads
    Artifact[] publicationUploads
    Artifact[] fileUploads

    boolean subtaskSkipPublish

    {
        group = GROUP
        description = DESCRIPTION
    }

    @TaskAction
    void bintrayUpload() {
        logger.info("Found {} configuration(s) to publish.", configurations?.length ?: 0);
        //TODO: [by yl] replace with findResults for Gradle 2.x
        configurationUploads = configurations.collect {
            if (it instanceof CharSequence) {
                Configuration configuration = project.configurations.findByName(it)
                if (configuration != null) {
                    return collectArtifacts(configuration)
                } else {
                    logger.error("{}: Could not find configuration: {}.", path, it)
                }
            } else if (conf instanceof Configuration) {
                return collectArtifacts((Configuration) it)
            } else {
                logger.error("{}: Unsupported configuration type: {}.", path, it.class)
            }
            []
        }.flatten() as Artifact[]

        publicationUploads = publications.collect {
            if (it instanceof CharSequence) {
                Publication publication = project.extensions.getByType(PublishingExtension).publications.findByName(it)
                if (publication != null) {
                    return collectArtifacts(publication)
                } else {
                    logger.error("{}: Could not find publication: {}.", path, it);
                }
            } else if (conf instanceof MavenPublication) {
                return collectArtifacts((Configuration) it)
            } else {
                logger.error("{}: Unsupported publication type: {}.", path, it.class)
            }
            []
        }.flatten() as Artifact[]

        RecordingCopyTask recordingCopyTask = getDependsOn().find { it instanceof RecordingCopyTask }
        fileUploads = (recordingCopyTask ? recordingCopyTask.fileUploads : []) as Artifact[]

        //Upload the files
        HTTPBuilder http = BintrayHttpClientFactory.create(apiUrl, user, apiKey)
        def repoPath = "${userOrg ?: user}/$repoName"
        def packagePath = "$repoPath/$packageName"

        def setAttributes = { attributesPath, attributes, entity, entityName ->
            http.request(POST, JSON) {
                uri.path = attributesPath
                def builder = new JsonBuilder()
                builder.content = attributes.collect {
                    //Support both arrays and singular values - coerce to an array of values
                    ['name': it.key, 'values': [it.value].flatten()]
                }
                body = builder.toString()
                response.success = { resp ->
                    logger.info("Attributes set on $entity '$entityName'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException(
                            "Could not set attributes on $entity '$entityName': $resp.statusLine $reader")
                }
            }
        }

        def checkAndCreatePackage = {
            // Check if the package has already been created by another BintrayUploadTask.
            Package pkg = checkPackageAlreadyCreated()
            if (!pkg) {
                return
            }
            def create
            http.request(HEAD) {
                uri.path = "/packages/$packagePath"
                response.success = { resp ->
                    logger.debug("Package '$packageName' exists.")
                }
                response.'404' = { resp ->
                    logger.info("Package '$packageName' does not exist. Attempting to creating it...")
                    create = true
                }
            }
            if (create) {
                if (dryRun) {
                    logger.info("(Dry run) Created pakage '$packagePath'.")
                    return
                }
                http.request(POST, JSON) {
                    uri.path = "/packages/$repoPath"
                    body = [name                   : packageName, desc: packageDesc, licenses: packageLicenses, labels: packageLabels,
                            website_url            : packageWebsiteUrl, issue_tracker_url: packageIssueTrackerUrl, vcs_url: packageVcsUrl,
                            public_download_numbers: packagePublicDownloadNumbers]

                    response.success = { resp ->
                        logger.info("Created package '$packagePath'.")
                    }
                    response.failure = { resp, reader ->
                        throw new GradleException("Could not create package '$packagePath': $resp.statusLine $reader")
                    }
                }
                if (packageAttributes) {
                    setAttributes "/packages/$packagePath/attributes", packageAttributes, 'package', packageName
                }
            }
            setPackageAsCreated(pkg)
        }

        def checkAndCreateVersion = {
            // Check if the version has already been created by another BintrayUploadTask.
            Version version = checkVersionAlreadyCreated()
            if (!version) {
                return;
            }
            def create
            http.request(HEAD) {
                uri.path = "/packages/$packagePath/versions/$versionName"
                response.success = { resp ->
                    logger.debug("Version '$packagePath/$versionName' exists.")
                }
                response.'404' = { resp ->
                    logger.info("Version '$packagePath/$versionName' does not exist. Attempting to creating it...")
                    create = true
                }
            }
            if (create) {
                if (dryRun) {
                    logger.info("(Dry run) Created version '$packagePath/$versionName'.")
                    return
                }
                http.request(POST, JSON) {
                    uri.path = "/packages/$packagePath/versions"
                    versionReleased = Utils.toIsoDateFormat(versionReleased)
                    body = [name: versionName, desc: versionDesc, released: versionReleased, vcs_tag: versionVcsTag]
                    response.success = { resp ->
                        logger.info("Created version '$versionName'.")
                    }
                    response.failure = { resp, reader ->
                        throw new GradleException("Could not create version '$versionName': $resp.statusLine $reader")
                    }
                }
                if (versionAttributes) {
                    setAttributes "/packages/$packagePath/versions/$versionName/attributes", versionAttributes,
                            'version', versionName
                }
            }
            setVersionAsCreated(version)
        }

        def gpgSignVersion = { pkg, version ->
            def pkgPath = "$repoPath/$pkg.name"
            def versionName = version.name
            if (dryRun) {
                logger.info("(Dry run) Signed version '$pkgPath/$versionName'.")
                return
            }
            http.request(POST, JSON) {
                uri.path = "/gpg/$pkgPath/versions/$versionName"
                if (version.gpgPassphrase) {
                    body = [passphrase: gpgPassphrase]
                }
                response.success = { resp ->
                    logger.info("Signed version '$versionName'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException("Could not sign version '$versionName': $resp.statusLine $reader")
                }
            }
        }

        def uploadArtifact = { artifact ->
            def versionPath = packagePath + '/' + versionName ?: artifact.version
            def uploadUri = "/content/$versionPath/${artifact.path}"
            if (!artifact.file.exists()) {
                logger.error("Skipping upload for missing file '$artifact.file'.")
                return
            }
            artifact.file.withInputStream { is ->
                is.metaClass.totalBytes = {
                    artifact.file.length()
                }
                logger.info("Uploading to $apiUrl$uploadUri...")
                if (dryRun) {
                    logger.info("(Dry run) Uploaded to '$apiUrl$uploadUri'.")
                    return
                }
                http.request(PUT) {
                    uri.path = uploadUri
                    requestContentType = BINARY
                    body = is
                    response.success = { resp ->
                        logger.info("Uploaded to '$apiUrl$uri.path'.")
                    }
                    response.failure = { resp, reader ->
                        throw new GradleException("Could not upload to '$apiUrl$uri.path': $resp.statusLine $reader")
                    }
                }
            }
        }

        def publishVersion = { pkg, version ->
            def pkgPath = "$repoPath/$pkg.name"
            def versionName = version.name
            def publishUri = "/content/$pkgPath/$versionName/publish"
            if (dryRun) {
                logger.info("(Dry run) Pulished version '$pkgPath/$versionName'.")
                return
            }
            http.request(POST, JSON) {
                uri.path = publishUri
                response.success = { resp ->
                    logger.info("Published '$pkgPath/$versionName'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException("Could not publish '$pkgPath/$versionName': $resp.statusLine $reader")
                }
            }
        }

        def mavenCentralSync = { pkg, version ->
            def pkgPath = "$repoPath/$pkg.name"
            def versionName = version.name
            if (dryRun) {
                logger.info("(Dry run) Sync to Maven Central performed '$pkgPath/$versionName'.")
                return
            }
            http.request(POST, JSON) {
                uri.path = "/maven_central_sync/$pkgPath/versions/$versionName"
                body = [username: ossUser, password: ossPassword]
                if (ossCloseRepo != null) {
                    body << [close: ossCloseRepo]
                }
                response.success = { resp ->
                    logger.info("Sync to Maven Central performed for '$pkgPath/$versionName'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException("Could not sync '$pkgPath/$versionName' to Maven Central: $resp.statusLine $reader")
                }
            }
        }

        def signPublishAndSync = {
            Collection<Package> packages = BintrayUploadTask.packagesCreated.values()
            for (Package p : packages) {
                Collection<Version> versions = p.versions.values()
                for (Version v : versions) {
                    if (v.gpgSign) {
                        gpgSignVersion(p, v)
                    }
                    if (v.publish) {
                        publishVersion(p, v)
                    }
                    if (v.mavenCentralSync) {
                        mavenCentralSync(p, v)
                    }
                }
            }
        }

        checkAndCreatePackage()
        checkAndCreateVersion()

        configurationUploads.each {
            uploadArtifact it
        }
        publicationUploads.each {
            uploadArtifact it
        }
        fileUploads.each {
            uploadArtifact it
        }
        if (lastTask) {
            signPublishAndSync()
        }
    }

    Package checkPackageAlreadyCreated() {
        Package pkg = new Package(packageName)
        Package p = BintrayUploadTask.packagesCreated.putIfAbsent(packageName, pkg)
        if (p) {
            if (!p.created) {
                synchronized (p) {
                    if (!p.created) {
                        p.wait()
                    }
                }
            }
            return null
        }
        return pkg
    }

    Version checkVersionAlreadyCreated() {
        Package pkg = BintrayUploadTask.packagesCreated.get(packageName)
        if (!pkg) {
            throw new IllegalStateException(
                "Attempted checking and creating version, before checking and creating the package.")
        }
        boolean shouldPublish = publish && !subtaskSkipPublish
        Version version = new Version(
            versionName, signVersion, gpgPassphrase, shouldPublish, shouldSyncToMavenCentral())
        Version v = pkg.addVersionIfAbsent(version)
        if (v) {
            if (!v.created) {
                synchronized (v) {
                    if (!v.created) {
                        v.wait()
                    }
                }
            }
            return null
        }
        return version
    }

    void setPackageAsCreated(Package pkg) {
        pkg.setAsCreated()
        synchronized (pkg) {
            pkg.notifyAll()
        }
    }

    void setVersionAsCreated(Version version) {
        version.setAsCreated()
        synchronized (version) {
            version.notifyAll()
        }
    }

    /**
     * Indicates whether this BintrayUploadTask is the last task to be excuted.
     * @return  true if this is the last BintrayUploadTask task.
     */
    boolean isLastTask() {
        currentTaskIndex == allBintrayUploadTasks.size() - 1
    }

    /**
     * Return the index of this BintrayUploadTask in the list of all tasks of type BintrayUploadTask.
     * @return  The task index.
     */
    int getCurrentTaskIndex() {
        List<BintrayUploadTask> tasks = allBintrayUploadTasks
        int currentTaskIndex = tasks.indexOf(this);
        if (currentTaskIndex == -1) {
            throw new Exception("Could not find the current task {} in the task graph", getPath());
        }
        currentTaskIndex
    }

    List<BintrayUploadTask> getAllBintrayUploadTasks() {
        if (!bintrayUploadTasks) {
            List<BintrayUploadTask> tasks = new ArrayList<BintrayUploadTask>()
            for (Task task : getProject().getGradle().getTaskGraph().getAllTasks()) {
                if (task instanceof BintrayUploadTask) {
                    tasks.add(task);
                }
            }
            bintrayUploadTasks = tasks
        }
        bintrayUploadTasks
    }

    boolean shouldSyncToMavenCentral() {
        syncToMavenCentral && ossUser != null && ossPassword != null
    }

    Artifact[] collectArtifacts(Configuration config) {
        boolean pomArtifact
        def artifacts = config.allArtifacts.findResults {
            if (!it.file.exists()) {
                logger.error("{}: file {} could not be found.", path, it.file.getAbsolutePath())
                return null
            }
            pomArtifact = !pomArtifact && it.type == 'pom'
            boolean signedArtifact = (it instanceof org.gradle.plugins.signing.Signature)
            def signedExtenstion = signedArtifact ? it.toSignArtifact.getExtension() : null
            new Artifact(
                    name: it.name, groupId: project.group, version: project.version, extension: it.extension,
                    type: it.type, classifier: it.classifier, file: it.file, signedExtenstion: signedExtenstion
            )
        }.unique();

        //Add pom file per config
        Upload installTask = project.tasks.withType(Upload).findByName('install');
        if (!installTask) {
            logger.info "maven plugin was not applied, no pom will be uploaded."
        } else if (!pomArtifact) {
            artifacts << new Artifact(name: project.name, groupId: project.group, version: project.version,
                    extension: 'pom', type: 'pom',
                    file: new File(getProject().convention.plugins['maven'].mavenPomDir, "pom-default.xml"))
        }
        artifacts
    }

    Artifact[] collectArtifacts(Publication publication) {
        if (!publication instanceof MavenPublication) {
            logger.info "{} can only use maven publications - skipping {}.", path, publication.name
            return []
        }
        def identity = publication.mavenProjectIdentity
        def artifacts = publication.artifacts.findResults {
            boolean signedArtifact = (it instanceof org.gradle.plugins.signing.Signature)
            def signedExtenstion = signedArtifact ? it.toSignArtifact.getExtension() : null
            new Artifact(
                    name: identity.artifactId, groupId: identity.groupId, version: identity.version,
                    extension: it.extension, type: it.extension, classifier: it.classifier, file: it.file,
                    signedExtenstion: signedExtenstion
            )
        }

        //Add the pom
        artifacts << new Artifact(
                name: identity.artifactId, groupId: identity.groupId, version: identity.version,
                extension: 'pom', type: 'pom', file: publication.asNormalisedPublication().pomFile)
        artifacts
    }

    class Package {
        private String name
        private boolean created
        private ConcurrentHashMap<String, Version> versions = new ConcurrentHashMap<String, Version>()

        Package(String name) {
            this.name = name
        }

        boolean isCreated() {
            return created
        }

        void setAsCreated() {
            this.created = true
        }

        public Version addVersionIfAbsent(Version version) {
            Version v = versions.putIfAbsent(version.name, version)
            if (v) {
                v.merge(version)
            }
            return v
        }
    }

    class Version {
        private String name
        private boolean created
        private boolean gpgSign
        private String gpgPassphrase
        private boolean publish
        private boolean mavenCentralSync

        Version(String name, boolean gpgSign, String gpgPassphrase, boolean publish, boolean mavenCentralSync) {
            this.name = name
            this.gpgSign = gpgSign
            this.gpgPassphrase = gpgPassphrase
            this.publish = publish
            this.mavenCentralSync = mavenCentralSync
        }

        boolean isCreated() {
            return created
        }

        void setAsCreated() {
            this.created = true
        }

        boolean isGpgSign() {
            return gpgSign
        }

        String getGpgPassphrase() {
            return gpgPassphrase
        }

        boolean isPublish() {
            return publish
        }

        boolean isMavenCentralSync() {
            return mavenCentralSync
        }

        void merge(Version version) {
            if (version) {
                this.gpgSign = this.gpgSign || version.gpgSign
                this.publish = this.publish || version.publish
                this.mavenCentralSync = this.mavenCentralSync || version.mavenCentralSync
                this.gpgPassphrase = gpgPassphrase ?: this.gpgPassphrase
            }
        }
    }
}
