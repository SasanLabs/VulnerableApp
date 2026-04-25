export function getInputValue(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

export function clearInputs(ids) {
  ids.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });
}

export function getNoBrowserCacheHeaders(headers = {}) {
  return {
    ...headers,
    "Cache-Control": "no-store, no-cache, max-age=0",
    Pragma: "no-cache",
  };
}

export function getRequestUrl(options = {}) {
  const { bannerInputId = "bannerInput", browserCacheBust = true } = options;
  const queryParams = new URLSearchParams();

  const banner = bannerInputId ? getInputValue(bannerInputId) : "";
  if (banner) {
    queryParams.set("banner", banner);
  }

  if (browserCacheBust) {
    queryParams.set(
      "_browserCacheBust",
      `${Date.now()}-${Math.random().toString(16).slice(2)}`
    );
  }

  const queryString = queryParams.toString();
  const url = getUrlForVulnerabilityLevel();
  return queryString ? url + "?" + queryString : url;
}

export function clearCacheAndFetchFreshResponse() {
  const clearCacheUrl =
    getUrlForVulnerability() +
    "/clearCache" +
    "?level=" +
    encodeURIComponent(getCurrentVulnerabilityLevel());

  doPostAjaxCall(
    fetchDataCallback,
    clearCacheUrl,
    true,
    null,
    getNoBrowserCacheHeaders()
  );
}

export function getHeadersWithForwardedHost(inputId = "forwardedHostInput") {
  const headers = getNoBrowserCacheHeaders();
  const forwardedHost = getInputValue(inputId);
  if (forwardedHost) {
    headers["X-Forwarded-Host"] = forwardedHost;
  }
  return headers;
}

export function setDemoUserCookie(value) {
  if (value) {
    document.cookie = `demo_user=${value}; path=/; SameSite=Lax`;
  } else {
    document.cookie =
      "demo_user=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  }
}

export function fetchDataCallback(data, request) {
  updateDiagnostics(request);
  document.getElementById("cachePoisoningResponse").innerHTML = data.content;
}

function updateDiagnostics(request) {
  const cacheStatus = request.getResponseHeader("X-Cache-Status") || "-";
  const cacheKey = request.getResponseHeader("X-Cache-Key") || "-";
  const cacheControl = request.getResponseHeader("Cache-Control") || "-";
  const vary = request.getResponseHeader("Vary") || "-";

  updateCacheStatusIndicator(cacheStatus);
  document.getElementById("cacheKey").textContent = cacheKey;

  const cacheControlEl = document.getElementById("cacheControl");
  if (cacheControlEl) cacheControlEl.textContent = cacheControl;

  const varyHeaderEl = document.getElementById("varyHeader");
  if (varyHeaderEl) varyHeaderEl.textContent = vary;

  updateResetCacheButton(cacheStatus, cacheKey);
}

function updateCacheStatusIndicator(cacheStatus) {
  const el = document.getElementById("cacheStatus");
  const status = String(cacheStatus || "-")
    .trim()
    .toUpperCase();

  el.textContent = cacheStatus;
  el.classList.remove(
    "cache-status-hit",
    "cache-status-miss",
    "cache-status-neutral"
  );

  if (status === "HIT") el.classList.add("cache-status-hit");
  else if (status === "MISS") el.classList.add("cache-status-miss");
  else el.classList.add("cache-status-neutral");
}

function updateResetCacheButton(cacheStatus, cacheKey) {
  const btn = document.getElementById("resetCacheBtn");
  const hasCache =
    cacheKey !== "-" && cacheStatus !== "-" && cacheStatus !== "";
  btn.disabled = !hasCache;
}
