//https://docs.cypress.io/api/cypress-api/arch

describe('4 commands you did not know', () => {


//Cypress.arch returns you the CPU architecture name of the underlying OS, as returned from Node's os.arch().

    it('Cypress.arch Demo', () => {
        cy.log(Cypress.arch)
    });

});