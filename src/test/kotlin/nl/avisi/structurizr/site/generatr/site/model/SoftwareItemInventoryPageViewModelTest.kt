package nl.avisi.structurizr.site.generatr.site.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import nl.avisi.structurizr.site.generatr.site.GeneratorContext
import org.junit.jupiter.api.Test

class SoftwareItemInventoryPageViewModelTest : ViewModelTest() {

    @Test
    fun `disabled when no selector property is configured`() {
        val generatorContext = generatorContext()

        assertThat(SoftwareItemInventoryPageViewModel.enabled(generatorContext.workspace)).isFalse()
    }

    @Test
    fun `enabled when selector property is configured`() {
        val generatorContext = inventoryGeneratorContext()

        assertThat(SoftwareItemInventoryPageViewModel.enabled(generatorContext.workspace)).isTrue()
    }

    @Test
    fun `default title and configured title`() {
        val generatorContext = inventoryGeneratorContext()
        assertThat(SoftwareItemInventoryPageViewModel.title(generatorContext.workspace))
            .isEqualTo("Software Item Inventory")

        generatorContext.workspace.views.configuration.addProperty(
            "generatr.site.inventory.title", "Software Items (IEC 62304)"
        )
        assertThat(SoftwareItemInventoryPageViewModel.title(generatorContext.workspace))
            .isEqualTo("Software Items (IEC 62304)")
    }

    @Test
    fun `columns default to the selector property`() {
        val generatorContext = inventoryGeneratorContext()

        assertThat(SoftwareItemInventoryPageViewModel.columns(generatorContext.workspace)).containsExactly(
            SoftwareItemInventoryPageViewModel.InventoryColumn("itemId", "itemId")
        )
    }

    @Test
    fun `columns are parsed from configuration with optional labels`() {
        val generatorContext = inventoryGeneratorContext().apply {
            workspace.views.configuration.addProperty(
                "generatr.site.inventory.columns",
                "itemId|Item ID, classification|Classification, safetyClass"
            )
        }

        assertThat(SoftwareItemInventoryPageViewModel.columns(generatorContext.workspace)).containsExactly(
            SoftwareItemInventoryPageViewModel.InventoryColumn("itemId", "Item ID"),
            SoftwareItemInventoryPageViewModel.InventoryColumn("classification", "Classification"),
            SoftwareItemInventoryPageViewModel.InventoryColumn("safetyClass", "safetyClass")
        )
    }

    @Test
    fun `lists only elements carrying the selector property, sorted by its value`() {
        val generatorContext = inventoryGeneratorContext()
        val system = generatorContext.workspace.model.addSoftwareSystem("MoRe Care System").apply {
            addProperty("itemId", "SYS-001")
        }
        val container = system.addContainer("Orchestrator").apply {
            addProperty("itemId", "BE-201")
            addProperty("classification", "HSW-ESS")
        }
        container.addComponent("Journal Service") // no selector property, must not appear
        system.addContainer("Unclassified DB") // no selector property, must not appear

        val viewModel = SoftwareItemInventoryPageViewModel(generatorContext)

        assertThat(
            viewModel.inventoryTable.bodyRows.map { row ->
                row.columns.filterIsInstance<TableViewModel.TextCellViewModel>().last().title
            }
        ).containsExactly("BE-201", "SYS-001")
    }

    @Test
    fun `renders configured property columns and a placeholder for missing values`() {
        val generatorContext = inventoryGeneratorContext().apply {
            workspace.views.configuration.addProperty(
                "generatr.site.inventory.columns",
                "itemId|Item ID,classification|Classification"
            )
        }
        val system = generatorContext.workspace.model.addSoftwareSystem("MoRe Care System")
        system.addContainer("Orchestrator").apply {
            addProperty("itemId", "BE-201")
            addProperty("classification", "HSW-ESS")
        }
        system.addContainer("Report Generator").apply {
            addProperty("itemId", "BE-501")
        }

        val viewModel = SoftwareItemInventoryPageViewModel(generatorContext)

        val row1 = viewModel.inventoryTable.bodyRows[0].columns
        val row2 = viewModel.inventoryTable.bodyRows[1].columns
        assertThat((row1[3] as TableViewModel.TextCellViewModel).title).isEqualTo("BE-201")
        assertThat((row1[4] as TableViewModel.TextCellViewModel).title).isEqualTo("HSW-ESS")
        assertThat((row2[3] as TableViewModel.TextCellViewModel).title).isEqualTo("BE-501")
        assertThat((row2[4] as TableViewModel.TextCellViewModel).title).isEqualTo("—")
        assertThat((row2[4] as TableViewModel.TextCellViewModel).greyText).isTrue()
    }

    @Test
    fun `container and component rows link to the container components page`() {
        val generatorContext = inventoryGeneratorContext()
        val system = generatorContext.workspace.model.addSoftwareSystem("MoRe Care System")
        val container = system.addContainer("Web Application").apply {
            addProperty("itemId", "FE-000")
        }
        container.addComponent("Threshold Administration Module").apply {
            addProperty("itemId", "FE-201")
        }

        val viewModel = SoftwareItemInventoryPageViewModel(generatorContext)

        val componentRow = viewModel.inventoryTable.bodyRows[1].columns
        val containerRow = viewModel.inventoryTable.bodyRows[0].columns
        assertThat((componentRow[0] as TableViewModel.LinkCellViewModel).link.href)
            .isEqualTo("/more-care-system/component/web-application")
        assertThat((componentRow[1] as TableViewModel.TextCellViewModel).title).isEqualTo("Component")
        assertThat((componentRow[2] as TableViewModel.TextCellViewModel).title)
            .isEqualTo("MoRe Care System / Web Application")
        assertThat((containerRow[0] as TableViewModel.LinkCellViewModel).link.href)
            .isEqualTo("/more-care-system/component/web-application")
        assertThat((containerRow[1] as TableViewModel.TextCellViewModel).title).isEqualTo("Container")
    }

    @Test
    fun `external software systems are rendered without a link`() {
        val generatorContext = inventoryGeneratorContext().apply {
            workspace.views.configuration.addProperty("generatr.site.externalTag", "External System")
        }
        generatorContext.workspace.model.addSoftwareSystem("Keycloak").apply {
            addTags("External System")
            addProperty("itemId", "BE-701")
        }

        val viewModel = SoftwareItemInventoryPageViewModel(generatorContext)

        val row = viewModel.inventoryTable.bodyRows.single().columns
        assertThat((row[0] as TableViewModel.TextCellViewModel).title).isEqualTo("Keycloak (External)")
    }

    private fun inventoryGeneratorContext(): GeneratorContext = generatorContext().apply {
        workspace.views.configuration.addProperty("generatr.site.inventory.selectorProperty", "itemId")
    }
}
