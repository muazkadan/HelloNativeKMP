package dev.muazkadan.hellonative.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.util.Properties

/**
 * Provides the [CMakeBuild] task type and points it at a CMake executable, resolved from
 * (in order) the `cmake.executable` Gradle property, `PATH`, or the newest CMake in the Android SDK.
 * The SDK fallback matters when Gradle is started from an IDE that doesn't inherit the shell `PATH`.
 */
class CMakePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cmake = project.resolveCMakeExecutable()
        project.tasks.withType<CMakeBuild>().configureEach {
            cmakeExecutable.convention(cmake)
        }
    }

    private fun Project.resolveCMakeExecutable(): Provider<String> {
        val executableName = if (currentOs == "windows") "cmake.exe" else "cmake"

        val fromPath = providers.environmentVariable("PATH").map { path ->
            path.split(File.pathSeparator)
                .map { File(it, executableName) }
                .firstOrNull(File::canExecute)
                ?.absolutePath
                .orEmpty()
        }.filter(String::isNotEmpty)

        val fromAndroidSdk = androidSdkDir().map { sdk ->
            File(sdk, "cmake").listFiles()
                ?.filter { File(it, "bin/$executableName").canExecute() }
                ?.maxByOrNull { VersionKey(it.name) }
                ?.let { File(it, "bin/$executableName").absolutePath }
                .orEmpty()
        }.filter(String::isNotEmpty)

        return providers.gradleProperty("cmake.executable")
            .orElse(fromPath)
            .orElse(fromAndroidSdk)
            .orElse(executableName)
    }

    private fun Project.androidSdkDir(): Provider<String> {
        val localProperties = rootProject.layout.projectDirectory.file("local.properties")
        val fromLocalProperties = providers.fileContents(localProperties).asText.map { text ->
            Properties().apply { load(text.reader()) }.getProperty("sdk.dir").orEmpty()
        }.filter(String::isNotEmpty)
        return fromLocalProperties.orElse(providers.environmentVariable("ANDROID_HOME"))
    }

    private class VersionKey(name: String) : Comparable<VersionKey> {
        private val parts = name.split('.', '-').map { it.toIntOrNull() ?: 0 }

        override fun compareTo(other: VersionKey): Int {
            for (i in 0 until maxOf(parts.size, other.parts.size)) {
                val diff = parts.getOrElse(i) { 0 } - other.parts.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }
    }
}
