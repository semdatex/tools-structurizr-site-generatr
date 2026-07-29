package nl.avisi.structurizr.site.generatr.site

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlin.test.Test

class VersionsJsonTest {
    @Test
    fun `writes branches and tags in the order given`() {
        val dir = createTempDir()

        writeVersionsJson(dir, "main", listOf("main", "branch-2"), listOf("v2.0.0", "v1.0.0"))

        assertThat(File(dir, "versions.json").readText()).isEqualTo(
            """{"defaultBranch":"main","branches":["main","branch-2"],"tags":["v2.0.0","v1.0.0"]}"""
        )
    }

    @Test
    fun `writes an empty tag array when there are no tags`() {
        val dir = createTempDir()

        writeVersionsJson(dir, "main", listOf("main"), emptyList())

        assertThat(File(dir, "versions.json").readText()).isEqualTo(
            """{"defaultBranch":"main","branches":["main"],"tags":[]}"""
        )
    }

    @Test
    fun `escapes characters that would break the JSON`() {
        val dir = createTempDir()

        writeVersionsJson(dir, """we"ird""", listOf("""we"ird"""), listOf("""back\slash"""))

        assertThat(File(dir, "versions.json").readText()).isEqualTo(
            """{"defaultBranch":"we\"ird","branches":["we\"ird"],"tags":["back\\slash"]}"""
        )
    }

    @Test
    fun `overwrites a versions file from an earlier run`() {
        val dir = createTempDir()
        writeVersionsJson(dir, "main", listOf("main"), listOf("v1.0.0"))

        writeVersionsJson(dir, "main", listOf("main"), listOf("v2.0.0", "v1.0.0"))

        assertThat(File(dir, "versions.json").readText()).isEqualTo(
            """{"defaultBranch":"main","branches":["main"],"tags":["v2.0.0","v1.0.0"]}"""
        )
    }

    private fun createTempDir(): File =
        File.createTempFile("versions-json-test", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
}
