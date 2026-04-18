////Website REPO: https://github.com/filiphric/testing-lists

//-> https://www.cypress.io/blog/2017/11/07/add-gui-to-your-e2e-api-tests/

//-> The intention of cy.request() is to be used for checking endpoints on an actual, running server without having to start the front end application. 

// -> These are tests that do not modify the server state.

describe('Basic API Testing - Part #1', () => {

  beforeEach(function() {
    cy.request('GET','http://localhost:3000/todos').as('getTodos')
  });

  it('Body Length - Test', () => {

    cy.request('http://localhost:3000/todos')
    .its('body')
    .should('have.length', 2)
    
  });

  it('Request Status - Test', () => {
    cy.request('http://localhost:3000/todos')
    .its('status')
    .should('eq', 200)
  });

  it('header/content type - test', () => {

    cy.request('http://localhost:3000/todos')
    .its('headers')
    .its('content-type')
    .should('include', 'application/json')
    .should('include', 'charset=utf-8')
    
  });

  const apiItems = [
    {
      "title": "test",
      "completed": true,
      "id": "1"
    },
    {
      "title": "wash dishes",
      "completed": false,
      "id": "2"
    }
  ]

  it('Loading of initial items - test', () => {
    
    cy.request('http://localhost:3000/todos')
    .its('body')
    .should('deep.eq', apiItems)

  });

  it('JSON Schema Validation', () => {
    cy.request('http://localhost:3000/todos')
    .its('body')
    .each(object => {
      expect(object).to.have.all.keys('title', 'completed', 'id')
    })
  });

  it('Using Alias Request', function(){

    cy.get('@getTodos').should(response => {
      expect(response.body).to.have.length(2)
      expect(response).to.have.property('headers')
      expect(response).to.have.property('duration')
    })

  })


});

