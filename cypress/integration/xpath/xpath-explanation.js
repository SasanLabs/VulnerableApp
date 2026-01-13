// To install Xpath Plugin -> npm install -D cypress-xpath
// Include the following command at support/index.js require('cypress-xpath')

//Xpath -> XPath is defined as XML path. It is a syntax or
//language for finding any element on the web page using the
//XML path expression. XPath is used to find the location of
//any element on a webpage using HTML DOM structure.

//Common Xpath Structure
/* //tagName[@attribute='value']  */
/*
   // -> Select Current Node
   Tagname -> Tagname of the particular node.
   @ -> Select attribute
   Attribute -> Attribute name of the node
   Value -> Value of the attribute    
*/

//Types of Xpath
/* 
    Absolute Xpath
    Absolute XPath is the direct way to find the element. 
    But the disadvantage of the absolute XPath is that if
    there are any changes made in the path of the element then that XPath fails.
    The key characteristic of XPath is that it begins with the single forward slash(/)
    ,which means you can select the element from the root node.

    Site: https://www.saucedemo.com/
    Example: /html/body/div/div/div/div/div/div/form/div/input

    Relativa Xpath
    For Relative XPath, the path starts from the middle of the HTML DOM structure
    . It starts with the double forward slash (//), which means it can search the
    element anywhere at the webpage.
    
    Example: //div[@class='login-box']/form/div/input[@id='user-name']

    //Dynamic Xpath Strategies
    1- Basic one: XPath expression selects nodes or lists of nodes on the basis of
    attributes like ID, name, classname, etc. from the XML document as illustrated below.
    //input[@name='user-name']

    2- Contains: The contain feature has an ability to find the element with partial text
     as shown in the below example. 
    //*[contains(@placeholder, 'User')] 

    3- Using OR & AND: In OR expression, two conditions are used, 
    whether the first condition OR second condition should be true. 
    //*[@data-test='username' or @data-test='password']

    In AND expression, two conditions are used. Both conditions
    should be true to find the element. It fails to find the element
    if any one condition is false.
    //*[@data-test='username' and @name='user-name']

    4- Starts-With Function: 
    Starts-with function finds the element whose attribute value changes on refresh or
    any operation on the webpage. In this expression, match the starting text of the 
    attribute used to find the element whose attribute changes dynamically. You can also 
    find the element whose attribute value is static (does not change).

    For example, suppose the ID of a particular element changes dynamically like:
    Id=" message12"
    Id=" message345"    
    Id=" message8769"
    And so on. But the initial text is same. In this case, we use Start-with expression “message.”
    //input[starts-with(@class,'input')]

    5- Text(): In this expression, with text function, we find the element with the exact text match
    as shown below. In this case, we find the element with text "Username or email."

    // //*[text()='Password for all users:']
    
    6- Using Index: This approach comes in use when you wish to specify a given tag name in terms of
    the index value you wish to locate. 
    
    (//input[starts-with(@class,'input')])[1]

*/