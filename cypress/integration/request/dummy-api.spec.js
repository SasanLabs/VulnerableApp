

const postRequest = (resource, endpoint) => {
    cy.request('POST', endpoint, resource)
}


describe('Scheduled Searches - Endpoint API Test - DEMO', function () {


    it('Create a scheduled Search', function () {

        cy.request('POST','test', ss)
        .should(response => { 
           expect(response.status).to.equal(200)
           expect(response.statusText).to.equal('OK')
           expect(response.headers['content-type']).to.include('application/json')
           expect(response.body).to.have.any.keys('id', 'type', 'active', 'name', 'range', 'schedule', 'userId', 'timezone', 'lastChangeDate','createdAt', 'data', 'type', 'start', 'end','occurrences', 'templateSearchId', 'overrideResults', 'dateTimeFormat')
           
           //This can be handled in a custom command.
           //Save the ID for further usage in a fixture file.
           cy.readFile("cypress/fixtures/api-demo/schedule-data.json", (err, data) => {
            if (err) {
                return console.error(err);
            };
            }).then((data) => {
                data.id = response.body["id"] //save the New Value of Unique ID
                cy.writeFile("cypress/fixtures/api-demo/schedule-data.json", JSON.stringify(data)) //Write it to the fixtures file
            })

            //End of the custom command.
           
        })
    })

    it.skip('Update a scheduled Search', ()=>{

        cy.fixture('api-demo/schedule-data').then(data=>{

            cy.request('PUT',`https://test/${data.id}`, ssUpdated)
            .should(response => { 
                expect(response.status).to.equal(200)
                expect(response.statusText).to.equal('OK')
                expect(response.headers['content-type']).to.include('application/json')
                expect(response.body["active"]).to.equal(false)
                expect(response.body["name"]).to.equal('Test1-Changed')
            })

        })

    })

    it('Delete a schheduled search', function(){
        cy.fixture('api-demo/schedule-data').then(data=>{

            cy.request('DELETE',`test/${data.id}`)
            .should(response => { 
                expect(response.status).to.equal(200)
                expect(response.statusText).to.equal('OK')
                expect(response.headers['content-type']).to.include('application/json')
                expect(response.body).to.equal(data.id)
            })

        })
        
    })

})



    
