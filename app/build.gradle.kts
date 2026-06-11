import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.bughunter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bughunter"
        minSdk = 29
        targetSdk = 36
        versionCode = 30000
        versionName = "2.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ──────────────────────────────────────────────────────────────
        // APK size controls
        // ──────────────────────────────────────────────────────────────
        // Ship native libs (Tink/BoringSSL, Conscrypt, …) for arm64-v8a
        // only. This covers every Android phone shipped since ~2017 —
        // Google Play's own Distribution dashboards put arm64 at >98% of
        // active devices in 2026. Without this filter, the debug APK
        // duplicates ~25 MB of native libs across four ABIs (arm64-v8a,
        // armeabi-v7a, x86, x86_64).
        //
        // Trade-off: x86_64 emulators on Windows/Linux PCs won't run
        // this build. Mac Apple-Silicon emulators are arm64 and work
        // fine. To enable x86_64 emulator runs locally, temporarily add
        // "x86_64" to this list — keep arm64-v8a-only for any APK you
        // distribute.
        ndk { abiFilters += "arm64-v8a" }

        // Strip non-English translations from AppCompat / Material /
        // Google Play Services. Each removed locale is ~30-100 KB; the
        // cumulative savings is ~3 MB. `localeFilters` is AGP 8.9+; on
        // 8.7.x the equivalent is `resourceConfigurations`.
        resourceConfigurations += listOf("en")

        val baseUrl = (project.findProperty("bh.baseUrl") as String?) ?: "https://www.bughunter.co.in"
        buildConfigField("String", "DEFAULT_BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        debug {
            // R8 shrinking on debug — see proguard-rules-debug.pro for why.
            // Cuts ~60 MB of dead code (Compose Material Icons Extended
            // alone is ~30 MB of unused classes) while keeping readable
            // stacktraces. The shared release-rules file at the end of
            // the list provides keep rules for Moshi DTOs, Retrofit,
            // Hilt, etc. so the shrinker doesn't strip reflective hits.
            isMinifyEnabled = true
            // Leave resource shrinking off in debug — every Preview
            // resource and test layout would have to be opt-in kept.
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
                "proguard-rules-debug.pro",
            )
            buildConfigField("boolean", "HTTP_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "HTTP_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Strip duplicated / non-runtime files. License files,
            // Kotlin module metadata for libs we don't reflect on, and
            // every other "informational" META-INF that just costs disk.
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/DEPENDENCIES",
                "/META-INF/*.kotlin_module",
                "/META-INF/*.version",
                "/META-INF/proguard/**",
                "/META-INF/com.android.tools/**",
                "/META-INF/maven/**",
                "/kotlin/**",
                "/kotlin-tooling-metadata.json",
                "DebugProbesKt.bin",
                "**/*.kotlin_builtins",
                "**/*.kotlin_metadata",
            )
        }
        // Don't compress native libraries; the OS mmap-loads them
        // straight out of the APK either way, so leaving them
        // uncompressed avoids a second copy on disk after install.
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  Kover — Kotlin code coverage for the JVM unit-test suite.
// ──────────────────────────────────────────────────────────────────────────
//  `koverXmlReportDebug` runs the debug unit tests and writes a JaCoCo-format
//  XML to build/reports/kover/reportDebug.xml that SonarQube ingests.
//
//  The excludes below drop code that JVM unit tests structurally cannot
//  cover — @Composable UI, generated Hilt/Moshi/Dagger glue, the
//  Application / Activity entry points and BuildConfig — so the coverage
//  number reflects the logic we CAN unit-test (ViewModels, repositories,
//  interceptors, mappers, use-cases). It mirrors sonar.coverage.exclusions
//  in the root build so the Kover report and Sonar agree on the denominator.
// ──────────────────────────────────────────────────────────────────────────
kover {
    reports {
        filters {
            excludes {
                // @Composable UI compiles to `<File>Kt` classes — exclude
                // the screen / composable-helper files by class suffix.
                // (Kover's annotatedBy("…Composable") proved unreliable for
                // top-level @Composable functions, so we match by name.)
                classes(
                    "*ScreenKt",
                    "*ComposableKt",
                    "*ComposableSingletons*",
                    // Compose UI by naming convention (all verified @Composable).
                    "*DialogKt",
                    "*PanelKt",
                    "*OverlayKt",
                    "*FabKt",
                    "*SectionKt",
                    "*BottomSheetKt",
                    "*RowKt",
                    "*CardKt",
                    "*PickerKt",
                    "*RendererKt",
                    "*PromptKt",
                    "*BannerKt",
                    "*ComposerKt",
                    "*FooterKt",
                    // Hilt-generated entry-point shims.
                    "*.Hilt_*",
                    "Hilt_*",
                    "com.bughunter.BuildConfig",
                    "com.bughunter.MainActivity",
                    "com.bughunter.MainActivity*",
                    "com.bughunter.*Application",
                    "com.bughunter.*Application*",
                    // Generated DI / serialization / codegen glue.
                    "*_Factory",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*_GeneratedInjector",
                    "*_Impl",
                    "*JsonAdapter",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.*",
                    // Android-framework-coupled: these write to the device via
                    // a @ApplicationContext ContentResolver / MediaStore, which
                    // JVM unit tests cannot exercise without Robolectric. Covered
                    // by instrumented tests instead.
                    "com.bughunter.core.domain.usecase.ExportBugsCsvUseCase",
                    "com.bughunter.core.domain.usecase.ExportBugsCsvUseCase*",
                    "com.bughunter.core.domain.usecase.UploadAttachmentsUseCase",
                    "com.bughunter.core.domain.usecase.UploadAttachmentsUseCase*",
                    // Hilt DI providers (network + Moshi) + EncryptedSharedPreferences.
                    "com.bughunter.core.network.NetworkModule",
                    "com.bughunter.core.network.NetworkModule*",
                    "com.bughunter.core.network.MoshiModule",
                    "com.bughunter.core.network.MoshiModule*",
                    "com.bughunter.core.data.local.EncryptedPrefs",
                    "com.bughunter.core.data.local.EncryptedPrefs*",
                )
                // UI-only packages: Compose screens, design tokens, the
                // app-shell navigation, chart + chat-bubble renderers, and
                // the DI graph. Exercised by instrumented tests, not JVM.
                packages(
                    "com.bughunter.core.ui.theme",
                    "com.bughunter.core.ui.components",
                    "com.bughunter.core.nav",
                    "com.bughunter.di",
                    "com.bughunter.feature.analytics.charts",
                    "com.bughunter.feature.chatbot.blocks",
                    "com.bughunter.feature.chatbot.messages",
                    "com.bughunter.feature.chatbot.tabs",
                    // ContentResolver/MediaStore file-export ViewModels — same
                    // @ApplicationContext constraint as the use-cases above;
                    // not unit-testable on the JVM without Robolectric.
                    "com.bughunter.feature.audit",
                    "com.bughunter.feature.dsar",
                    // Push stack: FCM service, registration worker, token
                    // syncer, channel registration — all Android-framework.
                    "com.bughunter.core.push",
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.bundles.network)
    ksp(libs.moshi.codegen)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    implementation(libs.coil.compose)

    implementation(libs.play.services.base)

    implementation(libs.zxing.core)

    testImplementation(libs.bundles.test.unit)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.test.android)
}
