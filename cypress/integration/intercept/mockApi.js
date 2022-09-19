
//Mock API responses may be useful during development and testing when live data is either unavailable or unreliable.
// While designing an API, you can use mock APIs to work concurrently on the front and back-end, as well as to gather feedback from developers. 

//Website REPO: https://github.com/filiphric/testing-lists

describe('Intercept Demo', () => {

    it('Intitial Validation', () => {
        cy.visit('http://localhost:3000/')
        cy.get('#todo-list li')
        .should('have.length', 2)
        .and('contain', 'test')
        .and('contain', 'wash dishes')
    });

    it('Mocked API Response', ()=> {

        cy.intercept('GET', '/todos', {
            fixture: 'intercept/interceptFixture.json'
        }).as('getTodos-Fixture')

        cy.visit('http://localhost:3000/')

        cy.get('#todo-list li')
        .should('have.length', 3)
        .and('contain', 'Cypress Assertions Video')
        .and('contain', 'Page Object Model Cypress')
        .and('contain', 'Intercept Cypress')
    
    })

    it('Mocked a ready todo item', () => {
        
        const stubExample = [{
            "title": "Mock API Response", 
            "completed": true, 
            "id":1
        }]

        cy.intercept('GET', '/todos', {
            body: stubExample
        })

        cy.visit('http://localhost:3000/')

        cy.get('div label').should('have.css', 'text-decoration', 'line-through solid rgb(217, 217, 217)')

    });


    
});