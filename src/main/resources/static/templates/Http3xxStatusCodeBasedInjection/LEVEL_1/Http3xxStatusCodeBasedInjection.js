function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  placeholderAnchorElement.href =
    getUrlForVulnerabilityLevel() + "?returnTo=/VulnerableApp";
  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
