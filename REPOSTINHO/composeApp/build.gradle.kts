import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

/*
 * A URL e o token do banco-api vêm do `local.properties`, que não vai para o git.
 *
 * O repositório é público: token commitado é token vazado, e é ele que separa os saldos
 * da rep da internet inteira. Com as chaves ausentes o app compila e roda usando o
 * retrato embutido, então quem clonar não fica travado.
 */
val bankApiProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val generateBankApiConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/bankApi")
    val baseUrl = bankApiProperties.getProperty("bancoApi.baseUrl").orEmpty()
    val token = bankApiProperties.getProperty("bancoApi.token").orEmpty()

    inputs.property("baseUrl", baseUrl)
    inputs.property("token", token)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
            .resolve("com/mach/apps/repostinho/data/remote")
        dir.mkdirs()
        dir.resolve("BankApiConfig.kt").writeText(
            """
            package com.mach.apps.repostinho.data.remote

            // Gerado pelo Gradle a partir do local.properties. Não edite à mão.
            internal object BankApiConfig {
                const val BASE_URL = "$baseUrl"
                const val TOKEN = "$token"
                val isConfigured: Boolean get() = BASE_URL.isNotBlank()
            }

            """.trimIndent()
        )
    }
}

/*
 * O plugin do Google só entra se o `google-services.json` existir.
 *
 * Ele é gitignored — o repositório é público —, e sem esta guarda um clone limpo falharia
 * no build em vez de subir com o login desligado.
 */
val firebaseConfigurado = file("google-services.json").exists()
if (firebaseConfigurado) {
    apply(plugin = "com.google.gms.google-services")
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
        commonMain {
            kotlin.srcDir(generateBankApiConfig.map { it.outputs.files.singleFile })
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation("io.insert-koin:koin-android:${libs.versions.koin.get()}")
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.networkKtor)
            implementation(libs.firebase.auth)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            implementation("io.insert-koin:koin-core:${libs.versions.koin.get()}")
            implementation("io.insert-koin:koin-compose:${libs.versions.koinCompose.get()}")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.mach.apps.repostinho"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mach.apps.repostinho"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

