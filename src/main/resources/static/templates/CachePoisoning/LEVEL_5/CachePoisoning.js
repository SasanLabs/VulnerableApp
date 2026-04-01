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

function buildLevel5Url() {
  let url = getUrlForVulnerabilityLevel();
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
        forwardedHost ? { "X-Forwarded-Host": forwardedHost } : {}
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      doGetAjaxCall(fetchDataCallback, buildLevel5Url(), true);
    });
}

addEvents();
