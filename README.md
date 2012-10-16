# Introduction

Gnucash Mobile for Android is companion application for desktop Gnucash designed for Android.
It allows you to record transactions on-the-go and export them in the OFX format and later import the data into Gnucash for the desktop. You can create multiple accounts for transactions in Gnucash.

The application supports Android 2.2 (API level 8) and above. 


# Installation

There are different ways to get the Gnucash app for Android.

You can also build and install the Gnucash for Android application from source. This is of particular interest for those who want to contribute or those who wish to live on the bleeding edge. 

### App Store
Coming soon. Stay tuned….

### Eclipse

The Android SDK primarily supports Eclipse for development and consequently, all the subprojects in the GnucashMobile folder are Eclipse Android projects. In order to compile the application, you need to import the com_actionbarsherlock and GnucashMobile projects into your eclipse workspace. Then you can just invoke "Run as Android application" from eclipse in order to build and install the application on your Android device.

If you are interested in running the Robotium tests, also import the GnucashTest project into your workspace and run it as "Android JUnit Test".

### Maven

Gnucash for Android also supports the Apache Maven build automation tool. 
This method is more interesting if you do not want to download and install eclipse and the necessary Android plugins. It is especially interesting if you already have maven installed.
There are a few steps you need in order to get up and running with maven. 

* Download and install [Maven](http://maven.apache.org/download.html) (follow the instructions on the website)
* Clone the GnucashMobile source using: git clone git://github.com/codinguser/GnucashMobile.git
* Open a terminal in the GnucashMobile folder and run *mvn clean install*
(**Note**: If you also want to run the tests, see this [blog post](http://goo.gl/TprMw) for details )
* To install the application on your phone, switch to the GnucashMobile subfolder and run *mvn android:deploy*

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
