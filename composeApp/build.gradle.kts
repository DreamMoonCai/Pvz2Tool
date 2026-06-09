@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm().mainRun {
        mainClass.set("io.github.dreammooncai.pvz2tool.pop.MainKt")
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.yukireflection.api.kotlin)
            implementation(libs.kaml)
            implementation(libs.androidx.documentfile)
            implementation(libs.android.floatingx)
            implementation(libs.android.floatingx.compose)
            implementation(libs.kotlin.gadulka)
            implementation(libs.coil.gif)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            // Ktor 网络客户端
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.io.semver.version)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.androidx.material.icons.core)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.kotlinx.serialization)
            implementation(libs.keight)
            implementation(libs.bcprov.jdk18on)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
            compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            compilerOptions.freeCompilerArgs.add("-Xexplicit-backing-fields")
            compilerOptions.freeCompilerArgs.add("-Xallow-contracts-on-more-functions")
        }
    }
}

android {
    namespace = "io.github.dreammooncai.pvz2tool"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.dreammooncai.pvz2tool"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
    buildTypes {
        all {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id","--package-id","0x66")
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

// ============================================================
// 删除所有 dimen 变体（兼容 configuration cache）
// 原理：AAPT2 link 时读取 incremental/*.txt 文件列表，
//       同时删掉 .flat 文件 + 从 .txt 列表中移除对应条目，
//       AAPT2 才不会把这些变体写入 arsc。
// ============================================================
run {
    // 需要从 arsc 中移除的变体目录前缀（文件名格式: values-xxx_values-xxx.arsc.flat）
    val dimenVariantPrefixes = listOf(
        "values-h", "values-w", "values-sw",
        "values-large", "values-xlarge", "values-small", "values-normal",
        "values-port", "values-land", "values-watch"
    )
    // 在配置阶段捕获 Provider（configuration cache 安全）
    val buildDirProvider = layout.buildDirectory

    afterEvaluate {
        tasks.matching { it.name.startsWith("process") && it.name.endsWith("Resources") }.configureEach {
            doFirst {
                val root = buildDirProvider.get().asFile

                // 1. 删除 merged_res 下的 flat 文件
                root.walkTopDown()
                    .filter { file ->
                        file.isFile &&
                        file.name.endsWith(".arsc.flat") &&
                                dimenVariantPrefixes.any { prefix -> file.name.startsWith(prefix) }
                    }
                    .forEach { flatFile ->
                        println("[DimenStrip] Delete flat: ${flatFile.name}")
                        flatFile.delete()
                    }

                // 2. 修改 incremental/process*Resources/*.txt，移除变体路径条目
                //    AAPT2 从此 txt 读取 flat 文件列表，必须同步移除
                root.walkTopDown()
                    .filter { file ->
                        file.isFile &&
                        file.name.startsWith("resources-list-for-") &&
                        file.name.endsWith(".txt")
                    }
                    .forEach { listFile ->
                        val original = listFile.readText()
                        // txt 内容是空格分隔的路径列表
                        val filtered = original
                            .split(" ")
                            .filter { path ->
                                val fileName = path.substringAfterLast("/").substringAfterLast("\\")
                                !dimenVariantPrefixes.any { prefix -> fileName.startsWith(prefix) }
                            }
                            .joinToString(" ")
                        if (filtered != original) {
                            listFile.writeText(filtered)
                            println("[DimenStrip] Patched list: ${listFile.name}")
                        }
                    }
            }
        }
    }
}


dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    debugImplementation(libs.compose.uiTooling)
}
