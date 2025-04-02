import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization") version "2.1.10"
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "ru.n08i40k.poco.triggers"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.n08i40k.poco.triggers"
        minSdk = 33
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
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
        val buildTypeFolder =
            if (buildType.name.lowercase() == "debug") "Debug" else "RelWithDebInfo"

        val taskName =
            "copyPocoTriggersDaemon${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"

        tasks.register<Copy>(taskName) {
            from(provider {
                val cxxDir = file("${projectDir}/build/intermediates/cxx/$buildTypeFolder")

                if (!cxxDir.exists())
                    throw GradleException("Папка $cxxDir не существует!")

                val buildIdDirs = cxxDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

                if (buildIdDirs.isEmpty())
                    throw GradleException("Нет доступных build_id директорий в $cxxDir")

                val lastCreatedDir = buildIdDirs.maxByOrNull { it.lastModified() }
                    ?: throw GradleException("Не удалось определить последнюю директорию в $cxxDir")

                val sourceFile = file("${lastCreatedDir.path}/obj/arm64-v8a/poco-triggers-daemon")

                if (!sourceFile.exists())
                    throw GradleException("Файл poco-triggers-daemon не найден в ${lastCreatedDir.path}")

                listOf(sourceFile)
            })

            into("${layout.buildDirectory}/generated/assets/bin")
            rename { "poco-triggers-daemon" }
        }.dependsOn("package${buildType.name.capitalized()}").dependsOn("buildCMake$buildTypeFolder[arm64-v8a]")

        assembleProvider.get().dependsOn(taskName)
    }
}

dependencies {
    // settings
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.lite)

    // run daemon as root
    implementation(libs.libsu.core)

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