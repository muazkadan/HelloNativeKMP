package dev.muazkadan.hellonative.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Builds one package of a Cargo workspace in release mode for one or more Rust targets, then
 * copies the library built for each target into its own subdirectory of [outputDir].
 */
@DisableCachingByDefault(because = "Outputs depend on the local Rust toolchain")
abstract class CargoBuild @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {

    /** Directory containing the workspace `Cargo.toml`. Cargo also reads `.cargo/config.toml` from here. */
    @get:Internal
    abstract val workspaceDir: DirectoryProperty

    /** Files that affect the build; used only for up-to-date checks. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val cargoExecutable: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    /** File name of the library Cargo produces, for example `libnative_greeting.a`. */
    @get:Input
    abstract val libraryFileName: Property<String>

    /** Maps each Rust target triple to the subdirectory of [outputDir] that receives its library. */
    @get:Input
    abstract val targets: MapProperty<String, String>

    @get:Input
    abstract val environment: MapProperty<String, String>

    /** Cargo's target directory, kept between runs so Cargo can build incrementally. */
    @get:LocalState
    abstract val targetDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun build() {
        val targets = targets.get()
        val targetDir = targetDir.get()

        execOperations.exec {
            workingDir = workspaceDir.get().asFile
            executable = cargoExecutable.get()
            args("build", "--release", "--locked", "--package", packageName.get(), "--target-dir", targetDir.asFile.absolutePath)
            targets.keys.forEach { args("--target", it) }
            environment(this@CargoBuild.environment.get())
        }

        fileSystemOperations.sync {
            into(outputDir)
            targets.forEach { (target, subdirectory) ->
                from(targetDir.file("$target/release/${libraryFileName.get()}")) { into(subdirectory) }
            }
        }
    }
}
