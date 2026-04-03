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

function getNoBrowserCacheHeaders(headers = {}) {
  return {
    ...headers,
    "Cache-Control": "no-cache",
  };
}

function getLevel1Url(includeBanner, resetCache = false) {
  let url = getUrlForVulnerabilityLevel();
  let queryParams = new URLSearchParams();

  if (includeBanner) {
    let banner = getBannerValue();
    if (banner) {
      queryParams.set("banner", banner);
    }
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
  clearBannerValue();
}

function addEvents() {
  document
    .getElementById("resetCacheBtn")
    .addEventListener("click", function () {
      doGetAjaxCall(
        fetchDataCallback,
        getLevel1Url(false, true),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let banner = requireBannerValue();
      if (!banner) {
        return;
      }

      doGetAjaxCall(
        fetchDataCallback,
        getLevel1Url(true),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      doGetAjaxCall(
        fetchDataCallback,
        getLevel1Url(false),
        true,
        getNoBrowserCacheHeaders()
      );
    });
}

updateCacheStatusIndicator("-");
updateResetCacheButton("-", "-");
addEvents();
