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
    clearDemoUserValue();
  };

  xmlHttpRequest.open(method, url, true);
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
      sendCachePoisoningRequest("GET", getUrlForVulnerabilityLevel());
    });

  document
    .getElementById("victimRequestBtn")
    .addEventListener("click", function () {
      clearDemoUserCookie();
      sendCachePoisoningRequest("GET", getUrlForVulnerabilityLevel());
    });
}

addEvents();
