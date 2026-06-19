
const endPoint = 'http://localhost:3000/todos'

const addTodo = item => {
    cy.request('POST', `${endPoint}`, item)
}

const deleteTodo = item => {
    cy.request('DELETE', `${endPoint}/${item.id}`)
}

const patchTodo = item => {
    cy.request('PATCH', `${endPoint}/${item.id}`, item)
}

const todoObject = {
    "title": "NewTodoFromPost",
    "completed": false,
    "id": "3"
}

const patchObject = {
    "title": "NewTodoFromPost",
    "completed": true,
    "id": "3"
}


describe('API DEMO', () => {

    it('Add a new todo', () => {
        
        addTodo(todoObject)

        cy.request('GET', `${endPoint}/${todoObject.id}`)
        .its('body')
        .should('deep.eq', todoObject)

    })

    it('Update a todo', () => {
        
        patchTodo(patchObject)

        cy.request('GET', `${endPoint}/${todoObject.id}`)
        .its('body')
        .should('deep.eq', patchObject)


    });

    it('Delete a todo', () => {
        deleteTodo(todoObject)

        cy.request('GET', endPoint)
        .its('body')
        .should('have.length', 2)

    });

    
});