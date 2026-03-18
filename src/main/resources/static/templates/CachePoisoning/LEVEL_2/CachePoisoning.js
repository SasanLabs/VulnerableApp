function getBannerValue() {
  return document.getElementById("bannerInput").value.trim();
}

function getCachePoisoningUrl(includeBanner) {
  let url = getUrlForVulnerabilityLevel();
  if (!includeBanner) {
    return url;
  }

  let banner = getBannerValue();
  if (!banner) {
    return url;
  }

  return url + "?banner=" + encodeURIComponent(banner);
}

function updateDiagnostics(xmlHttpRequest) {
  document.getElementById("cacheStatus").textContent =
    xmlHttpRequest.getResponseHeader("X-Cache-Status") || "-";
  document.getElementById("cacheKey").textContent =
    xmlHttpRequest.getResponseHeader("X-Cache-Key") || "-";
}

function updateResponseArea(content) {
  document.getElementById("cachePoisoningResponse").innerHTML = content;
}

function sendCachePoisoningRequest(method, url) {
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
  };

  xmlHttpRequest.open(method, url, true);
  if (method === "GET") {
    xmlHttpRequest.setRequestHeader("Content-Type", "application/json");
  }
  xmlHttpRequest.send();
}

function addEvents() {
  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      sendCachePoisoningRequest("GET", getCachePoisoningUrl(true));
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      sendCachePoisoningRequest("GET", getCachePoisoningUrl(false));
    });
}

addEvents();
