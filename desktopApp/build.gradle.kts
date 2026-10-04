import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.composeApp)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("java.library.path", rootProject.file("native/build/desktop").absolutePath)
}

compose.desktop {
    application {
        mainClass = "dev.muazkadan.hellonative.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.muazkadan.hellonative"
            packageVersion = "1.0.0"
        }
    }
}
