function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  placeholderAnchorElement.href = getUrlForVulnerabilityLevel() + "?returnTo=/";
  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
