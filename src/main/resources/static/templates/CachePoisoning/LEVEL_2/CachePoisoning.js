function getBannerValue() {
  return document.getElementById("bannerInput").value.trim();
}

function clearBannerValue() {
  document.getElementById("bannerInput").value = "";
}

function requireBannerValue() {
  let banner = getBannerValue();
  if (banner) {
    return banner;
  }

  alert("Banner is required");
  document.getElementById("bannerInput").focus();
  return null;
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

function updateDiagnostics(request) {
  document.getElementById("cacheStatus").textContent =
    request.getResponseHeader("X-Cache-Status") || "-";
  document.getElementById("cacheKey").textContent =
    request.getResponseHeader("X-Cache-Key") || "-";
}

function updateResponseArea(content) {
  document.getElementById("cachePoisoningResponse").innerHTML = content;
}

function fetchDataCallback(data, request) {
  updateDiagnostics(request);
  updateResponseArea(data.content);
  clearBannerValue();
}

function addEvents() {
  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let banner = requireBannerValue();
      if (!banner) {
        return;
      }

      doGetAjaxCall(fetchDataCallback, getCachePoisoningUrl(true), true);
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      doGetAjaxCall(fetchDataCallback, getCachePoisoningUrl(false), true);
    });
}

addEvents();
