//https://docs.cypress.io/api/cypress-api/browser#Log-browser-information

describe('4 Commands Probably You Did NOT Know', () => {

    beforeEach(() => {
        cy.visit('https://www.whatismybrowser.com/es/')
    });

    it('Log Web Browser Information', () => {
       cy.log(Cypress.browser.name)
       cy.log(Cypress.browser.family)
       cy.log(Cypress.browser.isHeaded)
       cy.log(Cypress.browser.isHeadless)
       cy.log(Cypress.browser.path)
       cy.log(Cypress.browser.version)
    });

    it('Check text depending on the browser', () => {
        if(Cypress.browser.name==='chrome'){
            cy.get('div[class="string-major"] a').should('have.text','Chrome 91 on Windows 10')
        }else if(Cypress.browser.name === 'firefox'){
            cy.get('div[class="string-major"] a').should('have.text','Firefox 90 on Windows 10')
        }
    });
    
});