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

function getNoBrowserCacheHeaders(headers = {}) {
  return {
    ...headers,
    "Cache-Control": "no-cache",
  };
}

function buildLevel3Url(includeBanner, resetCache = false) {
  let url = getUrlForVulnerabilityLevel();
  let queryParams = new URLSearchParams();

  if (includeBanner) {
    queryParams.set("banner", getBannerValue());
  }

  if (resetCache) {
    queryParams.set("resetCache", "true");
  }

  let queryString = queryParams.toString();
  return queryString ? url + "?" + queryString : url;
}

function updateDiagnostics(request) {
  let cacheStatus = request.getResponseHeader("X-Cache-Status") || "-";
  let cacheKey = request.getResponseHeader("X-Cache-Key") || "-";

  updateCacheStatusIndicator(cacheStatus);
  document.getElementById("cacheKey").textContent = cacheKey;
  document.getElementById("cacheControl").textContent =
    request.getResponseHeader("Cache-Control") || "-";
  document.getElementById("varyHeader").textContent =
    request.getResponseHeader("Vary") || "-";
  updateResetCacheButton(cacheStatus, cacheKey);
}

function updateResponseArea(content) {
  document.getElementById("cachePoisoningResponse").innerHTML = content;
}

function updateCacheStatusIndicator(cacheStatus) {
  let cacheStatusElement = document.getElementById("cacheStatus");
  let normalizedCacheStatus = String(cacheStatus || "-").trim().toUpperCase();

  cacheStatusElement.textContent = cacheStatus;
  cacheStatusElement.classList.remove(
    "cache-status-hit",
    "cache-status-miss",
    "cache-status-neutral"
  );

  if (normalizedCacheStatus === "HIT") {
    cacheStatusElement.classList.add("cache-status-hit");
    return;
  }

  if (normalizedCacheStatus === "MISS") {
    cacheStatusElement.classList.add("cache-status-miss");
    return;
  }

  cacheStatusElement.classList.add("cache-status-neutral");
}

function updateResetCacheButton(cacheStatus, cacheKey) {
  let resetCacheButton = document.getElementById("resetCacheBtn");
  let hasCachedRequest =
    cacheKey !== "-" && cacheStatus !== "-" && cacheStatus !== "";

  resetCacheButton.disabled = !hasCachedRequest;
  resetCacheButton.title = hasCachedRequest
    ? "Cached request available"
    : "No cached request yet";
}

function fetchDataCallback(data, request) {
  updateDiagnostics(request);
  updateResponseArea(data.content);
  clearInputs();
}

function addEvents() {
  document
    .getElementById("resetCacheBtn")
    .addEventListener("click", function () {
      doGetAjaxCall(
        fetchDataCallback,
        buildLevel3Url(false, true),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let banner = requireValue("bannerInput", "Banner");
      let forwardedHost = requireValue("forwardedHostInput", "Forwarded host");
      if (!banner || !forwardedHost) {
        return;
      }

      doGetAjaxCall(
        fetchDataCallback,
        buildLevel3Url(true),
        true,
        getNoBrowserCacheHeaders({ "X-Forwarded-Host": forwardedHost })
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      let banner = requireValue("bannerInput", "Banner");
      if (!banner) {
        return;
      }

      doGetAjaxCall(
        fetchDataCallback,
        buildLevel3Url(true),
        true,
        getNoBrowserCacheHeaders()
      );
    });
}

updateCacheStatusIndicator("-");
updateResetCacheButton("-", "-");
addEvents();
