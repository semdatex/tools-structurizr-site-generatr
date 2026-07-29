package nl.avisi.structurizr.site.generatr

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File
import kotlin.test.Test

class TagSiteExistsTest {
    @Test
    fun `true when the tag directory contains a rendered home page`() {
        val siteDir = createTempSiteDir()
        File(siteDir, "v1.0.0").apply { mkdirs() }.resolve("index.html").writeText("<html></html>")

        assertThat(tagSiteExists(siteDir, "v1.0.0")).isTrue()
    }

    @Test
    fun `false when the tag directory is missing`() {
        val siteDir = createTempSiteDir()

        assertThat(tagSiteExists(siteDir, "v1.0.0")).isFalse()
    }

    @Test
    fun `false when the tag directory exists but has no home page`() {
        val siteDir = createTempSiteDir()
        File(siteDir, "v1.0.0").mkdirs()

        assertThat(tagSiteExists(siteDir, "v1.0.0")).isFalse()
    }

    @Test
    fun `false when the home page is a directory instead of a file`() {
        val siteDir = createTempSiteDir()
        File(siteDir, "v1.0.0/index.html").mkdirs()

        assertThat(tagSiteExists(siteDir, "v1.0.0")).isFalse()
    }

    private fun createTempSiteDir(): File =
        File.createTempFile("tag-site-exists-test", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
}
