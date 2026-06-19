//Cypress is built on top of Mocha and Chai. And Cypress support chai's BDD and TDD assertion styles.
//The interface is borrowed from Mocha and we use describe, context, it, and specify

//-- Dummy aplication code
let add = (a,b) => a+b;
let subtract = (a,b) => a-b;
let divide = (a,b) => a/b;
let multiply = (a,b) => a*b;

//-- Cypress Test

//Describe -> Simply a way to group our tests in Mocha. We can nest our test in groups as deep as we need. It takes to arguments,
//the first is the name and then the call back function.

//!Context is just an alias for describe() and behaves the same way.


describe('Unit testing for our dummy application', () =>{

    //It -> Used for an individual test cases. Receives two parameters, one the name as string explaining what the test will do, 
    //and the second one is the call back function

    //Specify is also an alias of it, and we can use it in the same way.

    context('math with POSITIVE numbers', () => {

        it('Should add Numbers', ()=>{
            expect(add(1,2)).to.eq(3);
        });

        it('Should Subtract Numbers', ()=>{
            expect(subtract(2,1)).to.eq(1);
        });

        it('Should Multiply Numbers', ()=>{
            expect(multiply(2,2)).to.eq(4);
        });

        it('Should Divide Numbers', ()=>{
            expect(divide(4,2)).to.eq(2);
        });
    });

    context('math with DECIMAL numbers', () => {

        it('Should Add Numbers', ()=>{
            expect(add(2.2,2.2)).to.eq(4.4);
        });

        it('Should Subtract Numbers', ()=>{
            expect(subtract(4.4, 2.2)).to.eq(2.2);
        });
    });

});