const TestFilters = (givenTags, runTest) => {
    if (Cypress.env('tags')) {
      const tags = Cypress.env('tags').split(',')
      const isFound = givenTags.some((givenTag) => tags.includes(givenTag))
 
      if (isFound) {
        runTest()
      }
    } else {
      runTest()
    }
  };

  export default TestFilters