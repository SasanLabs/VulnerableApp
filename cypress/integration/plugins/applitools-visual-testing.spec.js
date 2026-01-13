//Steps to install the plugin
//1- npm install @applitools/eyes-cypress@3 --save-dev
//2- npx eyes-setup

//How it works https://applitools.com/tutorials/cypress.html#applitools-eyes-cypress-sdk

describe('Demo - Applitools', () => {
    
    it('Sign In Page Validation', () => {
        
        cy.visit('https://www.saucedemo.com/')

        cy.eyesOpen({
            appName: 'App Demo',
            batchName: 'Batch Name #1',
            browser: [
                {deviceName: 'iPhone X', screenOrientation: 'portrait'},
                {name: 'chrome', width: 1024, height: 768}
            ]
        })


        cy.eyesCheckWindow('Sign in page')

        cy.eyesClose();


    });

});