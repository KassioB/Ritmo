import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// O arquivo do projeto Firebase fica fora do git. Sem ele o app compila, mas o login fica indisponível,
// a não ser que a compilação aponte para os emuladores do Firebase (veja FIREBASE_EMULATOR_HOST).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// A chave e a senha ficam juntas fora do projeto. Sem elas, o APK de release sai sem assinatura.
val signing = Properties().apply {
    File(System.getProperty("user.home"), ".ritmo/signing.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}

android {
    namespace = "com.ritmo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ritmo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Só para testes: -Pritmo.firebaseEmulator=10.0.2.2 liga o app aos emuladores locais do Firebase.
        val emulatorHost = providers.gradleProperty("ritmo.firebaseEmulator").getOrElse("")
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")
    }

    signingConfigs {
        signing.getProperty("storeFile")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        // Os testes de migração leem os esquemas exportados pelo Room.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

androidComponents {
    onVariants { variant ->
        // Gerado por tools/generate_pet_icons.py: são dezenas de atalhos, um por estado do pet.
        variant.sources.manifests.addStaticManifestFile("src/main/pet-icons/AndroidManifest.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    // O Firebase traz um fragment antigo, que quebra o pedido de permissão de notificações.
    implementation(libs.androidx.fragment)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
