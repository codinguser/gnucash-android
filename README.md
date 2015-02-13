# Introduction

Gnucash Mobile for Android is companion application for desktop Gnucash designed for Android.
It allows you to record transactions on-the-go and export them in the QIF or OFX format and later import the data into Gnucash for the desktop. You can create multiple accounts for transactions in Gnucash. Double-entry accounting is also supported.

The application supports Android 2.2 Froyo (API level 8) and above. 


# Installation

There are different ways to get the Gnucash app for Android; through the app store, or building it yourself.


### App Store

<a href="http://play.google.com/store/apps/details?id=org.gnucash.android">
  <img alt="Android app on Google Play" src="http://developer.android.com/images/brand/en_generic_rgb_wo_60.png" />
</a>


## Building

### With Gradle

Run `gradlew build installDebug` from the within the project folder.
It will build the project for you and install it to the connected Android device or running emulator.


### With Android Studio
The easiest way to build is to install [Android Studio](https://developer.android.com/sdk/index.html) v1.+
with [Gradle](https://www.gradle.org/) v2.2.1.
Once installed, then you can import the project into Android Studio:

1. Open `File`
2. Import Project
3. Select `build.gradle` under the project directory
4. Click `OK`

Then, Gradle will do everything for you.

### With Maven
The build requires [Maven](http://maven.apache.org/download.html)
v3.1.1+ and the [Android SDK](http://developer.android.com/sdk/index.html)
to be installed in your development environment. In addition you'll need to set
the `ANDROID_HOME` environment variable to the location of your SDK:

    export ANDROID_HOME=/home/<user>/tools/android-sdk

After satisfying those requirements, the build is pretty simple:

* Run `mvn clean package` from the `app` directory to build the APK only
* Run `mvn clean install` from the root directory to build the app and also run
  the integration tests, this requires a connected Android device or running
  emulator. (see this [blog post](http://goo.gl/TprMw) for details)

You might find that your device doesn't let you install your build if you
already have the version from the Android Market installed.  This is standard
Android security as it it won't let you directly replace an app that's been
signed with a different key.  Manually uninstall GnuCash from your device and
you will then be able to install your own built version.

## Contributing

There are several ways you could contribute to the development.

One way is providing translations for locales which are not yet available, or improving translations.
See this [blog post](http://www.codinguser.com/2012/09/gnucash-for-android-beta-2-lost-in-translation/) for some guidelines.

You could as well contribute code, fixing bugs, new features or automated tests.
Take a look at the [bug tracker](https://github.com/codinguser/gnucash-android/issues?state=open)
for ideas where to start.

For development, it is recommended to use the IntelliJ IDEA 14+ IDE for development which is available as free
community edition. Import the project into the IDE from an external (maven) model.The IDE will resolve dependencies automatically.

#Licence
Gnucash for Android is free software; you can redistribute it and/or 
modify it under the terms of the Apache license, version 2.0.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and 
limitations under the License.
