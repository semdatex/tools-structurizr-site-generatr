package nl.avisi.structurizr.site.generatr.site.model

import com.structurizr.Workspace
import com.structurizr.model.Component
import com.structurizr.model.Container
import com.structurizr.model.SoftwareSystem
import com.structurizr.model.StaticStructureElement
import nl.avisi.structurizr.site.generatr.hasComponentDiagrams
import nl.avisi.structurizr.site.generatr.hasImageViews
import nl.avisi.structurizr.site.generatr.includedProperties
import nl.avisi.structurizr.site.generatr.normalize
import nl.avisi.structurizr.site.generatr.site.GeneratorContext

/**
 * A workspace-wide inventory of all elements (software systems, containers and components) that
 * carry a configurable selector property. Intended for use cases like a software item inventory
 * with safety classifications (e.g. IEC 62304), where every model element representing a software
 * item is marked with an identity property and the inventory renders one table row per item.
 *
 * The page is enabled by setting `generatr.site.inventory.selectorProperty` in the workspace view
 * configuration. Columns are configured through `generatr.site.inventory.columns`.
 */
class SoftwareItemInventoryPageViewModel(generatorContext: GeneratorContext) : PageViewModel(generatorContext) {
    override val url = url()
    override val pageSubTitle = title(generatorContext.workspace)

    val inventoryTable: TableViewModel = TableViewModel.create {
        val workspace = generatorContext.workspace
        val selectorProperty = checkNotNull(selectorProperty(workspace)) {
            "$SELECTOR_PROPERTY must be set in the workspace view configuration to generate the inventory page"
        }
        val columns = columns(workspace)

        headerRow(
            headerCellMedium("Element"),
            headerCell("Type"),
            headerCell("Location"),
            *columns.map { headerCell(it.label) }.toTypedArray()
        )

        inventoryElements(workspace, selectorProperty)
            .forEach { element ->
                bodyRow(
                    elementCell(element),
                    cell(element.typeName),
                    cell(element.location),
                    *columns.map { column -> propertyCell(element, column.key) }.toTypedArray()
                )
            }
    }

    private fun inventoryElements(workspace: Workspace, selectorProperty: String) =
        workspace.model.softwareSystems
            .sortedBy { it.name.lowercase() }
            .flatMap { system ->
                listOf<StaticStructureElement>(system) + system.containers
                    .sortedBy { it.name.lowercase() }
                    .flatMap { container ->
                        listOf<StaticStructureElement>(container) + container.components.sortedBy { it.name.lowercase() }
                    }
            }
            .filter { it.properties.containsKey(selectorProperty) }
            .sortedBy { it.properties.getValue(selectorProperty).lowercase() }

    private fun TableViewModel.TableViewInitializerContext.elementCell(element: StaticStructureElement) =
        when (element) {
            is SoftwareSystem -> softwareSystemCell(this@SoftwareItemInventoryPageViewModel, element)
            is Container -> linkedElementCell(element.softwareSystem, element, element.name)
            is Component -> linkedElementCell(element.container.softwareSystem, element.container, element.name)
            else -> cell(element.name)
        }

    private fun TableViewModel.TableViewInitializerContext.linkedElementCell(
        system: SoftwareSystem,
        container: Container,
        title: String
    ) = if (includedSoftwareSystems.contains(system) && container.hasComponentsPage(generatorContext.workspace))
        cellWithLink(
            this@SoftwareItemInventoryPageViewModel,
            title,
            "${SoftwareSystemPageViewModel.url(system, SoftwareSystemPageViewModel.Tab.COMPONENT)}/${container.name.normalize()}"
        )
    else
        cell(title)

    private fun TableViewModel.TableViewInitializerContext.propertyCell(
        element: StaticStructureElement,
        propertyKey: String
    ) = element.properties[propertyKey]
        ?.let { cell(it) }
        ?: cell("—", greyText = true)

    private fun Container.hasComponentsPage(workspace: Workspace) =
        workspace.hasComponentDiagrams(this) || includedProperties.isNotEmpty() || workspace.hasImageViews(id)

    private val StaticStructureElement.typeName
        get() = when (this) {
            is SoftwareSystem -> "Software System"
            is Container -> "Container"
            is Component -> "Component"
            else -> "Element"
        }

    private val StaticStructureElement.location
        get() = when (this) {
            is Container -> softwareSystem.name
            is Component -> "${container.softwareSystem.name} / ${container.name}"
            else -> ""
        }

    data class InventoryColumn(val key: String, val label: String)

    companion object {
        private const val SELECTOR_PROPERTY = "generatr.site.inventory.selectorProperty"
        private const val TITLE_PROPERTY = "generatr.site.inventory.title"
        private const val COLUMNS_PROPERTY = "generatr.site.inventory.columns"

        fun url() = "/software-items"

        fun enabled(workspace: Workspace) = selectorProperty(workspace) != null

        fun title(workspace: Workspace): String = workspace.views.configuration.properties
            .getOrDefault(TITLE_PROPERTY, "Software Item Inventory")

        fun selectorProperty(workspace: Workspace): String? = workspace.views.configuration.properties
            .get(SELECTOR_PROPERTY)?.takeIf { it.isNotBlank() }

        /**
         * Parses `generatr.site.inventory.columns`: a comma-separated list of `propertyKey|Column Label`
         * entries; the label is optional and defaults to the property key. When the property is not
         * configured, the selector property is rendered as the single value column.
         */
        fun columns(workspace: Workspace): List<InventoryColumn> = workspace.views.configuration.properties
            .getOrDefault(COLUMNS_PROPERTY, "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { spec ->
                val key = spec.substringBefore("|").trim()
                val label = spec.substringAfter("|", key).trim().ifEmpty { key }
                InventoryColumn(key, label)
            }
            .ifEmpty {
                selectorProperty(workspace)?.let { listOf(InventoryColumn(it, it)) }.orEmpty()
            }
    }
}
