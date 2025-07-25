import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val projectName = "NotiBot"
val apkFileName = "$projectName.apk"
val currentBuildTime: String = SimpleDateFormat("yy/MM/dd HH:mm:ss").format(Date())
val currentVersionDate: Int = SimpleDateFormat("yyMMdd").format(Date()).toInt()
val currentVersion: String = SimpleDateFormat("yy.MM.dd").format(Date())
val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
val date: String = dateFormat.format(Date())

android {
    namespace = "la.shiro.notibot"
    compileSdk = 36

    defaultConfig {
        applicationId = "la.shiro.notibot"
        minSdk = 30
        targetSdk = 36
        versionCode = currentVersionDate
        versionName = currentVersion
        androidResources.localeFilters.add("en")
        androidResources.localeFilters.add("zh-rCN")
        buildConfigField("long", "VERSION_CODE", "$currentVersionDate")
        buildConfigField("String", "BUILD_TIME", "\"$currentBuildTime\"")
        buildConfigField("String", "APP_NAME", "\"$projectName\"")
    }

    signingConfigs {
        val localProperties = Properties()
        localProperties.load(project.rootProject.file("local.properties").inputStream())
        getByName("debug") {
            keyAlias = localProperties.getProperty("keyAlias")
            keyPassword = localProperties.getProperty("keyPassword")
            storeFile = file("moe.jks")
            storePassword = localProperties.getProperty("keyPassword")
        }
        create("release") {
            keyAlias = localProperties.getProperty("keyAlias")
            keyPassword = localProperties.getProperty("keyPassword")
            storeFile = file("moe.jks")
            storePassword = localProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
            isShrinkResources = false
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            isShrinkResources = false
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
        buildConfig = true
        viewBinding = true
    }

    dependenciesInfo {
        includeInApk = false
    }

    applicationVariants.all {
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                outputFileName = apkFileName
            }
        }
    }
}

allprojects {
    gradle.projectsEvaluated {
        tasks.register<Zip>("zipReleaseApkAndAssets") {
            val apkFile = file("release/$apkFileName")
            val outputDir = file("dist")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            from(apkFile) {
                into(projectName)
            }
            from("etc") {
                into(projectName)
            }

            archiveFileName.set("${projectName}_${date}.zip")
            destinationDirectory.set(outputDir)

            doLast {
                println("ZIP file created at: ${outputDir.absolutePath}/${archiveFileName.get()}")
            }
        }
        tasks.register<Zip>("zipDebugSymbols") {
            val mappingFile = file("build/outputs/mapping/release/mapping.txt")
            val outputDir = file("dist")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            from(mappingFile) {
                into("DebugSymbols")
            }
            archiveFileName.set("${projectName}_${date}_Symbols.zip")
            destinationDirectory.set(outputDir)
            doLast {
                println("Symbols file created at: ${outputDir.absolutePath}/${archiveFileName.get()}")
            }
        }

        tasks.getByName("assembleRelease").finalizedBy("zipReleaseApkAndAssets")
        tasks.getByName("zipReleaseApkAndAssets").finalizedBy("zipDebugSymbols")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}