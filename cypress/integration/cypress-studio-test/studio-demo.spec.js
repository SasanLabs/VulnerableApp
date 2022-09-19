/* === Test Created with Cypress Studio === */
it('Web-tables-demo', function() {
  /* ==== Generated with Cypress Studio ==== */
  cy.visit('https://demoqa.com/webtables');
  cy.get('#addNewRecordButton').click();
  cy.get('#firstName').clear();
  cy.get('#firstName').type('Joan');
  cy.get('#lastName').clear();
  cy.get('#lastName').type('Test');
  cy.get('#userEmail').clear();
  cy.get('#userEmail').type('joan@test.com');
  cy.get('#age').clear();
  cy.get('#age').type('50');
  cy.get('#salary').clear();
  cy.get('#salary').type('5000');
  cy.get('#department').clear();
  cy.get('#department').type('Sales');
  /* ==== End Cypress Studio ==== */
  /* ==== Generated with Cypress Studio ==== */
  cy.get('#submit').click();
  /* ==== End Cypress Studio ==== */
});