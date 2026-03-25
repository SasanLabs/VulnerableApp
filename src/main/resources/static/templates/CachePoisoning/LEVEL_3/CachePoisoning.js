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

function updateDiagnostics(xmlHttpRequest) {
  document.getElementById("cacheStatus").textContent =
    xmlHttpRequest.getResponseHeader("X-Cache-Status") || "-";
  document.getElementById("cacheKey").textContent =
    xmlHttpRequest.getResponseHeader("X-Cache-Key") || "-";
  document.getElementById("cacheControl").textContent =
    xmlHttpRequest.getResponseHeader("Cache-Control") || "-";
  document.getElementById("varyHeader").textContent =
    xmlHttpRequest.getResponseHeader("Vary") || "-";
}

function updateResponseArea(content) {
  document.getElementById("cachePoisoningResponse").innerHTML = content;
}

function sendCachePoisoningRequest(method, url, forwardedHost) {
  let xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function () {
    if (xmlHttpRequest.readyState !== XMLHttpRequest.DONE) {
      return;
    }

    if (xmlHttpRequest.status !== 200) {
      alert("Request failed");
      return;
    }

    let data = JSON.parse(xmlHttpRequest.responseText);
    updateDiagnostics(xmlHttpRequest);
    updateResponseArea(data.content);
    clearInputs();
  };

  xmlHttpRequest.open(method, url, true);
  if (forwardedHost) {
    xmlHttpRequest.setRequestHeader("X-Forwarded-Host", forwardedHost);
  }
  xmlHttpRequest.send();
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

      sendCachePoisoningRequest("GET", buildLevel3Url(), forwardedHost);
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      let banner = requireValue("bannerInput", "Banner");
      if (!banner) {
        return;
      }

      sendCachePoisoningRequest("GET", buildLevel3Url(), null);
    });
}

addEvents();
