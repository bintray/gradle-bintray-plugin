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
    apply plugin: 'bintray'
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
        publications = ['mavenStuff'] // When uploading Maven-based publication files
        pkg {
            repo = 'myrepo'
            userOrg = 'myorg' // an optional organization name when the repo belongs to one of the user's orgs
            name = 'mypkg'
            desc = 'a fantastic package, indeed!'
            licenses = ['Apache-2.0']
            labels = ['gear', 'gore', 'gorilla']
        }
        dryRun = dry // whether to run this as dry-run, without deploying
    }
```



You can use the `-P` command line option to pass user and key as command line argument:

`gradle -Puser=someuser -Pkey=ASDFASDFASDF bintrayUpload`

then you need to use those properties in your config:

```groovy
bintray {
    user = property('user')
    key = property('key')
}
```


# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved JFrog
