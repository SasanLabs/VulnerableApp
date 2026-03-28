describe('Variables & Aliases DEMO', () => {

    beforeEach(function(){
        cy.visit('https://demoqa.com/modal-dialogs')
    })

    it('Return Variables Misconception', () => {

        //You cannot assign or work with the return values of any Cypress command. Commands are enqueued and run asynchronously.

        const smallModalText = cy.get('#showSmallModal').text();

        cy.log(smallModalText)

    });

    it('CLOSURES & VARIABLES', () => {

        //If you're familiar with native Promises the Cypress .then() works the same way. You can continue to nest more Cypress commands inside of the .then().

        //Each nested command has access to the work done in previous commands. 

        cy.get('#showSmallModal').then($modalButon => {
            
            //By using callback functions we've created a closure. Closures enable us to keep references around to refer to work done in previous commands.
            const smallModalText = $modalButon.text();
            cy.log(smallModalText)

            $modalButon.click();

            cy.get('#example-modal-sizes-title-sm').contains(smallModalText, {matchCase: false})

            //This rule is when you are dealing with mutable objects (that change state).
            // When things change state you often want to compare an object's previous value to the next value.

            //The commands outside of the .then() will not run until all of the nested commands finish.

        })
        
    });

    it('Context Misconception', () =>{
        cy.log(smallModalText)
    })

    //Sharing context is the simplest way to use aliases.

    //To alias something you'd like to share use the .as() command.
    it('ALIASES', function() {

        cy.get('#showSmallModal').invoke('text').as('invoqueText')

        cy.get('#showSmallModal').then($modalButon => {
            
            //By using callback functions we've created a closure. Closures enable us to keep references around to refer to work done in previous commands.
            const smallModalText = $modalButon.text();
            cy.log(smallModalText)

            cy.wrap(smallModalText).as('wrapText')

        })
    });

    it('Sharing Context',  function() {
        cy.log(this.invoqueText)
        cy.log(this.wrapText)
        
    });
    
});