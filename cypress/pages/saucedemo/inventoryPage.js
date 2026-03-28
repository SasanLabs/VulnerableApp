class inventoryPage {
    elements = {
        titleSpan: () => cy.get('.title')
    }
}

module.exports = new inventoryPage();