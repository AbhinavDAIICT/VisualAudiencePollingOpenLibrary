# qCardsOpenLibrary


## Setup Andropid Studio

1. Clone the repository

2. Launch Anroid Studio

3. Go to "File" menu and choose "Open Project"
4. Navigate to the cloned repository location and select "qCardsOpenLibrary" project
5. Wait for Gradle build to finish

### If you get "android-library" not found error
1. Add this code to build.gradle 
```
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:0.9.+'
    } 
```
 (NOTE: Replace **0.9.+** with latest gradle version)

## Setup OpenCV
1. Follow steps at [this link](http://stackoverflow.com/questions/17767557/how-to-use-opencv-in-android-studio-using-gradle-build-tool)
(NOTE: use openCV version 2.4.10 for this project)
