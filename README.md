# updraft-plugin 🚀

This is a gradle plugin for automated upload to updraft.


## Instructions

In order to use the plugin follow those steps:

Preconditions:
- Uses Java 11
 
1.Add the code below to you `build.gradle` file in the project root folder.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.appswithlove.updraft/updraft/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.appswithlove.updraft/updraft)

```groovy
buildscript {
  repositories {
    ...
    mavenCentral()
  }

  dependencies {
    ...
    classpath 'com.appswithlove.updraft:updraft:2.2.7'
  }
}
```

or 

```kotlin
plugins {
  id("com.appswithlove.updraft") version "2.2.7"
}
```

2.Apply the plugin in `app/build.gradle`

```groovy
    id 'com.appswithlove.updraft'
```

or 

```kotlin
  id("com.appswithlove.updraft")
```

3.Add one or multiple `urls['YOURPRODUCTFLAVOUR']` wrapped in `updraft` to the file. To get the url, go to your Updraft App and get the https:// url part of the `curl` command. (e.g. https://updraft.com/api/app_upload/.../.../)
With this, the plugin knows to which updraft app your apk should be uploaded.

The part `YourBuildVariant` should be replaced by the exact name your build variant. For example: 
 

```groovy
updraft {
  urls['StagingRelease'] = ["your/staging/url/"]
  urls['ProdRelease'] = ["your/prod/url/", "your/prod2/url/"]
}
```

or
```kotlin
updraft {
    urls = mapOf(
        "ProdRelease" to listOf("your/prod/url"),
        "StagingRelease" to listOf("your/url"),
    )
}
```

4.Done! 

## Usage
After installing the plugin, you should be able to find the Gradle Updraft tasks in Android Studio. The naming is always `updraft` + buildVariant (`updraftBundle` + buildVariant for App Bundles). The appropriate url will be chosen as destination. There is 1 task for every available buildVariant.
`"Gradle Project" Window -> Tasks -> Other -> updraft... (e.g. updraftRelease or updraftBundleRelease)`
In order to use them, make sure that you build the project *before*. 

Otherwise, you can call the gradle tasks via command: 

```
./gradlew updraftRelease // for APK
./gradlew updraftBundleRelease // for AAB
```

Or combined with clean + assemble: 

```
./gradlew clean assembleRelease updraftRelease // for APK
./gradlew clean bundleRelease updraftBundleRelease // for AAB
```

## Breaking Change
When Upgrading from Version `2.1.7` to `2.2.0`, the plugin id must be changed from `updraft` to `com.appswithlove.updraft`

## Release Notes
In order to upload release notes, there are 3 options:
1. Last commit message (default). If you don't specify anything, the release notes will contain the content of the latest commit message.
2. Add parameter `releaseNotes` to `updraft` tag and pass in a string or function that generates a string.
3. Add your release notes to `/src/main/updraft/release-notes.txt` or `/src/someFlavor/updraft/release-notes.txt`. If this file exists in either `main` or your current `flavour`, it will be taken instead of the git commit message.
4. You can also pass releaseNotes as a runtime parameter `./gradlew updraftRelease -PreleaseNotes="your release notes"`


## Debug

In order to debug the plugin, `clean` -> `jar` -> `publishJarPublicationToMavenLocal` and connect your android App to the mavenLocal-version of the android plugin by adding the following snipped to your root-folder `build.gradle`

```groovy
buildscript {
	repositories {
		mavenLocal()
		...
	}
	dependencies {
	    classpath 'com.appswithlove.updraft:updraft:2.2.7'
	    ...
	}
}
```

After that, call the following script in the terminal of your android app (replace `FLAVOUR`)

```console
./gradlew updraftRelease -Dorg.gradle.debug=true --no-daemon
```

Lastly, open the Updraft Plugin in Android Studio, add an `Remote` build configuration with `Attach to remote JVM` and run the configuration on debug. Now the gradlew call you triggered before will start running and will hit the break points in the plugin. :)

Don't forget to republish the plugin-jar when doing changes.
