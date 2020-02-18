# Overview

The Gradle Bintray Plugin allows you to publish artifacts to Bintray.

[ ![Download](https://api.bintray.com/packages/jfrog/jfrog-jars/gradle-bintray-plugin/images/download.svg) ](https://bintray.com/jfrog/jfrog-jars/gradle-bintray-plugin/_latestVersion)

# Table of Contents  
[Getting Started Using the Plugin](#Getting_Started_Using_the_Plugin)<br>
[Building and Testing the Sources](#Building_and_Testing_the_Sources)<br>
[Creating Repositories, Packages and Versions](#Creating_Repositories,_Packages_and_Versions)<br>
[GPG File Signing](#GPG_File_Signing)<br>
[Maven Central Sync](#Maven_Central_Sync)<br>
[Plugin DSL](#Plugin_DSL)<br>
[Example Projects](#Example_Projects)<br>
[Building and Testing the Sources](#Building_and_Testing_the_Sources)<br>
[Release Notes](#Release_Notes)<br>
[Code Contributions](#Code_Contributions)<br>
[License](#License)<br>

<a name="Getting_Started_Using_the_Plugin"/>

# Getting Started Using the Plugin
Please follow the below steps to add the Gradle Bintray Plugin to your Gradle build script.

#### Step 1: [Sign up](https://bintray.com/docs/usermanual/working/working_allaboutjoiningbintraysigningupandloggingin.html) to [Bintray](https://bintray.com/) and locate your API Key under Edit Your Profile -> API Key

#### Step 2: Apply the plugin to your Gradle build script

To apply the plugin, please add one of the following snippets to your `build.gradle` file:

###### Gradle >= 2.1
```groovy
plugins {
    id "com.jfrog.bintray" version "1.+"
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
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.+'
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

#### Step 5: If you're uploading a Debian package, configure its details
If your Gradle build deploys a Debian package to a Debian repository, you need to specify the Distribution, Component and Architecture for the package.
You do this by adding the *debian* closure into the *pkg* closure.

```groovy
    pkg {
        repo = 'generic'
        name = 'gradle-project'
        userOrg = 'bintray_user'
        licenses = ['Apache-2.0']
        vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
        debian {
            distribution = 'squeeze'
            component = 'main'
            architecture = 'i386,noarch,amd64'
        }
    }
```
The *component* property is optional and has *main* as its default value.

#### Step 6: Add version information to the `pkg` closure

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


#### Step 7: Define artifacts to be uploaded to Bintray

The plugin supports three methods to create groups of artifacts: Configurations, Publications and Copying specific files using filesSpec.  One of the methods should be used to group artifacts to be uploaded to Bintray.  Using the Configurations approach is the easiest, since this option attempts to infer what artifacts to upload based on the Gradle project and dependencies that are defined.  Publications gives more fine-grained control, especially when needing to publish metadata for publishing to Maven Central.  Copying specific files can be used as a last option, which provides the ability to define custom rules using the Gradle's [CopySpec](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html) task.  In general, the first two options should be sufficient for your needs.

##### [Maven Publications](https://docs.gradle.org/current/dsl/org.gradle.api.publish.maven.MavenPublication.html)

* Maven Publications should be added to the Gradle script, outside of the bintray closure. They should however be referenced from inside the bintray closure.
* Applying the *maven-publish* plugin is required when using Maven Publications.
* To avoid [this](https://github.com/gradle/gradle/issues/1118) issue, which can cause transitive dependencies of your published artifacts not to be included, make sure to apply the *java-library* plugin to your build script. Applying the plugin resolves the issue for Gradle version 3.4 and above. For Gradle versions below 3.4, you can use [this](https://gist.github.com/bugs84/b7887fb5d7f9f2d484b8) workaround. 
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
Here's another publication example, which adds sourcesJar, javadocJar and configures the generated pom.xml.
You need the sourcesJar in case you'd like your package to be linked to JCenter.
In case you'd also like Bintray to sync your package to Maven Central, you'll need sourcesJar, javadocJar and the generated pom.xml must comply with [Maven Central's requirements](http://central.sonatype.org/pages/requirements.html).

```groovy
// Create the pom configuration:
def pomConfig = {
    licenses {
        license {
            name "The Apache Software License, Version 2.0"
            url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution "repo"
        }
    }
    developers {
        developer {
            id "developer-id"
            name "developer-name"
            email "dev@d.com"
        }
    }
    
    scm {
       url "https://github.com/yourgithubaccount/example"
    }
}

// Create the publication with the pom configuration:
publishing {
    publications {
        MyPublication(MavenPublication) {
            from components.java
            artifact sourcesJar
            artifact javadocJar
            groupId 'org.jfrog.gradle.sample'
            artifactId 'gradle-project'
            version '1.1'
            pom.withXml {
                def root = asNode()
                root.appendNode('description', 'Your description of the lib')
                root.appendNode('name', 'Your name of the lib')
                root.appendNode('url', 'https://site_for_lib.tld')
                root.children().last() + pomConfig
            }
        }
    }
}
```

If you are trying to publish an Android project, specifying the `from components.java` line in the above example is not applicable.  Also, the POM file generated does not include the dependency chain so it must be explicitly added using this [workaround](https://discuss.gradle.org/t/maven-publish-doesnt-include-dependencies-in-the-project-pom-file/8544).

```groovy
publishing {
    publications {
        MyPublication(MavenPublication) {
          // Define this explicitly if using implementation or api configurations
          pom.withXml {
            def dependenciesNode = asNode().getAt('dependencies')[0] ?: asNode().appendNode('dependencies')
          
            // Iterate over the implementation dependencies (we don't want the test ones), adding a <dependency> node for each
            configurations.implementation.allDependencies.each {
               // Ensure dependencies such as fileTree are not included.
               if (it.name != 'unspecified') {
                  def dependencyNode = dependenciesNode.appendNode('dependency')
                  dependencyNode.appendNode('groupId', it.group)
                  dependencyNode.appendNode('artifactId', it.name)
                  dependencyNode.appendNode('version', it.version)
               }
            }
          }
        }
    }
}     
```

The Publication should be referenced from the bintray closure as follows:

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


#### Step 8: Run the build

> gradle bintrayUpload

<a name="Creating_Repositories,_Packages_and_Versions"/>

# Creating Repositories, Packages and Versions
## General
*  When uploading files to Bintray, you need to specify the Repository, Package and Version to which files are uploaded. The plugin checks whether the specified Package already exists in the specified Repository. If the specified Package does not exist, the plugin will create it. The same is done for the specified version, so you don't need to worry about having your builds deploy your artifacts into new Packages and Versions. The plugin, however, expects the Repository to exist already and the plugin will not try to create the Repository if it does not exist. 
* The plugin uses the specified Package and Version details, for example, the Package VCS URL, only for creating the Package and Version. The plugin will not attempt to update those properties if the Package or Version already exist.

## Mandatory properties
* For the *pkg* closure, if the package already exists, the only mandatory properties are *repo* and *name*. If the package does not exist, the *licenses* and *vcsUrl properties are manadatory for OSS packages.
* For the *version* closure, the only mandatory property is *name*.

<a name="GPG_File_Signing"/>

# GPG File Signing
The plugin allows using Bintray supports for files GPG signing. To have your Version files signed by Bintray, you first need to [configure your public and private GPG keys](https://bintray.com/docs/usermanual/interacting/interacting_editingyouruserprofile.html#anchorMANAGINGGPGKEYS) in Bintray, and then add the *gpg* closure inside the *version* closure as shown in the below *Plugin DSL* section. If your GPG keys are not configured in Bintray and `sign` is true, then the files will be signed using Bintray's internal keys.

<a name="Maven_Central_Sync"/>

# Maven Central Sync
The plugin allows using Bintray's interface with Maven Central. You can have the artifacts of a Version sent to Maven Central, by adding the adding the *mavenCentralSync* closure inside the *version* closure, as shown in the below *Plugin DSL* section.
If that closure is omitted, the version will not be sent to Maven central.

In order for this functionality to be enabled, you first must verify the following:
* The Version belongs to a Repository whose type is Maven and the Version belongs to a Package that is [included in JCenter](https://bintray.com/docs/usermanual/uploads/uploads_includingyourpackagesinacentralrepository.html).
* Your package must comply with the requirement of Maven Central (click [here](http://central.sonatype.org/pages/requirements.html) for more information). 
In particular, `sourcesJar`, `javadocsJar` and valid `pom.xml` must be included (see above); 
also files must be signed to be sent to Maven Central, so GPG file signing should be enabled (see above) if Maven Central sync is enabled.

<a name="Plugin_DSL"/>

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

        //Optional Debian details
        debian {
            distribution = 'squeeze'
            component = 'main'
            architecture = 'i386,noarch,amd64'
        }
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

<a name="Example_Projects"/>

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

<a name="Building_and_Testing_the_Sources"/>

# Building and Testing the Sources
The code is built using Gradle and includes integration tests. The tests use real repositories in your Bintray organization.
## Before running the tests
Make sure you have the following repositories in your Bintray organization:
* A Maven repository named **maven**.
* A Debian repository named **debian**.
Also, the following environment variable should be set:
```
export BINTRAY_USER=<Your Bintray user name>
export BINTRAY_KEY=<Your Bintray API Key>
export BINTRAY_ORG=<Your Bintray organization name>
```

## Running the build with or without tests
To build the code using the gradle wrapper in Unix run:
```
> ./gradlew clean build
```

To build the code using the gradle wrapper in Windows run:
```
> gradlew clean build
```

To build the code using the environment gradle run:
```
> gradle clean build
```

To build the code without running the tests, add to the "clean build" command the "-x test" option, for example:
```
> ./gradlew clean build -x test
```

<a name="Release_Notes"/>

# Release Notes
The release notes are available on [Bintray](https://bintray.com/jfrog/jfrog-jars/gradle-bintray-plugin#release).

<a name="Code_Contributions"/>

# Code Contributions
We welcome code contributions through pull requests.
Please join our contributors community and help us make this plugin even better!

<a name="License"/>

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved JFrog
