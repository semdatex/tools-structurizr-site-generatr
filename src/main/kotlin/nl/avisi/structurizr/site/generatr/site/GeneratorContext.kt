package nl.avisi.structurizr.site.generatr.site

import com.structurizr.Workspace

data class GeneratorContext(
    val version: String,
    val workspace: Workspace,
    val branches: List<String>,
    val currentBranch: String,
    val serving: Boolean,
    val svgFactory: (key: String, url: String) -> String?,
    val legendSvgFactory: (key: String) -> String? = { null },
    val tags: List<String> = emptyList(),
    /**
     * When set, pages do not embed the branch/tag list; the version switcher is filled at
     * runtime from versions.json in the site root. This makes a rendered version's pages
     * independent of which *other* versions the site happens to carry, so they can be reused
     * (e.g. from a build cache) after the version list changed.
     */
    val clientSideVersionSwitcher: Boolean = false
)
