//https://docs.cypress.io/api/cypress-api/dom#Syntax

//Cypress.dom.method() is a collection of DOM related helper methods.

describe('4 Commands Probably You Did NOT Know', () => {

    it('isVisible DEMO', () => {

        cy.visit('https://demoqa.com/accordian')

        cy.get('.collapse').eq(6).then(($element)=>{
            cy.log(`Collapse content as soon as the website is loaded:  ${Cypress.dom.isVisible($element)}`)
        })

        cy.get('#section1Heading').click();

        cy.get('.collapse').eq(6).then(($element)=>{
            cy.log(`Collapse content as soon as I click on the card:  ${Cypress.dom.isVisible($element)}`)
        })


    });
    
});