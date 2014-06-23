# Overview
This plugin allows you to publish artifacts to a repository on [bintray](https://bintray.com/). 

# Usage
To use the plugin, configure your `build.gradle` script and add the plugin:
```groovy
    buildscript {
        repositories {
            maven { url 'http://jcenter.bintray.com' }
        }
        dependencies {
            classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:VERSION'
        }
    }
    apply plugin: 'com.jfrog.bintray'
```

# Tasks
The plugin adds the `bintrayUpload` task to your projects, which allows you to upload to bintray and optionally create the target package.
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
        }
        dryRun = false // whether to run this as dry-run, without deploying
        publish = true //If version should be auto published after an upload
    }
```

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved JFrog