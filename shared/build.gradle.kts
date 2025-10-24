import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktorfit.plugin)
    alias(libs.plugins.metro)
}

val isMac = System.getProperty("os.name").startsWith("Mac")
kotlin {
    // Androidをターゲットに設定
    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }
    if (isMac) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        // 全プラットフォームで共有されるコード
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.navigation.compose)
                // Permissions
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.microphone)
                implementation(libs.moko.permissions.camera)
                // mimi
                implementation(libs.mimi.engine.core)
                implementation(libs.mimi.engine.ktor)
                implementation(libs.mimi.service.token)
                implementation(libs.mimi.service.asr.core)
                implementation(libs.mimi.service.nict.asr)
                implementation(libs.mimi.service.nict.tts)
                implementation(libs.mimi.utils)
                // ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                // Date
                implementation(libs.kotlinx.datetime)
                // Ktorfit
                implementation(libs.ktorfit.lib)
                implementation(libs.ktorfit.annotations)
                // logger
                implementation(libs.napier)
                // kotlin-Result
                implementation(libs.kotlin.result)
                // markdown
                implementation(libs.markdown.renderer.core)
                implementation(libs.markdown.renderer.m3)
                // camera
                implementation(libs.camerak)
            }
        }

        // Android固有のコード
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.ktor.client.android)
            }
        }

        if (isMac) {
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }

                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
            }
        }
    }
}

buildkonfig {
    packageName = "ai.fd.shared.aichat"

    val secretPropsFile = project.rootProject.file("secrets.properties")
    if (!secretPropsFile.exists()) throw FileNotFoundException("secrets.properties not found")
    val properties = Properties()
    properties.load(FileInputStream(secretPropsFile))

    defaultConfigs {
        buildConfigField(STRING, "mimiApplicationId", properties.getProperty("mimiApplicationId"))
        buildConfigField(STRING, "mimiClientId", properties.getProperty("mimiClientId"))
        buildConfigField(STRING, "mimiClientSecret", properties.getProperty("mimiClientSecret"))

        buildConfigField(STRING, "geminiApiKey", properties.getProperty("geminiApiKey"))
        buildConfigField(STRING, "geminiEndpoint", properties.getProperty("geminiEndpoint"))
        buildConfigField(STRING, "geminiModel", properties.getProperty("geminiModel"))
    }
}

dependencies {
    listOf(
        libs.ktorfit.ksp
    ).forEach {
        add("kspAndroid", it)
        if (isMac) {
            add("kspIosX64", it)
            add("kspIosArm64", it)
            add("kspIosSimulatorArm64", it)
        }
    }
}

android {
    namespace = "ai.fd.shared.aichat"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
