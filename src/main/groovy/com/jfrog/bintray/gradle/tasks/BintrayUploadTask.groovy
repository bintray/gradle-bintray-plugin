package com.jfrog.bintray.gradle.tasks

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayHttpClientFactory
import com.jfrog.bintray.gradle.BintrayPlugin
import com.jfrog.bintray.gradle.Utils
import com.jfrog.bintray.gradle.tasks.entities.Artifact
import com.jfrog.bintray.gradle.tasks.entities.Package
import com.jfrog.bintray.gradle.tasks.entities.Repository
import com.jfrog.bintray.gradle.tasks.entities.Version
import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
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
    static final String TASK_NAME = 'bintrayUpload'
    static final String GROUP = 'publishing'
    static final String DESCRIPTION = 'Publishes artifacts to bintray.com.'
    static final String API_URL_DEFAULT = 'https://api.bintray.com'
    public BintrayExtension extension
    public Project project

    private ConcurrentHashMap<String, Repository> repositories = new ConcurrentHashMap<>()
    private HTTPBuilder httpBuilder

    @Input
    @Optional
    String apiUrl

    @Input
    @Optional
    String user

    @Input
    @Optional
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
    boolean override

    @Input
    boolean dryRun

    @Input
    @Optional
    String userOrg

    @Input
    @Optional
    String repoName

    @Input
    @Optional
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
    String packageGithubRepo

    @Input
    @Optional
    String packageGithubReleaseNotesFile

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
    String debianDistribution

    @Input
    @Optional
    String debianComponent

    @Input
    @Optional
    String debianArchitecture

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
    {
        group = GROUP
        description = DESCRIPTION
    }

    @TaskAction
    void bintrayUpload() {
        logger.info("Gradle Bintray Plugin version: ${new Utils().pluginVersion}");
        if (getEnabled()) {
            validateDebianDefinition()
            //TODO: [by yl] replace with findResults for Gradle 2.x
            configurationUploads = configurations.collect {
                if (it instanceof CharSequence) {
                    Configuration configuration = project.configurations.findByName(it)
                    if (configuration != null) {
                        return collectArtifacts(configuration)
                    } else {
                        logger.error("{}: Could not find configuration: {}.", path, it)
                    }
                } else if (it instanceof Configuration) {
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
                } else if (it instanceof MavenPublication) {
                    return collectArtifacts((MavenPublication) it)
                } else {
                    logger.error("{}: Unsupported publication type: {}.", path, it.class)
                }
                []
            }.flatten() as Artifact[]

            RecordingCopyTask recordingCopyTask = getDependsOn().find { it instanceof RecordingCopyTask }
            fileUploads = (recordingCopyTask ? recordingCopyTask.fileUploads : []) as Artifact[]

            if (repoName == null || versionName == null || packageName == null) {
                logger.warn("Repository name, package name or version name are null for project: " + project.getDisplayName())
                return
            }
            checkAndCreatePackage()
            checkAndCreateVersion()
            configurationUploads.each {
                uploadArtifact(it)
            }
            publicationUploads.each {
                uploadArtifact(it)
            }
            fileUploads.each {
                uploadArtifact(it)
            }
        } else {
            logger.debug("Gradle Bintray plugin is disabled for task: " + project.getName())
        }
    }

    void checkAndCreatePackage() {
        // Check if the package has already been created by another BintrayUploadTask.
        Package pkg = checkPackageAlreadyCreated()
        if (pkg && pkg.isCreated()) {
            return
        }
        def create
        getHttpBuilder().request(HEAD) {
            Utils.addHeaders(headers)
            uri.path = "/packages/$packagePath"
            response.success = { resp ->
                logger.debug("Package '$packageName' exists.")
            }
            response.'404' = { resp ->
                logger.info("Package '$packageName' does not exist. Attempting to create it...")
                create = true
            }
        }
        if (create) {
            if (dryRun) {
                logger.info("(Dry run) Created package '$packagePath'.")
            } else {
                getHttpBuilder().request(POST, JSON) {
                    Utils.addHeaders(headers)
                    uri.path = "/packages/$subject/$repoName"
                    body = [name                     : packageName, desc: packageDesc,
                            licenses                 : packageLicenses,
                            labels                   : packageLabels,
                            website_url              : packageWebsiteUrl,
                            issue_tracker_url        : packageIssueTrackerUrl,
                            vcs_url                  : packageVcsUrl,
                            public_download_numbers  : packagePublicDownloadNumbers,
                            github_repo              : packageGithubRepo,
                            github_release_notes_file: packageGithubReleaseNotesFile]

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
        }
        setPackageAsCreated(pkg)
    }

    void checkAndCreateVersion() {
        // Check if the version has already been created by another BintrayUploadTask.
        if (versionName == null) {
            return
        }
        Version version = checkVersionAlreadyCreated()
        if (version && version.isCreated()) {
            return
        }
        def create
        getHttpBuilder().request(HEAD) {
            Utils.addHeaders(headers)
            uri.path = "/packages/$packagePath/versions/$versionName"
            response.success = { resp ->
                logger.debug("Version '$packagePath/$versionName' exists.")
            }
            response.'404' = { resp ->
                logger.info("Version '$packagePath/$versionName' does not exist. Attempting to create it...")
                create = true
            }
        }
        if (create) {
            if (dryRun) {
                logger.info("(Dry run) Created version '$packagePath/$versionName'.")
            } else {
                getHttpBuilder().request(POST, JSON) {
                    Utils.addHeaders(headers)
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
        }
        setVersionAsCreated(version)
    }

    void validateDebianDefinition() {
        if ((debianDistribution && !debianArchitecture) ||
                (!debianDistribution && debianArchitecture)) {
            throw new GradleException("Both 'distribution' and 'architecture' are mandatory in the gradle.debian closure.")
        }
    }

    void setAttributes(String attributesPath, Map attributes, String entity, String entityName) {
        getHttpBuilder().request(POST, JSON) {
            Utils.addHeaders(headers)
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

    void addUploadHeaders(Map<?, ?> headers) {
        Utils.addHeaders(headers)
        if (debianDistribution) {
            String component = debianComponent ? debianComponent : "main"
            headers.put("X-Bintray-Debian-Distribution", debianDistribution)
            headers.put("X-Bintray-Debian-Component", component)
            headers.put("X-Bintray-Debian-Architecture", debianArchitecture)
        }
    }

    Package checkPackageAlreadyCreated() {
        Package pkg = new Package(packageName)
        Package p = getRepository().packages.putIfAbsent(packageName, pkg)
        if (p && !p.created) {
            synchronized (p) {
                if (!p.created) {
                    p.wait()
                }
            }
        }
        return p ? p : pkg
    }

    Version checkVersionAlreadyCreated() {
        Package pkg = getRepository().packages.get(packageName)
        if (!pkg) {
            throw new IllegalStateException(
                    "Attempted checking and creating version, before checking and creating the package.")
        }
        Version v = new Version(
                versionName, signVersion, gpgPassphrase, publish, shouldSyncToMavenCentral())
        Version version = pkg.addVersionIfAbsent(v)
        if (version && !version.created) {
            synchronized (version) {
                if (!version.created) {
                    version.wait()
                }
            }
        }
        return version ? version : v
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

    void uploadArtifact(Artifact artifact) {
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
            logger.warn("Uploading to $apiUrl$uploadUri...")
            if (dryRun) {
                logger.info("(Dry run) Uploaded to '$apiUrl$uploadUri'.")
                return
            }
            getHttpBuilder().request(PUT) {
                if (override) {
                    uri.query = [override: "1"]
                }
                addUploadHeaders(headers)
                // Set the requestContentType to BINARY, so that HTTPBuilder can encode the uploaded file:
                requestContentType = BINARY
                // Set the Content-Type to '*/*' to enable Bintray to set it on its own:
                headers["Content-Type"] = '*/*'
                uri.path = uploadUri
                body = is
                response.success = { resp ->
                    logger.warn("Uploaded to '$apiUrl$uri.path'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException("Could not upload to '$apiUrl$uri.path': $resp.statusLine $reader")
                }
            }
        }
    }

    boolean shouldSyncToMavenCentral() {
        syncToMavenCentral && ossUser != null && ossPassword != null
    }

    Artifact[] collectArtifacts(Configuration config) {
        // First lets check if there is a pom inside.
        String artifactId
        ArrayList<Artifact> artifacts = new ArrayList<>()
        boolean pomArtifact = config.allArtifacts.findResults {
            if (!it.file.exists()) {
                logger.error("{}: file {} could not be found.", path, it.file.getAbsolutePath())
                return null
            }
            if (it.type == 'pom') {
                return true
            }
        }
        if (!pomArtifact) {
            // Add pom file per config
            Upload installTask = project.tasks.withType(Upload).findByName('install');
            if (!installTask) {
                logger.info "maven plugin was not applied, no pom will be uploaded."
            } else {
                File pom = new File(getProject().convention.plugins['maven'].mavenPomDir, "pom-default.xml")
                if (pom.exists()) {
                    artifactId = Utils.readArtifactIdFromPom(pom)
                    artifacts << new Artifact(
                            name: artifactId,
                            groupId: project.group,
                            version: project.version,
                            extension: 'pom',
                            type: 'pom',
                            file: pom
                    )
                } else {
                    logger.debug("Pom file " + pom.getAbsolutePath() + " doesn't exists.")
                }
            }
        }
        config.allArtifacts.findResults {
            if (!it.file.exists()) {
                logger.error("{}: file {} could not be found.", path, it.file.getAbsolutePath())
                return null
            }
            boolean signedArtifact = (it instanceof org.gradle.plugins.signing.Signature)
            def signedExtension = signedArtifact ? it.toSignArtifact.getExtension() : null
            String name = artifactId == null ? it.name : artifactId
            artifacts << new Artifact(
                    name: name, groupId: project.group, version: project.version, extension: it.extension,
                    type: it.type, classifier: it.classifier, file: it.file, signedExtension: signedExtension
            )
        }.unique()

        artifacts
    }


    HTTPBuilder getHttpBuilder() {
        if (httpBuilder == null) {
            httpBuilder = BintrayHttpClientFactory.create(apiUrl, user, apiKey)
        }
        return httpBuilder
    }

    String getSubject() {
        "${userOrg ?: user}"
    }

    String getPackagePath() {
        "$subject/$repoName/$packageName"
    }

    Artifact[] collectArtifacts(Publication publication) {
        if (!publication instanceof MavenPublication) {
            logger.info "{} can only use maven publications - skipping {}.", path, publication.name
            return []
        }
        def artifacts = publication.artifacts.findResults {
            boolean signedArtifact = (it instanceof org.gradle.plugins.signing.Signature)
            def signedExtension = signedArtifact ? it.toSignArtifact.getExtension() : null
            new Artifact(
                    name: publication.artifactId,
                    groupId: publication.groupId,
                    version: publication.version,
                    extension: it.extension,
                    type: it.extension,
                    classifier: it.classifier,
                    file: it.file,
                    signedExtension: signedExtension
            )
        }

        // Add the pom file
        artifacts << new Artifact(
                name: publication.artifactId,
                groupId: publication.groupId,
                version: publication.version,
                extension: 'pom',
                type: 'pom',
                file: publication.asNormalisedPublication().pomFile
        )
        artifacts
    }

    public ConcurrentHashMap<String, Repository> getCachedRepositories() {
        BintrayUploadTask t = ((BintrayUploadTask) project.rootProject.tasks.findByName(TASK_NAME))
        if (t == null) {
            project.rootProject.getPluginManager().apply(BintrayPlugin.class)
            t = ((BintrayUploadTask) project.rootProject.tasks.findByName(TASK_NAME))
        }
        if (t == null) {
            throw new RuntimeException("Could not find $TASK_NAME task in root project")
        }
        t.getRepositories()
    }

    public ConcurrentHashMap<String, Repository> getRepositories() {
        if (this.project != this.project.rootProject) {
            throw new IllegalStateException("The getRepositories method can be invoked on the root project$TASK_NAME task only")
        }
        return repositories
    }

    private Repository getRepository() {
        Repository repository = new Repository(repoName)
        Repository r = getCachedRepositories().putIfAbsent(repoName, repository)
        if (!r) {
            return repository
        }
        return r
    }
}
