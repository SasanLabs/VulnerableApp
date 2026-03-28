
describe('Button Actions (regression)', { tags: '@regressionTag' },() => {

    beforeEach(() => {
        cy.visit('https://demoqa.com/buttons')
    })

    it('Double Click', () => {

        cy.get('#doubleClickBtn').dblclick()
        cy.get('#doubleClickMessage').should('have.text', 'You have done a double click')

    });

    it('Right Click', () => {

        cy.get('#rightClickBtn').rightclick()
        cy.get('#rightClickMessage').should('have.text', 'You have done a right click')

    });
});
