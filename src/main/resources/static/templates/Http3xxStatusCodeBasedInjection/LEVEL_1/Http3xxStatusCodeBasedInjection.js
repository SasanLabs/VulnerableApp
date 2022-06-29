function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  let url = window.location.href;
  if (url.endsWith("/VulnerableApp/")) {
    placeholderAnchorElement.href =
      getUrlForVulnerabilityLevel() + "?returnTo=/VulnerableApp/";
  } else {
    placeholderAnchorElement.href =
      getUrlForVulnerabilityLevel() + "?returnTo=/";
  }
  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
