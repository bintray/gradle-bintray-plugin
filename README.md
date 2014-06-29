# Overview
This plugin allows you to publish artifacts to a repository on [bintray](https://bintray.com/). 

# Usage
To use the plugin, configure your `build.gradle` script and add the plugin:
```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:VERSION'
        }
    }
    apply plugin: 'com.jfrog.bintray'
```

**Gradle Compatibility:**
When using Gradle publications you'll need to use Gradle 2.x; Otherwise, the plugin is compatible with Gradle 1.12 and above.
 **JVM Compatibility:**
Java 6 and above.

# Tasks
The plugin adds the `bintrayUpload` task to your projects, which allows you to upload to bintray and optionally create
the target package and version.
Artifacts can be uploaded from the specified configurations or (the newly supported) publications.

## Configuration

### build.gradle
```groovy
    bintray {
        user = 'bintray_user'
        key = 'bintray_api_key'
        
        configurations = ['deployables'] // When uploading configuration files
        // - OR -
        publications = ['mavenStuff'] // When uploading Maven-based publication files

        dryRun = false // whether to run this as dry-run, without deploying
        publish = true //If version should be auto published after an upload
        pkg {
            repo = 'myrepo'
            userOrg = 'myorg' // an optional organization name when the repo belongs to one of the user's orgs
            name = 'mypkg'
            desc = 'what a fantastic package indeed!'
            websiteUrl = 'https://github.com/bintray/gradle-bintray-plugin'
            issueTrackerUrl = 'https://github.com/bintray/gradle-bintray-plugin/issues'
            vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
            licenses = ['Apache-2.0']
            labels = ['gear', 'gore', 'gorilla']
            publicDownloadNumbers = true

            // optional version attributes
            version {
                name = '1.3-Final' // bintray logical version name
                desc = 'optional, version-specific description'
                vcsTag = '1.3.0'
            }
        }

    }
```

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved JFrog
