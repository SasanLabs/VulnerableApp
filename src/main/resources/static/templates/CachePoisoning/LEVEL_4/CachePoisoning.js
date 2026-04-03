function getDemoUserValue() {
  return document.getElementById("demoUserInput").value.trim();
}

function clearDemoUserValue() {
  document.getElementById("demoUserInput").value = "";
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

function getLevel4Url(resetCache = false) {
  let url = getUrlForVulnerabilityLevel();
  if (!resetCache) {
    return url;
  }

  return url + "?resetCache=true";
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
  clearDemoUserValue();
}

function addEvents() {
  document
    .getElementById("resetCacheBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      doGetAjaxCall(
        fetchDataCallback,
        getLevel4Url(true),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("poisonCacheBtn")
    .addEventListener("click", function () {
      let demoUser = requireDemoUserValue();
      if (!demoUser) {
        return;
      }

      setDemoUserCookie(demoUser);
      doGetAjaxCall(
        fetchDataCallback,
        getLevel4Url(),
        true,
        getNoBrowserCacheHeaders()
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      doGetAjaxCall(
        fetchDataCallback,
        getLevel4Url(),
        true,
        getNoBrowserCacheHeaders()
      );
    });
}

updateCacheStatusIndicator("-");
updateResetCacheButton("-", "-");
addEvents();
