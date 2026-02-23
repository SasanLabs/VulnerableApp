import homeSaucePage from '../../pages/saucedemo/homeSaucePage'
import inventoryPage from '../../pages/saucedemo/inventoryPage'

const tests = require('../../fixtures/data-driven/sauceUsers.json');

describe('Home Sauce Demo', () => {

    beforeEach(function(){
        cy.visit('https://www.saucedemo.com/');
    });

    tests.forEach(test => {

        it(test.name, function(){

            homeSaucePage.typeUsername(test.username);
            homeSaucePage.typePassword(test.password);
            homeSaucePage.clickLogin();

            if(test.name === 'should login to inventory page'){
                inventoryPage.elements.titleSpan().should('have.text', test.expected)
            }else{
                homeSaucePage.elements.errorMessage().should('have.text', test.expected)  
            }
    
        });

    });
    
});