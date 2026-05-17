function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  
  placeholderAnchorElement.href =
    getUrlForVulnerabilityLevel() + "?returnTo=/VulnerableApp/templates/Phishing/fake-login.html";

  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
