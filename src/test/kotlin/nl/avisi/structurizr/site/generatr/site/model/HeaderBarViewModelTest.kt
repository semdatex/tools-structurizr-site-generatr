package nl.avisi.structurizr.site.generatr.site.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class HeaderBarViewModelTest : ViewModelTest() {
    private val generatorContext = generatorContext(
        "some workspace", branches = listOf("main", "branch-2"), version = "0.42.1337"
    )
    private val generatorContextWithTags = generatorContext(
        "some workspace", branches = listOf("main", "branch-2"), version = "0.42.1337",
        tags = listOf("v2.0.0", "v1.0.0")
    )
    private val pageViewModel = object : PageViewModel(generatorContext) {
        override val url: String = "/master/system"
        override val pageSubTitle: String = "subtitle"
    }

    @Test
    fun `workspace title link`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.titleLink).isEqualTo(
            LinkViewModel(pageViewModel, "some workspace", HomePageViewModel.url())
        )
    }

    @Test
    fun `current branch`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.currentBranch).isEqualTo(generatorContext.currentBranch)
    }

    @Test
    fun `branch pulldown menu`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.branches).containsExactly(
            BranchHomeLinkViewModel(pageViewModel, "main"),
            BranchHomeLinkViewModel(pageViewModel, "branch-2")
        )
    }

    @Test
    fun `tag pulldown menu`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContextWithTags)

        assertThat(viewModel.tags).containsExactly(
            BranchHomeLinkViewModel(pageViewModel, "v2.0.0"),
            BranchHomeLinkViewModel(pageViewModel, "v1.0.0")
        )
    }

    @Test
    fun `no tags by default`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.tags).isEmpty()
    }

    @Test
    fun `version number`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.version).isEqualTo("0.42.1337")
        assertThat(viewModel.showVersion).isTrue()
    }

    @Test
    fun `no version when blank`() {
        val contextWithoutVersion = generatorContext(
            "some workspace", branches = listOf("main", "branch-2"), version = ""
        )
        val viewModel = HeaderBarViewModel(pageViewModel, contextWithoutVersion)

        assertThat(viewModel.showVersion).isFalse()
    }

    @Test
    fun logo() {
        generatorContext.workspace.views.configuration.addProperty(
            "generatr.style.logoPath",
            "site/logo.png"
        )
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.hasLogo).isTrue()
        assertThat(viewModel.logo).isEqualTo(ImageViewModel(pageViewModel, "/site/logo.png"))
    }

    @Test
    fun `no logo`() {
        val viewModel = HeaderBarViewModel(pageViewModel, generatorContext)

        assertThat(viewModel.hasLogo).isFalse()
    }

    @TestFactory
    fun `no dark mode`() = listOf(
        "light" to false,
        "dark" to false,
        "auto" to true,
    ).map { (theme, allowToggle) ->
        DynamicTest.dynamicTest(theme) {
            generatorContext.workspace.views.configuration.addProperty(
                "generatr.site.theme",
                theme
            )

            val viewModel = HeaderBarViewModel(object : PageViewModel(generatorContext) {
                override val url: String = "/master/system"
                override val pageSubTitle: String = "subtitle"
            }, generatorContext)

            assertThat(viewModel.allowToggleTheme).isEqualTo(allowToggle)
        }
    }
}
