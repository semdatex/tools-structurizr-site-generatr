@file:OptIn(ExperimentalCli::class)

package nl.avisi.structurizr.site.generatr

import kotlinx.cli.*
import nl.avisi.structurizr.site.generatr.site.*
import java.io.File

class GenerateSiteCommand : Subcommand(
    "generate-site",
    "Generate a site for the selected workspace."
) {
    private val gitUrl by option(
        ArgType.String, "git-url", "g",
        "The URL of the Git repository which contains the Structurizr model. " +
            "If a Git repository is provided, it will be cloned and" +
            "--workspace-file and --assets-dir will be treated as paths within the cloned repository. " +
            "If no Git repository is provided, --workspace-file and --assets-dir will be used as-is, and the site" +
            "will only contain one branch, named after the --default-branch option."
    )
    private val gitUsername by option(
        ArgType.String, "git-username", "u",
        "Username for the Git repository"
    )
    private val gitPassword by option(
        ArgType.String, "git-password", "p",
        "Password for the Git repository"
    )
    private val workspaceFile by option(
        ArgType.String, "workspace-file", "w",
        "Relative path within the Git repository of the workspace file"
    ).required()
    private val assetsDir by option(
        ArgType.String, "assets-dir", "a",
        "Relative path within the Git repository where static assets are located"
    )
    private val branches by option(
        ArgType.String, "branches", "b",
        "Comma-separated list of branches to include in the generated site. Not used if '--all-branches' option is set to true"
    ).default("master")
    private val defaultBranch by option(
        ArgType.String, "default-branch", "d",
        "The default branch"
    ).default("master")
    private val version by option(
        ArgType.String, "version", "v",
        "The version of the site, shown as-is in the branch switcher. When omitted, no version is shown."
    ).default("")
    private val outputDir by option(
        ArgType.String, "output-dir", "o",
        "Directory where the generated site will be stored. Will be created if it doesn't exist yet."
    ).default("build/site")

    private val allBranches by option(
        ArgType.Boolean, "all-branches", "all",
        "When set, generate a site for every branch in the git repository"
    ).default(value = false)
    private val excludeBranches by option(
        ArgType.String, "exclude-branches", "ex",
        "Comma-separated list of branches to exclude from the generated site"
    ).default("")
    private val tags by option(
        ArgType.String, "tags", "t",
        "Comma-separated list of tags to include in the generated site. Not used if '--all-tags' option is set to true"
    ).default("")
    private val allTags by option(
        ArgType.Boolean, "all-tags", "alltags",
        "When set, generate a site for every tag in the git repository"
    ).default(value = false)
    private val excludeTags by option(
        ArgType.String, "exclude-tags", "extags",
        "Comma-separated list of tags to exclude from the generated site"
    ).default("")

    override fun execute() {
        val siteDir = File(outputDir).apply { mkdirs() }
        val gitUrl = gitUrl

        generateRedirectingIndexPage(siteDir, defaultBranch)
        copySiteWideAssets(siteDir)

        if (gitUrl != null)
            generateSiteForModelInGitRepository(gitUrl, siteDir)
        else
            generateSiteForModel(siteDir)
    }

    private fun generateSiteForModelInGitRepository(gitUrl: String, siteDir: File) {
        val cloneDir = File("build/model-clone")
        val clonedRepository = ClonedRepository(cloneDir, gitUrl, gitUsername, gitPassword).apply {
            refreshLocalClone()
        }

        val branchNames = if (allBranches)
            clonedRepository.getBranchNames(excludeBranches.split(","))
        else
            branches.split(",")

        println("The following branches will be checked for Structurizr Workspaces: $branchNames")

        val workspaceFileInRepo = File(clonedRepository.cloneDir, workspaceFile)
        val branchesToGenerate = branchNames.filter { branch ->
            println("Checking branch $branch")
            try {
                clonedRepository.checkoutBranch(branch)
                createStructurizrWorkspace(workspaceFileInRepo)
                true
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                println("Bad Branch $branch: $errorMessage")
                false
            }
        }.sortedWith(branchComparator(defaultBranch))

        println("The following branches contain a valid Structurizr workspace: $branchesToGenerate")

        if (!branchesToGenerate.contains(defaultBranch)) {
            throw Exception("$defaultBranch does not contain a valid structurizr workspace. Site generation halted.")
        }

        val tagsToGenerate = tagsToGenerate(clonedRepository, workspaceFileInRepo, branchesToGenerate)

        branchesToGenerate.forEach { branch ->
            println("Generating site for branch $branch")
            clonedRepository.checkoutBranch(branch)
            generateSiteForCheckout(clonedRepository, branchesToGenerate, tagsToGenerate, branch, siteDir)
        }

        tagsToGenerate.forEach { tag ->
            println("Generating site for tag $tag")
            clonedRepository.checkoutTag(tag)
            generateSiteForCheckout(clonedRepository, branchesToGenerate, tagsToGenerate, tag, siteDir)
        }
    }

    private fun tagsToGenerate(
        clonedRepository: ClonedRepository,
        workspaceFileInRepo: File,
        branchesToGenerate: List<String>
    ): List<String> {
        val tagNames = if (allTags)
            clonedRepository.getTagNames(excludeTags.split(","))
        else
            tags.split(",").filter { it.isNotBlank() }

        if (tagNames.isEmpty()) return emptyList()

        println("The following tags will be checked for Structurizr Workspaces: $tagNames")

        return tagNames.filter { tag ->
            println("Checking tag $tag")
            try {
                clonedRepository.checkoutTag(tag)
                createStructurizrWorkspace(workspaceFileInRepo)
                true
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                println("Bad Tag $tag: $errorMessage")
                false
            }
        }.filter { tag ->
            (tag !in branchesToGenerate).also { noCollision ->
                if (!noCollision)
                    println("Skipping tag $tag: a branch with the same name is already included in the site")
            }
        }.sortedWith(tagComparator())
            .also { println("The following tags contain a valid Structurizr workspace: $it") }
    }

    private fun generateSiteForCheckout(
        clonedRepository: ClonedRepository,
        branchesToGenerate: List<String>,
        tagsToGenerate: List<String>,
        refName: String,
        siteDir: File
    ) {
        val workspace = createStructurizrWorkspace(File(clonedRepository.cloneDir, workspaceFile))
        writeStructurizrJson(workspace, File(siteDir, refName))
        generateDiagrams(workspace, File(siteDir, refName))
        generateSite(
            version,
            workspace,
            assetsDir?.let { File(clonedRepository.cloneDir, it) },
            siteDir,
            branchesToGenerate,
            refName,
            tags = tagsToGenerate
        )
    }

    private fun generateSiteForModel(siteDir: File) {
        val workspace = createStructurizrWorkspace(File(workspaceFile))
        writeStructurizrJson(workspace, File(siteDir, defaultBranch))
        generateDiagrams(workspace, File(siteDir, defaultBranch))
        generateSite(
            version,
            workspace,
            assetsDir?.let { File(it) },
            siteDir,
            listOf(defaultBranch),
            defaultBranch
        )
    }
}

fun branchComparator(defaultBranch: String) = Comparator<String> { a, b ->
    if (a == defaultBranch) -1
    else if (b == defaultBranch) 1
    else a.compareTo(b, ignoreCase = true)
}

/**
 * Sorts tags in descending natural order (newest release first for version-like
 * tags): numeric segments are compared as numbers, so "v1.10.0" sorts above "v1.9.0".
 */
fun tagComparator() = Comparator<String> { a, b -> naturalCompare(b, a) }

private val naturalOrderChunkPattern = Regex("""\d+|\D+""")

private fun naturalCompare(a: String, b: String): Int {
    val chunksA = naturalOrderChunkPattern.findAll(a).map { it.value }.toList()
    val chunksB = naturalOrderChunkPattern.findAll(b).map { it.value }.toList()

    chunksA.zip(chunksB).forEach { (chunkA, chunkB) ->
        val result = if (chunkA.first().isDigit() && chunkB.first().isDigit())
            chunkA.toBigInteger().compareTo(chunkB.toBigInteger())
        else
            chunkA.compareTo(chunkB, ignoreCase = true)
        if (result != 0) return result
    }

    return chunksA.size.compareTo(chunksB.size)
}
