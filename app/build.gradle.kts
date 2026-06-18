import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String): String = localProps.getProperty(key, "")

android {
    namespace = "com.example.sellerhelperebay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sellerhelperebay"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "EBAY_ENV", "\"${localProp("ebay.env").ifEmpty { "SANDBOX" }}\"")
        buildConfigField("String", "EBAY_SANDBOX_CLIENT_ID", "\"${localProp("ebay.sandbox.clientId")}\"")
        buildConfigField("String", "EBAY_SANDBOX_CLIENT_SECRET", "\"${localProp("ebay.sandbox.clientSecret")}\"")
        buildConfigField("String", "EBAY_SANDBOX_RUNAME", "\"${localProp("ebay.sandbox.ruName")}\"")
        buildConfigField("String", "EBAY_PROD_CLIENT_ID", "\"${localProp("ebay.prod.clientId")}\"")
        buildConfigField("String", "EBAY_PROD_CLIENT_SECRET", "\"${localProp("ebay.prod.clientSecret")}\"")
        buildConfigField("String", "EBAY_PROD_RUNAME", "\"${localProp("ebay.prod.ruName")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.firebase.ai)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
