//Data Driven Testing using data from a JSON file.

const tests = require('../../fixtures/data-driven/sauceUsers.json');

describe('Data Driven Test reading data from a JSON file', function(){

    beforeEach(function(){
        cy.visit('https://www.saucedemo.com/');
    });

    tests.forEach(test => {

        it(test.name, function(){

            cy.get('[data-test="username"]').type(test.username);
            cy.get('[data-test="password"]').type(test.password);
            cy.get('[data-test="login-button"]').click();

            if(test.name === 'Standard User'){
                cy.get('.title').should('contain.text', test.expected);
            }else{
                cy.get('[data-test="error"]').should('contain.text', test.expected);    
            }
    
        });

    });
});