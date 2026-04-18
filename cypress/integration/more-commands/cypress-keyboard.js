describe('4 Commands Probably You Did NOT Know', () => {

    //Cypress KEYBOARD
    //The Keyboard API allows you set the default values for how
    //the .type() command is executed.
    //Change the keystrokeDelay
    //https://docs.cypress.io/api/cypress-api/keyboard-api

    /*
    You can specify this as a global change at Cypress/Support/Index.js
        Cypress.Keyboard.defaults({
            keystrokeDelay: 20,
        })
    */

    it('Cypress Keyboard - Slow Type', { keystrokeDelay: 100 }, () => {

        cy.visit('https://www.saucedemo.com/')

        cy.get('#user-name').type('user name test')

        
    });

    it('Cypress Keyboard - Standard', { keystrokeDelay: 10 }, () => {
        //You can specify this as a global change at Cypress/Support/Index.js

        cy.visit('https://www.saucedemo.com/')

        cy.get('#user-name').type('user name test')

        
    });

    it('Cypress Keyboard - Fast Type', { keystrokeDelay: 0 }, () => {
        //You can specify this as a global change at Cypress/Support/Index.js

        cy.visit('https://www.saucedemo.com/')

        cy.get('#user-name').type('user name test')

        
    });
    
});