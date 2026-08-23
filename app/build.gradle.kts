plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

apply(plugin = "org.jetbrains.kotlin.kapt")

android {
    namespace = "pk.vexel.financepassport"
    compileSdk = 36

    defaultConfig {
        applicationId = "pk.vexel.financepassport"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Each androidTest class otherwise shares one continuous app process/database for the
        // whole connectedAndroidTest run, so an in-flight viewModelScope write cancelled by a
        // prior test's Activity teardown can leave shared state (Room's connection pool) wedged
        // for every test that follows. Orchestrator + clearPackageData gives every test class a
        // fresh process and app data, matching how each test is actually written to assume it
        // starts from a clean install.
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables.useSupportLibrary = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Internal QA only. Replace with the external production signing key before publishing.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("androidx.biometric:biometric:1.1.0")
    add("kapt", "androidx.room:room-compiler:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    // Room 2.8 migration bundles use the 1.8 serialization ABI; pin the
    // instrumentation helper's runtime away from older transitive BOMs.
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestUtil("androidx.test:orchestrator:1.5.1")
}

extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension>("kapt") {
    arguments { arg("room.schemaLocation", "$projectDir/schemas") }
}

configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
    )
}
