//What is the retry ability in Cypress
//-> The retry-ability allows the tests to complete each command as soon as the assertion passes, 
//without hard-coding waits. If your application takes a few milliseconds or even seconds to render each DOM element - no big deal, 
//the test does not have to change at all.

describe('Retry-Ability Session', function(){

    beforeEach(function(){
        cy.visit('http://192.168.0.15:8888'); // Command

        //Get -> Get one or more DOM elements by selector or alias.
        cy.get('.new-todo')                 // command - Get one or more DOM elements by selector or alias.
          .type('todo A{enter}')            // command
          .type('todo B{enter}')            // command
    });

    //✅ If the assertion that follows the cy.get() command passes, then the command finishes successfully.
    //🚨 If the assertion that follows the cy.get() command fails, then the cy.get() command will requery the application’s DOM again. 
    //Then Cypress will try the assertion against the elements yielded from cy.get(). If the assertion still fails, cy.get() will try requerying the DOM again, 
    //and so on until the cy.get() command timeout is reached.

    //In the example, Within a few milliseconds after the DOM updates, cy.get() finds two elements and the should('have.length', 2) assertion passes.

    it.only('should create two items', () => {
        cy.get('.todo-list li')             // command
          .should('have.length', 2)         // assertion
    });


    //A single command followed by multiple assertions retries each one of them – in order. Thus when the first assertion passes, the command will be retried with 
    //first and second assertion. When the first and second assertion pass, then the command will be retried with the first, second, and third assertion, and so on.
    it('should match the content of the <li> elements', ()=>{
        cy.get('.todo-list li')     // command
        .should('have.length', 2) // assertion
        .and(($li) => {  //And is an alias of should 
            // 2 more assertions
            expect($li.get(0).textContent, 'first item').to.equal('todo A')
            expect($li.get(1).textContent, 'second item').to.equal('todo B')
        })
    });

    // Not every command is retried
    /* 
    --> Cypress only retries commands that query the DOM: cy.get(), .find(), .contains(), etc.
    --> Why are some commands NOT retried? : Commands are not retried when they could potentially change the state of the application under test. For example, 
        Cypress will not retry the .click() command, because it could change something in the application.
          --> ome commands that cannot be retried still have built-in waiting. For example, as described in the “Assertions” section of .click(), the click() command waits
                to click until the element becomes actionable.
                Cypress tries to act like a human user would using the browser.
                    * Can a user click on the element?
                    * Is the element invisible?
                    * Is the element behind another element?
                    * Does the element have the disabled attribute?
                The .click() command will automatically wait until multiple built-in assertion checks like these pass, and then it will attempt to click just once.
    */

    // Built-in assertions
    /* 
    Often a Cypress command has built-in assertions that will cause the command to be retried. For example, the .eq() command will be retried even without any
     attached assertions until it finds an element with the given index in the previously yielded list of elements.
    */
 
    it('should be a demonstration of a built in assertion', () =>{
        cy.get('.todo-list li')     // command
        .should('have.length', 2) // assertion
        .eq(1)                    // command -> Get A DOM element at a specific index in an array of elements.
    })

    //TimeOuts 
    // By default each command that retries, does so for up to 4 seconds - the defaultCommandTimeout setting.

    // cypress run --config defaultCommandTimeout=10000
    // cy.get('.mobile-nav', { timeout: 10000 })
    // Disable retry -> cy.get('#ssr-error', { timeout: 0 }).should('not.exist')

    //Recommendations to avoid flaky tests:
    //1-- to avoid unnecessarily splitting commands that query elements. In our case we first query elements using cy.get() and then query from that list of elements using .find(). 
    //     We can combine two separate queries into one - forcing the combined query to be retried.
    //2-- Whenever you write a longer test, we recommend alternating commands with assertions. 





});