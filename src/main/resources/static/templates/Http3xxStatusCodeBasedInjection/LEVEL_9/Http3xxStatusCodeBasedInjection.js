function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
<<<<<<< HEAD

  placeholderAnchorElement.href =
    getUrlForVulnerabilityLevel() +
    "?returnTo=/VulnerableApp/templates/Phishing/fake-login.html";
=======
  
  placeholderAnchorElement.href =
    getUrlForVulnerabilityLevel() + "?returnTo=/VulnerableApp/phishing/fake-login.html";
>>>>>>> develop

  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
