import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.material.icons.extended)
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt:coil-svg:2.5.0")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.yandex.mapkit.kmp)
            implementation(libs.yandex.mapkit.kmp.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.cio)
            implementation(libs.compass.geolocation)
            implementation(libs.compass.geolocation.mobile)
            implementation(libs.compass.permissions.mobile)
            implementation(libs.postgresql)
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
            implementation("io.github.ismoy:imagepickerkmp:1.0.28-beta3")
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.placy"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.placy"
        minSdk = 26
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val keystoreFile = project.rootProject.file("apikey.properties")
        val properties = Properties()
        if (keystoreFile.exists()) {
            keystoreFile.inputStream().use(properties::load)
        }

        val list = listOf(
            "MAPKIT_API_KEY",
            "SERVER_URL",
            "USERNAME",
            "PASSWORD",
        )

        for(element in list) {
            buildConfigField(
                type = "String",
                name = element,
                value = properties.getProperty(element, when (element) {
                    "SERVER_URL" -> "\"https://example.invalid\""
                    "USERNAME" -> "\"test-user\""
                    else -> "\"\""
                })
            )
        }

        // ДОБАВИТЬ для тестов
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    // ДОБАВИТЬ ЭТИ НАСТРОЙКИ
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // ДОБАВИТЬ для указания AndroidManifest для тестов
    sourceSets {
        getByName("androidTest") {
            manifest.srcFile("src/androidTest/AndroidManifest.xml")
            java.srcDirs("src/androidTest/kotlin")
            resources.srcDirs("src/androidTest/resources")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    implementation(libs.kotlin.test)
    implementation(libs.junit)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.rules)
    implementation(libs.androidx.junit.v115)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)

    // Mockito
    implementation(libs.mockito.kotlin)
    implementation(libs.mockito.core)

    // Coroutines тестирование
    implementation(libs.kotlinx.coroutines.test)

    // Ktor Mock
    implementation(libs.ktor.client.mock)

    // Compose UI тестирование (если нужно)
    implementation(libs.androidx.ui.test.junit4)
}
// Branch coverage for the Android debug unit tests; only generated resource and configuration classes are excluded.
tasks.register<JacocoReport>("unitBranchCoverage") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*")
    })
    sourceDirectories.setFrom(files("src/androidMain/kotlin", "src/commonMain/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec", "outputs/unit_test_code_coverage/debugUnitTest/*.exec")
    })
}
