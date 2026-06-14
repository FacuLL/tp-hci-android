plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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
    }

    signingConfigs {
        // Self-signed key checked into the repo so the chair can rebuild the
        // exact same installable APK (course project, not a store release).
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
    }
    lint {
        // Every translatable string must exist in all locales (es default + en).
        error += listOf("MissingTranslation", "ExtraTranslation")
        abortOnError = true
    }
}

// ── i18n enforcement ─────────────────────────────────────────────────────────
// Hard gate run before every build: fails compilation when a user-facing string
// is hardcoded in the UI, or when the es/en string catalogs drift apart.
val verifyI18n by tasks.registering {
    group = "verification"
    description = "Fails on hardcoded UI strings and on es/en string-resource mismatches."

    val uiDir = layout.projectDirectory.dir("src/main/java/tp3/grupo1/hci/itba/edu/ar/ui")
    val resDir = layout.projectDirectory.dir("src/main/res")
    inputs.dir(uiDir)
    inputs.dir(resDir)

    doLast {
        val problems = mutableListOf<String>()

        // 1) Hardcoded user-facing string literals in Compose code.
        //    Matches Text("…"), Text(text = "…") and contentDescription = "…".
        //    Add `// i18n-ok` on a line to whitelist a deliberate exception.
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

        // 2) String-resource parity between the default (es) and en catalogs.
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
