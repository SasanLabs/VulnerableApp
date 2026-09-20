function redirectUrlFor(destinationName) {
  return (
    getUrlForVulnerabilityLevel() +
    "?returnTo=" +
    encodeURIComponent(destinationName)
  );
}

function updatePlaceholderDiv() {
  let input = document.getElementById("returnToInput");
  let testLink = document.getElementById("testRedirectBtn");

  let updateTestLink = function () {
    testLink.href = redirectUrlFor(input.value);
  };
  input.addEventListener("input", updateTestLink);
  updateTestLink();

  document.querySelectorAll(".sample-link").forEach(function (link) {
    link.href = redirectUrlFor(link.getAttribute("data-value"));
  });
}

updatePlaceholderDiv();
