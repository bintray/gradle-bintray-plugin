package com.jfrog.bintray.gradle

import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import org.gradle.kotlin.dsl.closureOf

/**
 * Uses the supplied [block] to configure the [BintrayExtension.PackageConfig] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension] The bintray plugin instance for the project
 * @param block A lambda which receives the [BintrayExtension.PackageConfig] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    repo = ""
 *    ...
 *  }
 * }
 *
 */
fun BintrayExtension.pkg(block: BintrayExtension.PackageConfig.() -> Unit): Any = pkg(closureOf(block))

/**
 * Uses the supplied [block] to configure the [BintrayExtension.VersionConfig] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension.PackageConfig] The bintray version config instance for the project
 * @param block A lambda which receives the [BintrayExtension.VersionConfig] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    version {
 *      name = ""
 *      ...
 *    }
 *    ...
 *  }
 * }
 *
 */
fun BintrayExtension.PackageConfig.version(block: BintrayExtension.VersionConfig.() -> Unit): Any = version(closureOf(block))


/**
 * Uses the supplied [block] to configure the [RecordingCopyTask] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension] The bintray plugin instance for the project
 * @param block A lambda which receives the [RecordingCopyTask] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    fileSpec {
 *    from("arbitrary-files")
 *    into("standalone_files/level1")
 *    rename("(.+)\\.(.+)", "$1-suffix.$2")
 *   }
 *  }
 * }
 *
 */
fun BintrayExtension.fileSpec(block: RecordingCopyTask.() -> Unit): Any = filesSpec(closureOf(block))

/**
 * Uses the supplied [block] to configure the [BintrayExtension.DebianConfig] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension.PackageConfig] The bintray version config instance for the project
 * @param block A lambda which receives the [BintrayExtension.DebianConfig] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    debian {
 *      distribution = 'squeeze'
 *      component = 'main'
 *      architecture = 'i386,noarch,amd64'
 *    }
 *    ...
 *  }
 * }
 *
 */
fun BintrayExtension.PackageConfig.debian(block: BintrayExtension.DebianConfig.() -> Unit): Any = debian(closureOf(block))

/**
 * Uses the supplied [block] to configure the [BintrayExtension.GpgConfig] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension.VersionConfig] The bintray version config instance for the project
 * @param block A lambda which receives the [BintrayExtension.GpgConfig] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    gpg {
 *     sign = true //Determines whether to GPG sign the files. The default is false
 *     passphrase = 'passphrase' //Optional. The passphrase for GPG signing'
 *    }
 *    ...
 *  }
 * }
 *
 */
fun BintrayExtension.VersionConfig.gpg(block: BintrayExtension.GpgConfig.() -> Unit): Any = gpg(closureOf(block))

/**
 * Uses the supplied [block] to configure the [BintrayExtension.MavenCentralSyncConfig] for bintray.
 * @since 1.8.6
 * @receiver [BintrayExtension.VersionConfig] The bintray version config instance for the project
 * @param block A lambda which receives the [BintrayExtension.MavenCentralSyncConfig] used for configuration
 * @usage
 * ```
 * bintray {
 *  pkg {
 *    gpg {
 *      mavenCentralSync {
 *      sync = true //[Default: true] Determines whether to sync the version to Maven Central.
 *      user = 'userToken' //OSS user token: mandatory
 *      password = 'paasword' //OSS user password: mandatory
 *      close = '1' //Optional property. By default the staging repository is closed and artifacts are released to Maven Central. You can optionally turn this behaviour *     off (by puting 0 as value) and release the version manually.
 *    }
 *    ...
 *  }
 * }
 *
 */
fun BintrayExtension.VersionConfig.mavenCentralSync(block: BintrayExtension.MavenCentralSyncConfig.() -> Unit): Any = mavenCentralSync(closureOf(block))

/**
 * Sets the [configurations] of the current bintray instance.
 * @param configurations the value configurations should be set to
 * @see configurations
 */
fun BintrayExtension.configurations(vararg configurations: String) {
    this.configurations = configurations
}

/**
 * Sets the [publications] of the current bintray instance.
 * @param publications the value publications should be set to
 * @see publications
 */
fun BintrayExtension.publications(vararg publications: String) {
    this.publications = publications
}

/**
 * Sets the [licenses] of the current package config instance.
 * @param licenses the value licenses should be set to
 * @see licenses
 */
fun BintrayExtension.PackageConfig.licenses(vararg licenses: String) {
    this.licenses = licenses
}

/**
 * Sets the [labels] of the current package config instance.
 * @param labels the value labels should be set to
 * @see labels
 */
fun BintrayExtension.PackageConfig.labels(vararg labels: String) {
    this.labels = labels
}

/**
 * Sets the [attributes] of the current package config instance.
 * @param attributes the value attributes should be set to
 * @see attributes
 */
fun BintrayExtension.PackageConfig.attributes(vararg attributes: Pair<Any, Any>) {
    this.attributes = mapOf(*attributes)
}

/**
 * Sets the [attributes] of the current version config instance.
 * @param attributes the value attributes should be set to
 * @see attributes
 */
fun BintrayExtension.VersionConfig.attributes(vararg attributes: Pair<Any, Any>) {
    this.attributes = mapOf(*attributes)
}
