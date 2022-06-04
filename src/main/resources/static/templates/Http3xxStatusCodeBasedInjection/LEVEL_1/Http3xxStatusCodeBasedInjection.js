function updatePlaceholderDiv() {
  let placeholderAnchorElement = document.getElementById("placeholder");
  console.log(placeholderAnchorElement);
  let url = window.location.href;
  if (url.endsWith(":9090/VulnerableApp/")){
    placeholderAnchorElement.href =
        getUrlForVulnerabilityLevel() + "?returnTo=/VulnerableApp/";
  } else if (url.endsWith("localhost/") || url.endsWith("localhost")){
    placeholderAnchorElement.href =
        getUrlForVulnerabilityLevel() + "?returnTo=/";
  }
  placeholderAnchorElement.innerText = "Click here";
}

updatePlaceholderDiv();
