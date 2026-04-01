function getBannerValue() {
  return document.getElementById("bannerInput").value.trim();
}

function getForwardedHostValue() {
  return document.getElementById("forwardedHostInput").value.trim();
}

function clearInputs() {
  document.getElementById("bannerInput").value = "";
  document.getElementById("forwardedHostInput").value = "";
}

function requireValue(inputId, label) {
  let value = document.getElementById(inputId).value.trim();
  if (value) {
    return value;
  }

  alert(label + " is required");
  document.getElementById(inputId).focus();
  return null;
}

function buildLevel3Url() {
  return (
    getUrlForVulnerabilityLevel() +
    "?banner=" +
    encodeURIComponent(getBannerValue())
  );
}

function updateDiagnostics(request) {
  document.getElementById("cacheStatus").textContent =
    request.getResponseHeader("X-Cache-Status") || "-";
  document.getElementById("cacheKey").textContent =
    request.getResponseHeader("X-Cache-Key") || "-";
  document.getElementById("cacheControl").textContent =
    request.getResponseHeader("Cache-Control") || "-";
  document.getElementById("varyHeader").textContent =
    request.getResponseHeader("Vary") || "-";
}

function updateResponseArea(content) {
  document.getElementById("cachePoisoningResponse").innerHTML = content;
}

function fetchDataCallback(data, request) {
  updateDiagnostics(request);
  updateResponseArea(data.content);
  clearInputs();
}

function addEvents() {
  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let banner = requireValue("bannerInput", "Banner");
      let forwardedHost = requireValue("forwardedHostInput", "Forwarded host");
      if (!banner || !forwardedHost) {
        return;
      }

      doGetAjaxCall(fetchDataCallback, buildLevel3Url(), true, { "X-Forwarded-Host": forwardedHost});
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      let banner = requireValue("bannerInput", "Banner");
      if (!banner) {
        return;
      }

      doGetAjaxCall(fetchDataCallback, buildLevel3Url(), true);
    });
}

addEvents();
