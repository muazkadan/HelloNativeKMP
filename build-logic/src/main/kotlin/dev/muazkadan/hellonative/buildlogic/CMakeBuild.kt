package dev.muazkadan.hellonative.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
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

/** Configures, builds and installs a CMake project in Release mode. */
@DisableCachingByDefault(because = "Outputs depend on the local native toolchain")
abstract class CMakeBuild @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {

    /** Directory containing the top-level `CMakeLists.txt`. */
    @get:Internal
    abstract val sourceDir: DirectoryProperty

    /** Files that affect the build; used only for up-to-date checks. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val cmakeExecutable: Property<String>

    /** Extra `-D` cache entries and other configure-time arguments. */
    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Input
    abstract val environment: MapProperty<String, String>

    @get:LocalState
    abstract val buildDir: DirectoryProperty

    /** Install prefix; receives everything the project's `install()` rules produce. */
    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun build() {
        // Always configure from scratch: toolchain variables can't change in an existing cache.
        fileSystemOperations.delete { delete(buildDir, installDir) }

        val build = buildDir.get().asFile.absolutePath
        cmake("-S", sourceDir.get().asFile.absolutePath, "-B", build, "-DCMAKE_BUILD_TYPE=Release", *arguments.get().toTypedArray())
        cmake("--build", build, "--config", "Release", "--parallel")
        cmake("--install", build, "--config", "Release", "--prefix", installDir.get().asFile.absolutePath)
    }

    private fun cmake(vararg args: String) {
        execOperations.exec {
            executable = cmakeExecutable.get()
            args(*args)
            environment(this@CMakeBuild.environment.get())
        }
    }
}
