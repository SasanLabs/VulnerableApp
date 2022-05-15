function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  placeholderAnchorElement.href =
    getUrlForVulnerabilityLevel() + "?returnTo=/index.html";
  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
