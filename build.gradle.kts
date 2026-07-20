buildscript {
    repositories {
        maven { url = uri("${rootDir}/gradle/offline-repo") }
        maven { url = uri("${rootDir}/gradle/plugin-repo") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.52")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.21-1.0.28")
    }
}
