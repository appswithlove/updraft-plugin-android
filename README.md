# updraft-plugin 🚀

This is a gradle plugin for automated upload to updraft.


## Instructions

In order to use the plugin follow those steps:
 
1.Add the code below to you `build.gradle` file in the `app` folder.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.appswithlove.updraft/updraft/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.appswithlove.updraft/updraft)

```groovy
buildscript {
  repositories {
    ...
    mavenCentral()
  }

  dependencies {
    ...
    classpath 'com.appswithlove.updraft:updraft:VERSION'
  }
}
```

2.Apply the plugin in `build.gradle` in `app` folder (same file as before).

```groovy
apply plugin: 'updraft'
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

4.Done! 

## Usage
After installing the plugin, you should be able to find the Gradle Updraft tasks in Android Studio. The naming is always `updraft` + buildVariant. The appropriate url will be choosen as destination. There is 1 task for every available buildVariant. 
`"Gradle Project" Window -> Tasks -> Other -> updraft... (e.g. updraftRelease)`
In order to use them, make sure that you build the project *before*. 

Otherwise, you can call the gradle tasks via command: 

```
./gradlew updraftRelease
```

Or combined with clean + assemble: 

```
./gradlew clean assembleRelease updraftRelease
```

## Debug

In order to debug the plugin, `clean` -> `jar` -> `publishJarPublicationToMavenLocal` and connect your android App to the mavenLocal-version of the android plugin by adding the following snipped to your root-folder `build.gradle`

```groovy
buildscript {
	repositories {
		mavenLocal()
		...
	}
	dependencies{
	    classpath 'com.appswithlove.updraft:updraft:VERSION'
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
