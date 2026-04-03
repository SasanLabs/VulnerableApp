function getDemoUserValue() {
  return document.getElementById("demoUserInput").value.trim();
}

function getBannerValue() {
  return document.getElementById("bannerInput").value.trim();
}

function getForwardedHostValue() {
  return document.getElementById("forwardedHostInput").value.trim();
}

function clearInputs() {
  document.getElementById("demoUserInput").value = "";
  document.getElementById("bannerInput").value = "";
  document.getElementById("forwardedHostInput").value = "";
}

function requireDemoUserValue() {
  let demoUser = getDemoUserValue();
  if (demoUser) {
    return demoUser;
  }

  alert("Demo user is required");
  document.getElementById("demoUserInput").focus();
  return null;
}

function setDemoUserCookie(demoUser) {
  document.cookie =
    "demo_user=" + encodeURIComponent(demoUser) + "; path=/; SameSite=Lax";
}

function clearDemoUserCookie() {
  document.cookie =
    "demo_user=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax";
}

function getNoBrowserCacheHeaders(headers = {}) {
  return {
    ...headers,
    "Cache-Control": "no-cache",
  };
}

function buildLevel5Url(includeBanner = true, resetCache = false) {
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
      clearDemoUserCookie();
      doGetAjaxCall(
        fetchDataCallback,
        buildLevel5Url(false, true),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let demoUser = requireDemoUserValue();
      let forwardedHost = getForwardedHostValue();
      if (!demoUser) {
        return;
      }

      setDemoUserCookie(demoUser);
      doGetAjaxCall(
        fetchDataCallback,
        buildLevel5Url(),
        true,
        getNoBrowserCacheHeaders(
          forwardedHost ? { "X-Forwarded-Host": forwardedHost } : {}
        )
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      doGetAjaxCall(
        fetchDataCallback,
        buildLevel5Url(),
        true,
        getNoBrowserCacheHeaders()
      );
    });
}

updateCacheStatusIndicator("-");
updateResetCacheButton("-", "-");
addEvents();
