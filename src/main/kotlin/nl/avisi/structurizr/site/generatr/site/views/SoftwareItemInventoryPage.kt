package nl.avisi.structurizr.site.generatr.site.views

import kotlinx.html.HTML
import kotlinx.html.h2
import nl.avisi.structurizr.site.generatr.site.model.SoftwareItemInventoryPageViewModel

fun HTML.softwareItemInventoryPage(viewModel: SoftwareItemInventoryPageViewModel) {
    page(viewModel = viewModel) {
        contentDiv {
            h2 { +viewModel.pageSubTitle }
            table(viewModel.inventoryTable)
        }
    }
}
