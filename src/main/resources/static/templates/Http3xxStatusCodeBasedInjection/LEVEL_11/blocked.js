function getQueryParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

const blockedTarget = getQueryParam("target");
const targetDiv = document.getElementById("blockedTarget");

if (targetDiv) {
  targetDiv.textContent = blockedTarget || "Unknown target";
}
