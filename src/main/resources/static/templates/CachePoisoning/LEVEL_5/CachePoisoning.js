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
      let demoUser = requireDemoUserValue();
      if (!demoUser) {
        return;
      }

      setDemoUserCookie(demoUser);
      sendCachePoisoningRequest(
        "GET",
        buildLevel5Url(),
        getForwardedHostValue()
      );
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      sendCachePoisoningRequest("GET", buildLevel5Url(), null);
    });
}

addEvents();
