@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
// 注意：Kotlin DSL 中 `java` 会被 JavaPluginExtension 遮蔽，禁止写 java.time.* 全限定名，必须 import
import java.time.LocalDate
import java.util.Properties as JavaProperties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// ──────────────────────────────────────────────────────────────
// 动态版本号：versionName = 月.日.迭代（如 8.6.0）
//            versionCode = yy*1000000 + MM*10000 + dd*100 + 迭代（跨年仍单调递增）
//
// 日期在每次构建时实时获取。项目开启了 configuration-cache，配置阶段直接调
// LocalDate.now() 会被缓存固化导致跨天不刷新，因此必须包进 ValueSource：
// Gradle 每次构建都会重新执行 obtain()，返回值变化即自动作废配置缓存。
//
// 迭代数存于根目录 version.properties：跨天自动归零；
// 同日再发包执行 ./gradlew bumpIteration，或临时用 -PverIter=N 覆盖。
// ──────────────────────────────────────────────────────────────
abstract class AppVersionValueSource : ValueSource<String, AppVersionValueSource.Params> {
    interface Params : ValueSourceParameters {
        val propsPath: Property<String>
        val iterationOverride: Property<String>
    }

    override fun obtain(): String {
        val today = LocalDate.now()
        val file = File(parameters.propsPath.get())
        val props = JavaProperties()
        if (file.exists()) file.inputStream().use { props.load(it) }
        // 记录日期与今天不一致 → 说明是新的一天，迭代数归零
        val saved = if (props.getProperty("date")?.trim() == today.toString()) {
            props.getProperty("iteration")?.trim()?.toIntOrNull() ?: 0
        } else 0
        val iteration = parameters.iterationOverride.orNull?.trim()?.toIntOrNull() ?: saved
        val name = "${today.monthValue}.${today.dayOfMonth}.$iteration"
        val code = (today.year % 100) * 1_000_000 +
                today.monthValue * 10_000 +
                today.dayOfMonth * 100 +
                iteration
        return "$name|$code"
    }
}

private val appVersionRaw = providers.of(AppVersionValueSource::class) {
    parameters.propsPath.set(rootProject.file("version.properties").absolutePath)
    parameters.iterationOverride.set(providers.gradleProperty("verIter"))
}.get()
val dynamicVersionName: String = appVersionRaw.substringBefore('|')
val dynamicVersionCode: Int = appVersionRaw.substringAfter('|').toInt()

tasks.register("bumpIteration") {
    group = "versioning"
    description = "同一天再次发包时把迭代数 +1（如 8.6.0 → 8.6.1）"
    val propsFile = rootProject.file("version.properties")
    doLast {
        val today = LocalDate.now()
        val props = JavaProperties()
        if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }
        val cur = if (props.getProperty("date")?.trim() == today.toString()) {
            props.getProperty("iteration")?.trim()?.toIntOrNull() ?: 0
        } else -1
        val next = cur + 1
        // 手写而非 Properties.store()，后者会抹掉文件里的说明注释
        propsFile.writeText(
            buildString {
                appendLine("# Pvz2Tool 版本迭代记录")
                appendLine("# 版本号规则：versionName = 月.日.迭代   versionCode = yy*1000000 + MM*10000 + dd*100 + 迭代")
                appendLine("# 日期由构建时实时获取，无需手工维护；跨天后迭代数自动归零。")
                appendLine("# 同一天需要发第二个包时执行：./gradlew bumpIteration")
                appendLine("# 也可临时覆盖：./gradlew assembleRelease -PverIter=3")
                appendLine("date=$today")
                appendLine("iteration=$next")
            }
        )
        println("版本号已更新为 ${today.monthValue}.${today.dayOfMonth}.$next")
    }
}

tasks.register("printVersion") {
    group = "versioning"
    description = "打印当前构建将使用的版本号"
    val n = dynamicVersionName
    val c = dynamicVersionCode
    doLast { println("versionName=$n  versionCode=$c") }
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
            implementation(libs.yukireflection.api)
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
            implementation(libs.hiddenapibypass)

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
            implementation(libs.arsclib)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kaml)
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
        // 版本号 = 当前月.日.迭代次数（如 8 月 6 日第 0 次迭代 → 8.6.0），构建时按系统日期动态生成
        versionCode = dynamicVersionCode
        versionName = dynamicVersionName
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


// 合并引擎离线验证需要较大堆（目标游戏 APK 可能数百 MB，.so/dex 会被整块读入内存）
tasks.named<Test>("jvmTest") {
    maxHeapSize = "4g"
}

dependencies {
    implementation(libs.androidx.appcompat)
    // 注意：刻意不引入 com.google.android.material:material 与 androidx.constraintlayout。
    // 二者原先仅由 material 以「传递依赖」形式进入产物（本项目源码/XML 未直接使用 Material 或
    // ConstraintLayout；唯一引用是平台内置主题 @android:style/Theme.Material.Light.NoActionBar）。
    // 工具箱 dex 一旦自带 constraintlayout，会与目标游戏 APK 自带的那份在 INSERT_BEFORE 下抢先加载、
    // 资源 id 不匹配导致登录界面异常，因此合并引擎才需要剥离。直接从依赖根消除它，比运行时剥离更干净，
    // ToolboxApkMerger 的 STRIP_DEX_PACKAGE_PREFIXES 兜底逻辑保留以应对「目标 APK 自带 CL」的场景。
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    debugImplementation(libs.compose.uiTooling)
}
