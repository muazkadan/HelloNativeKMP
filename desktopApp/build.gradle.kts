import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

// The JNI library built by :composeApp, laid out as <os>-<arch>/libnative_greeting_jni.*
val desktopNativeLibs = configurations.dependencyScope("desktopNativeLibs")
val desktopNativeLibsDir = configurations.resolvable("desktopNativeLibsDir") {
    extendsFrom(desktopNativeLibs.get())
    attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named("desktop-native-libs")) }
}

dependencies {
    implementation(projects.composeApp)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    desktopNativeLibs(projects.composeApp)
}

compose.desktop {
    application {
        mainClass = "dev.muazkadan.hellonative.MainKt"
        // JNI library loading is a restricted method since JDK 24 (JEP 472).
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.muazkadan.hellonative"
            packageVersion = "1.0.0"
            // Exposed at runtime through the compose.application.resources.dir system property.
            appResourcesRootDir.set(layout.dir(desktopNativeLibsDir.map { it.singleFile }))
        }
    }
}

// Registered by the Compose plugin after evaluation, so match it lazily by name.
tasks.named { it == "prepareAppResources" }.configureEach {
    dependsOn(desktopNativeLibsDir)
}
