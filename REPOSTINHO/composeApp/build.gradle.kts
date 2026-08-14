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
 * A URL do banco-api vem do `local.properties`, que não vai para o git.
 *
 * O token **não** entra mais no binário. Quem autoriza o app é o token do morador logado,
 * emitido pelo Firebase; o `x-rep-token` virou chave de administração e fica só na máquina
 * de quem cadastra por linha de comando. Enquanto ele era compilado aqui, extrair o APK
 * dava acesso a saldos, emails e fotos da rep inteira — e o app vai para quinze aparelhos.
 */
val bankApiProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val generateBankApiConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/bankApi")
    val baseUrl = bankApiProperties.getProperty("bancoApi.baseUrl").orEmpty()

    inputs.property("baseUrl", baseUrl)
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
    /*
     * Assinatura de release lida do `local.properties`, como a URL do banco-api.
     *
     * Sem ela o APK sai sem assinar e o Android recusa instalar. O keystore em si fica
     * fora do repositório: perdê-lo significa nunca mais conseguir atualizar o app nos
     * aparelhos que já o têm, porque o Android só aceita atualização assinada pela mesma
     * chave.
     */
    val keystorePath = bankApiProperties.getProperty("repostinho.keystore").orEmpty()
    val keystoreExiste = keystorePath.isNotBlank() && file(keystorePath).exists()

    signingConfigs {
        if (keystoreExiste) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = bankApiProperties.getProperty("repostinho.keystorePassword")
                keyAlias = bankApiProperties.getProperty("repostinho.keyAlias")
                keyPassword = bankApiProperties.getProperty("repostinho.keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Sem keystore o build continua funcionando e sai sem assinar, para quem
            // clonar não travar — só não dá para instalar.
            if (keystoreExiste) signingConfig = signingConfigs.getByName("release")
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

