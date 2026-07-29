package nl.avisi.structurizr.site.generatr.site.model

import nl.avisi.structurizr.site.generatr.site.GeneratorContext

class HeaderBarViewModel(pageViewModel: PageViewModel, generatorContext: GeneratorContext) {
    val url = pageViewModel.url
    val logo = logoPath(generatorContext)?.let { ImageViewModel(pageViewModel, "/$it") }
    val hasLogo = logo != null
    val titleLink = LinkViewModel(pageViewModel, generatorContext.workspace.name, HomePageViewModel.url())
    val searchLink = LinkViewModel(pageViewModel, generatorContext.workspace.name, SearchViewModel.url())
    val branches = generatorContext.branches
        .map { BranchHomeLinkViewModel(pageViewModel, it) }
    val tags = generatorContext.tags
        .map { BranchHomeLinkViewModel(pageViewModel, it) }
    val currentBranch = generatorContext.currentBranch
    val version = generatorContext.version
    val showVersion = version.isNotBlank()
    val allowToggleTheme = pageViewModel.allowToggleTheme

    /**
     * When set, the switcher entries are omitted from the page and filled at runtime from
     * versions.json by version-switcher.js. See GeneratorContext.clientSideVersionSwitcher.
     */
    val clientSideVersionSwitcher = generatorContext.clientSideVersionSwitcher

    private fun logoPath(generatorContext: GeneratorContext) =
        generatorContext.workspace.views.configuration.properties
            .getOrDefault(
                "generatr.style.logoPath",
                null
            )
}
