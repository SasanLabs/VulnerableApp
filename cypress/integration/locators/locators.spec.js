let username = "standard_user";
let password = "secret_sauce";

describe('Locators in Cypress', function(){

    beforeEach(function(){
        cy.visit('https://www.saucedemo.com/');
    });

    it('GET Method', function(){
        cy.get('#user-name').type(username);
        cy.get('input#password').type(password);
        cy.get('[data-test="login-button"]').click();
    });

    it('EQ|FIRST|LAST Method',  function(){
        cy.get('input').first().type(username);
        cy.get('input').eq(1).type(password);
        cy.get('input').last().click();
    });

    it('FILTER Method',function(){
        cy.get('input').filter('[type="text"]').type(username);
        cy.get('input').filter('[type="password"]').type(password);
        cy.get('input').filter('[type="submit"]').click();
        
    })

    it('FIND Method', function(){
        cy.get('form').find('input').eq(0).type(username);
        cy.get('form').find('input').eq(1).type(password);
        cy.get('form').find('input').eq(2).click();
    })

    it('PARENT Method', function(){
        cy.get('form').parent().should('have.class','login-box');
    })

});