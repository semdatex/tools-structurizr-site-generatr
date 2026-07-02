package nl.avisi.structurizr.site.generatr.site

import assertk.assertThat
import assertk.assertions.containsExactly
import nl.avisi.structurizr.site.generatr.tagComparator
import kotlin.test.Test

class TagComparatorTest {
    @Test
    fun `sorts descending so the newest release comes first`() {
        val tags = listOf("v1.0.0", "v2.0.0", "v1.5.0")

        assertThat(tags.sortedWith(tagComparator()))
            .containsExactly("v2.0.0", "v1.5.0", "v1.0.0")
    }

    @Test
    fun `compares numeric segments as numbers`() {
        val tags = listOf("v1.9.0", "v1.10.0", "v1.2.0")

        assertThat(tags.sortedWith(tagComparator()))
            .containsExactly("v1.10.0", "v1.9.0", "v1.2.0")
    }

    @Test
    fun `sorts tags without version numbers alphabetically descending`() {
        val tags = listOf("alpha", "gamma", "beta")

        assertThat(tags.sortedWith(tagComparator()))
            .containsExactly("gamma", "beta", "alpha")
    }

    @Test
    fun `shorter tag comes after longer tag with same prefix`() {
        val tags = listOf("v1.0", "v1.0.1")

        assertThat(tags.sortedWith(tagComparator()))
            .containsExactly("v1.0.1", "v1.0")
    }

    @Test
    fun `handles pre-release style suffixes`() {
        val tags = listOf("v1.0.0-rc1", "v1.0.0-rc2")

        assertThat(tags.sortedWith(tagComparator()))
            .containsExactly("v1.0.0-rc2", "v1.0.0-rc1")
    }
}
