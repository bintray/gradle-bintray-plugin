# Overview

The Gradle Bintray Plugin allows you to publish artifacts to Bintray.

[ ![Download](https://api.bintray.com/packages/jfrog/jfrog-jars/gradle-bintray-plugin/images/download.svg) ](https://bintray.com/jfrog/jfrog-jars/gradle-bintray-plugin/_latestVersion)

# Getting Started Using the Plugin
Please follow the below steps to add the Gradle Bintray Plugin to your Gradle build script.

#### Step 1: [Sign up](https://bintray.com/docs/usermanual/working/working_allaboutjoiningbintraysigningupandloggingin.html) to [Bintray](https://bintray.com/) and locate your API Key under Edit Your Profile -> API Key

#### Step 2: Apply the plugin to your Gradle build script

To apply the plugin, please add one of the following snippets to your `build.gradle` file:

###### Gradle >= 2.1
```groovy
plugins {
    id "com.jfrog.bintray" version "1.6"
}
```
* Currently the "plugins" notation cannot be used for applying the plugin for sub projects, when used from the root build script.

###### Gradle < 2.1
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.6'
    }
}
apply plugin: 'com.jfrog.bintray'
```
* If you have a multi project build make sure to apply the plugin and the plugin configuration to every project which its artifacts you wish to publish to bintray.

#### Step 3: Add the `bintray` configuration closure to your `build.gradle` file

Add the below "bintray" closure with your bintray user name and key.

```groovy
bintray {
    user = 'bintray_user'
    key = 'bintray_api_key'
    ...
}
```

In case you prefer not to have your Bintray credentials explicitly defined in the script,
you can store them in environment variables or in external user properties and use them as follows:

```groovy
bintray {
    user = System.getenv('BINTRAY_USER')
    key = System.getenv('BINTRAY_KEY')
    ...
}
```


#### Step 4: Add your Bintray package information to the `bintray` closure

Mandatory parameters:

1. repo - existing repository in bintray to add the artifacts to (for example: 'generic', 'maven' etc)
2. name - package name
3. licenses - your package licenses (mandatory if the package doesn't exist yet and must be created, and if the package is an OSS package; optional otherwise)
4. vcsUrl - your VCS URL (mandatory if the package doesn't exist yet and must be created, and if the package is an OSS package; optional otherwise)

Optional parameters:

1. userOrg – an optional organization name when the repo belongs to one of the user's orgs. If not added will use 'BINTRAY_USER' by default

```groovy
bintray {   
    user = 'bintray_user'
    key = 'bintray_api_key'
    pkg {
        repo = 'generic'
        name = 'gradle-project'
        userOrg = 'bintray_user'
        licenses = ['Apache-2.0']
        vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
    }
}
```


#### Step 5: Add version information to the `pkg` closure

Mandatory parameters:

1. name - Version name

Optional parameters:

1. desc - Version description
2. released - Date of the version release. Can accept one of the following formats: 
    * Date in the format of 'yyyy-MM-dd'T'HH:mm:ss.SSSZZ'
    * java.util.Date instance
3. vcsTag - Version control tag name
4. attributes - Attributes to be attached to the version
    

```groovy
pkg {
    version {
        name = '1.0-Final'
        desc = 'Gradle Bintray Plugin 1.0 final'
        released  = new Date()
        vcsTag = '1.3.0'
        attributes = ['gradle-plugin': 'com.use.less:com.use.less.gradle:gradle-useless-plugin']
    }
}
```


#### Step 6: Define artifacts to be uploaded to Bintray

The plugin supports three methods to create groups of artifacts: Configurations, Publications and Copying specific files using filesSpec. One of the methods should be used to group artifacts to be uploaded to Bintray.

##### [Maven Publications](https://docs.gradle.org/current/dsl/org.gradle.api.publish.maven.MavenPublication.html)

* Maven Publications should be added to the Gradle script, outside of the bintray closure. They should however be referenced from inside the bintray closure.
* Ivy Publications are not supported.

Below you can find an example for Maven Publication that can be added to your Gradle script:

```groovy
publishing {
    publications {
        MyPublication(MavenPublication) {
            from components.java
            groupId 'org.jfrog.gradle.sample'
            artifactId 'gradle-project'
            version '1.1'
        }
    }
}
```

This Publication should be referenced from the bintray closure as follows:

```groovy
bintray {
    user = 'bintray_user'
    key = 'bintray_api_key' 
    publications = ['MyPublication'] 
}
```

* [Example project](https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples/publications-example) for using Maven Publications.

##### [Configurations](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html)

Configurations should be added to the Gradle script, outside of the bintray closure. They should however be referenced from inside the bintray closure.

The following example uses the archives Configuration by applying the java plugin:

```groovy
apply plugin: 'java'
```
and the Configuration should be referenced from the bintray closure as follows:

```groovy
bintray {
    user = 'bintray_user'
    key = 'bintray_api_key' 
    configurations = ['archives']
}
```

* [Example project](https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples/configurations-example) for using Configurations.

##### Copying specific files using filesSpec

FilesSpec is following [Gradle's CopySpec](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html) which is used by the copy task.

Below you can find an example for uploading arbitrary files from a specific folder ('build/libs') to a directory ('standalone_files/level1') under the build version in bintray using filesSpec.

```groovy
bintray {
    user = 'bintray_user'
    key = 'bintray_api_key'
    filesSpec {
       from 'build/libs'
       into 'standalone_files/level1'
    }
}
```

* [Example project](https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples/filesSpec-example) for using filesSpec.


#### Step 7: Run the build

> gradle bintrayUpload

# Creating Repositories, Packages and Versions
## General
*  When uploading files to Bintray, you need to specify the Repository, Package and Version to which files are uploaded. The plugin checks whether the specified Package already exists in the specified Repository. If the specified Package does not exist, the plugin will create it. The same is done for the specified version, so you don't need to worry about having your builds deploy your artifacts into new Packages and Versions. The plugin, however, expects the Repository to exist already and the plugin will not try to create the Repository if it does not exist. 
* The plugin uses the specified Package and Version details, for example, the Package VCS URL, only for creating the Package and Version. The plugin will not attempt to update those properties if the Package or Version already exist.

## Mandatory properties
* For the *pkg* closure, if the package already exists, the only mandatory properties are *repo* and *name*. If the package does not exist, the *licenses* and *vcsUrl properties are manadatory for OSS packages.
* For the *version* closure, the only mandatory property is *name*.

# GPG File Signing
The plugin allows using Bintray supports for files GPG signing. To have your Version files signed by Bintray, you first need to [configure your public and private GPG keys](https://bintray.com/docs/usermanual/interacting/interacting_editingyouruserprofile.html#anchorMANAGINGGPGKEYS) in Bintray, and then add the *gpg* closure inside the *version* closure as shown in the below *Plugin DSL* section. If your GPG keys are not configured in Bintray and `sign` is true, then the files will be signed using Bintray's internal keys.

# Maven Central Sync
The plugin allows using Bintray's interface with Maven Central. You can have the artifacts of a Version sent to Maven Central, by adding the adding the *mavenCentralSync* closure inside the *version* closure, as shown in the below *Plugin DSL* section.
If that closure is omitted, the version will not be sent to Maven central.

In order for this functionality to be enabled, you first must verify the following:
* The Version belongs to a Repository whose type is Maven and the Version belongs to a Package that is [included in JCenter](https://bintray.com/docs/usermanual/uploads/uploads_includingyourpackagesinacentralrepository.html).
* Your package must comply with the requirement of Maven Central (click [here](http://central.sonatype.org/pages/requirements.html) for more information). In particular, files must be signed to be sent to Maven Central. So GPG file signing should also be enabled (see above) if Maven Central sync is enabled.

# Plugin DSL
The Gradle Bintray plugin can be configured using its own Convention DSL inside the build.gradle script of your root project.
The syntax of the Convention DSL is described below:

## build.gradle
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
    dryRun = false //[Default: false] Whether to run this as dry-run, without deploying
    publish = true //[Default: false] Whether version should be auto published after an upload    
    override = false //[Default: false] Whether to override version artifacts already published    
    //Package configuration. The plugin will use the repo and name properties to check if the package already exists. In that case, there's no need to configure the other package properties (like userOrg, desc, etc).
    pkg {
        repo = 'myrepo'
        name = 'mypkg'
        userOrg = 'myorg' //An optional organization name when the repo belongs to one of the user's orgs
        desc = 'what a fantastic package indeed!'
        websiteUrl = 'https://github.com/bintray/gradle-bintray-plugin'
        issueTrackerUrl = 'https://github.com/bintray/gradle-bintray-plugin/issues'
        vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
        licenses = ['Apache-2.0']
        labels = ['gear', 'gore', 'gorilla']
        publicDownloadNumbers = true
        attributes= ['a': ['ay1', 'ay2'], 'b': ['bee'], c: 'cee'] //Optional package-level attributes

        githubRepo = 'bintray/gradle-bintray-plugin' //Optional Github repository
        githubReleaseNotesFile = 'README.md' //Optional Github readme file
                
        //Optional version descriptor
        version {
            name = '1.3-Final' //Bintray logical version name
            desc = //Optional - Version-specific description'
            released  = //Optional - Date of the version release. 2 possible values: date in the format of 'yyyy-MM-dd'T'HH:mm:ss.SSSZZ' OR a java.util.Date instance
            vcsTag = '1.3.0'
            attributes = ['gradle-plugin': 'com.use.less:com.use.less.gradle:gradle-useless-plugin'] //Optional version-level attributes
            //Optional configuration for GPG signing
            gpg {
                sign = true //Determines whether to GPG sign the files. The default is false
                passphrase = 'passphrase' //Optional. The passphrase for GPG signing'
            }
            //Optional configuration for Maven Central sync of the version
            mavenCentralSync {
                sync = true //[Default: true] Determines whether to sync the version to Maven Central.
                user = 'userToken' //OSS user token: mandatory
                password = 'paasword' //OSS user password: mandatory
                close = '1' //Optional property. By default the staging repository is closed and artifacts are released to Maven Central. You can optionally turn this behaviour off (by puting 0 as value) and release the version manually.
            }            
        }
    }
}
```
* As an example, you can also refer to these [sample projects](https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples).

**Gradle Compatibility:**
When using Gradle publications or when using `filesSpec` for direct file uploads, you'll need to use Gradle 2.x; Otherwise, the plugin is compatible with Gradle 1.12 and above.

**JVM Compatibility:**
Java 6 and above.

# Example Projects
As an example, you can also refer to these [sample projects](https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples).



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
