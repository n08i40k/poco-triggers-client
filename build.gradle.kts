// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    kotlin("plugin.serialization") version "2.1.10" apply false
    id("com.google.protobuf") version "0.9.4" apply false

    id("com.google.devtools.ksp") version "2.1.20-2.0.0" apply false
    id("com.google.dagger.hilt.android") version "2.55" apply false
}