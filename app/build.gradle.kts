import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import org.gradle.internal.extensions.stdlib.capitalized
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization")
    id("com.google.protobuf")

    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "ru.n08i40k.poco.triggers"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.n08i40k.poco.triggers"
        minSdk = 33
        targetSdk = 35
        versionCode = 8
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }

        buildConfigField("String", "BUILD_TIME", "\"${Date().toInstant().epochSecond}\"")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
            }

            assets.srcDirs("build/generated/assets")
        }
        getByName("test") {
            proto {
                srcDir("src/test/proto")
            }
        }
        getByName("androidTest") {
            proto {
                srcDir("src/androidTest/proto")
            }
        }
    }

    applicationVariants.all {
        val buildTypeCMake =
            if (buildType.name.lowercase() == "debug") "Debug" else "RelWithDebInfo"

        val cmakeTaskName = "buildCMake$buildTypeCMake[arm64-v8a]"

        val taskName =
            "copyPocoTriggersDaemon${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"

        val copyTask = tasks.register<Copy>(taskName) {
            dependsOn(cmakeTaskName)

            from(tasks.named(cmakeTaskName).get().outputs.files)

            mkdir(layout.buildDirectory.dir("generated/assets/bin"))
            into(layout.buildDirectory.dir("generated/assets/bin"))

            rename { "poco-triggers-daemon" }
        }

        mergeAssetsProvider.get().dependsOn(copyTask)

        tasks.matching { it.name.startsWith("lintVitalAnalyze${buildType.name.capitalized()}") }
            .configureEach {
                dependsOn(copyTask)
            }

        tasks.matching { it.name.startsWith("generate${buildType.name.capitalized()}LintVitalReportModel") }
            .configureEach {
                dependsOn(copyTask)
            }
    }

}

dependencies {
    // settings
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.lite)

    // run daemon as root
    implementation(libs.libsu.core)

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // default
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:21.0-rc-1"
    }

    plugins {
        id("javalite") {
            artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("javalite") { }
            }
        }
    }
}