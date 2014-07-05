# Overview
This plugin allows you to publish artifacts to a repository on [bintray](https://bintray.com/).

[ ![Download](https://api.bintray.com/packages/jfrog/jfrog-jars/gradle-bintray-plugin/images/download.png) ](https://bintray.com/jfrog/jfrog-jars/gradle-bintray-plugin/_latestVersion)

# Usage
To use the plugin, configure your `build.gradle` script and add the plugin:
```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:0.5'
        }
    }
    apply plugin: 'com.jfrog.bintray'
```

**Gradle Compatibility:**
When using Gradle publications or when using `filesSpec` for direct file uploads, you'll need to use Gradle 2.x; Otherwise, the plugin is compatible with Gradle 1.12 and above.
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
        
        configurations = ['deployables'] //When uploading configuration files
        // - OR -
        publications = ['mavenStuff'] //When uploading Maven-based publication files
        // - AND/OR -
        filesSpec { //When uploading any arbitrary files ('filesSpec' is a standard Gradle CopySpec)
            from 'arbitrary-files'
            into 'standalone_files/level1'
            rename '(.+)\\.(.+)', '$1-suffix.$2'
        }
        dryRun = false //Whether to run this as dry-run, without deploying
        publish = true //If version should be auto published after an upload
        pkg {
            repo = 'myrepo'
            userOrg = 'myorg' //An optional organization name when the repo belongs to one of the user's orgs
            name = 'mypkg'
            desc = 'what a fantastic package indeed!'
            websiteUrl = 'https://github.com/bintray/gradle-bintray-plugin'
            issueTrackerUrl = 'https://github.com/bintray/gradle-bintray-plugin/issues'
            vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
            licenses = ['Apache-2.0']
            labels = ['gear', 'gore', 'gorilla']
            publicDownloadNumbers = true
            attributes= ['a': ['ay1', 'ay2'], 'b': ['bee'], c: 'cee'] //Optional package-level attributes
            //Optional version descriptor
            version {
                name = '1.3-Final' //Bintray logical version name
                desc = 'optional, version-specific description'
                vcsTag = '1.3.0'
                attributes = ['gradle-plugin': 'com.use.less:com.use.less.gradle:gradle-useless-plugin'] //Optional version-level attributes
            }
        }

    }
```
* As an example, you can also refer to this multi-module sample project [build file](https://github.com/bintray/bintray-examples/blob/master/gradle-multi-example/build.gradle).

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved JFrog
