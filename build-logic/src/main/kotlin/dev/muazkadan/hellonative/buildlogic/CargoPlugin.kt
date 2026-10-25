package dev.muazkadan.hellonative.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.withType
import java.io.File

/**
 * Provides the [CargoBuild] task type and points it at a Cargo executable, resolved from
 * (in order) the `cargo.executable` Gradle property, `PATH`, `$CARGO_HOME/bin`, or `~/.cargo/bin`.
 * The fallbacks matter when Gradle is started from an IDE or an Xcode build phase, which often
 * don't inherit the shell `PATH`.
 */
class CargoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cargo = project.resolveCargoExecutable()
        project.tasks.withType<CargoBuild>().configureEach {
            cargoExecutable.convention(cargo)
        }
    }

    private fun Project.resolveCargoExecutable(): Provider<String> {
        val executableName = if (currentOs == "windows") "cargo.exe" else "cargo"

        fun Provider<String>.findIn(dirs: (String) -> List<String>): Provider<String> = map { value ->
            dirs(value)
                .map { File(it, executableName) }
                .firstOrNull(File::canExecute)
                ?.absolutePath
                .orEmpty()
        }.filter(String::isNotEmpty)

        val fromPath = providers.environmentVariable("PATH").findIn { it.split(File.pathSeparator) }
        val fromCargoHome = providers.environmentVariable("CARGO_HOME").findIn { listOf("$it/bin") }
        val fromUserHome = providers.systemProperty("user.home").findIn { listOf("$it/.cargo/bin") }

        return providers.gradleProperty("cargo.executable")
            .orElse(fromPath)
            .orElse(fromCargoHome)
            .orElse(fromUserHome)
            .orElse(executableName)
    }
}
