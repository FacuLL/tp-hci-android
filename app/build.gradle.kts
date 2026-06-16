import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// La API key se lee de local.properties (no versionado) o de la env var API_KEY.
// Nunca se hardcodea ni se commitea. Ver local.properties.example.
val apiKey: String = run {
    val props = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { props.load(it) }
    props.getProperty("API_KEY") ?: System.getenv("API_KEY") ?: ""
}
if (apiKey.isBlank()) {
    logger.warn("WARNING: API_KEY no configurada (local.properties o env). La app no podra autenticarse.")
}

android {
    namespace = "tp3.grupo1.hci.itba.edu.ar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "tp3.grupo1.hci.itba.edu.ar"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inyectada en build time desde local.properties / env (no versionada).
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("lumina-release.jks")
            storePassword = "lumina2026"
            keyAlias = "lumina"
            keyPassword = "lumina2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    lint {
        error += listOf("MissingTranslation", "ExtraTranslation")
        abortOnError = true
    }
}

// Regla para evitar strings hardcodeadas en el código y obligar a traducir todo.
val verifyI18n by tasks.registering {
    group = "verification"
    description = "Fails on hardcoded UI strings and on es/en string-resource mismatches."

    val uiDir = layout.projectDirectory.dir("src/main/java/tp3/grupo1/hci/itba/edu/ar/ui")
    val resDir = layout.projectDirectory.dir("src/main/res")
    inputs.dir(uiDir)
    inputs.dir(resDir)

    doLast {
        val problems = mutableListOf<String>()

        val hardcoded = Regex("""Text\s*\(\s*(text\s*=\s*)?"|contentDescription\s*=\s*"""")
        uiDir.asFile.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if (hardcoded.containsMatchIn(line) && !line.contains("// i18n-ok")) {
                        problems += "Hardcoded string: ${file.name}:${index + 1}  ->${line.trim()}"
                    }
                }
            }

        fun keysIn(dirName: String): Set<String> {
            val dir = resDir.dir(dirName).asFile
            val files = dir.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }
                ?: return emptySet()
            val nameRx = Regex("""name="([^"]+)"""")
            return files.flatMap { f -> nameRx.findAll(f.readText()).map { it.groupValues[1] } }.toSet()
        }
        val es = keysIn("values")
        val en = keysIn("values-en")
        (es - en).sorted().forEach { problems += "Missing EN translation: $it" }
        (en - es).sorted().forEach { problems += "Missing ES (default) string: $it" }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "i18n check failed (${problems.size}):\n" + problems.joinToString("\n"),
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(verifyI18n) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.socketio.client)
    implementation(libs.reorderable)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
