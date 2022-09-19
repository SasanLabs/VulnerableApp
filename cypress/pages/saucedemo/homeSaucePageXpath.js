class homeSaucePage{

    elements = {
        usernameInput: () => cy.xpath(`//input[@data-test='username']`),
        passwordInput: () => cy.xpath(`//*[@data-test='password' or @id='password']`),
        loginBtn: () => cy.xpath(`//*[@value='Login']`),
        errorMessage: () => cy.xpath('//h3')
    }

    typeUsername(username){
        this.elements.usernameInput().type(username);
    }

    typePassword(password){
        this.elements.passwordInput().type(password);
    }

    clickLogin(){
        this.elements.loginBtn().click();
    }


}

module.exports = new homeSaucePage();