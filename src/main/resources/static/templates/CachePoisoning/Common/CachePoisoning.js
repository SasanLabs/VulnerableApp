const CONFIGS = {
  LEVEL_1: {
    title: "Cache Poisoning - Level 1",
    subtitle: "A shared cache stores responses by route only. Use the controls below and rely on the app help panel for exploitation guidance.",
    banner: { show: true, label: "Attacker banner", placeholder: "FAKE URGENT UPDATE" },
    forwardedHost: { show: false },
    demoUser: { show: false },
    poisonBtn: "inline",
    signals: ["status", "key"],
    responseInitial: "Use the controls above to poison the cache and verify whether the victim receives the attacker-controlled banner.",
    victimBtnText: "Send victim request"
  },
  LEVEL_2: {
    title: "Cache Poisoning - Level 2",
    subtitle: "This level blocks only obvious script payloads. Try a harmless-looking HTML banner or a misleading status message and observe that the shared cache still replays it to the next visitor.",
    banner: { show: true, label: "Filtered banner", placeholder: "<b>URGENT STATUS PAGE UPDATE</b>" },
    forwardedHost: { show: false },
    demoUser: { show: false },
    poisonBtn: "inline",
    signals: ["status", "key"],
    responseInitial: "Poison the cache with a filtered banner, then send the victim request and confirm the same response is replayed from the shared cache.",
    victimBtnText: "Send victim request"
  },
  LEVEL_3: {
    title: "Cache Poisoning - Level 3",
    subtitle: "This level fixes the banner cache key, but still trusts <code>X-Forwarded-Host</code> when building absolute asset URLs. Use the same banner value for both requests and change only the attacker header.",
    banner: { show: true, label: "Shared banner key", placeholder: "shared-status-page" },
    forwardedHost: { show: true, label: "Attacker forwarded host", placeholder: "attacker.example" },
    demoUser: { show: false },
    poisonBtn: "inline",
    signals: ["status", "key", "cacheControl", "vary"],
    responseInitial: "Send the attacker request with a forwarded host, then repeat the victim request with the same banner key and no forwarded header.",
    victimBtnText: "Send victim request"
  },
  LEVEL_4: {
    title: "Cache Poisoning - Level 4",
    subtitle: "This level stops trusting <code>X-Forwarded-Host</code>, but still caches personalized content publicly. The attacker request sets <code>demo_user</code>; the victim request clears that cookie before loading the same route.",
    banner: { show: false },
    forwardedHost: { show: false },
    demoUser: { show: true, label: "Attacker demo_user cookie", placeholder: "admin" },
    poisonBtn: "demo",
    signals: ["status", "key", "cacheControl", "vary"],
    responseInitial: "Personalize the attacker request with <code>demo_user</code>, then clear the cookie with the victim request and observe the reused shared-cache response.",
    victimBtnText: "Send victim request"
  },
  LEVEL_5: {
    title: "Cache Poisoning - Level 5 (Secure)",
    subtitle: "This secure level ignores untrusted forwarding headers and uses a private, non-stored response for personalized content. Try sending the same malicious inputs and compare the diagnostics with the previous levels.",
    banner: { show: true, label: "Optional banner preview", placeholder: "<b>OWNED</b>" },
    forwardedHost: { show: true, label: "Optional forwarded host", placeholder: "attacker.example" },
    demoUser: { show: true, label: "Personalized demo_user", placeholder: "admin" },
    poisonBtn: "demo",
    signals: ["status", "key", "cacheControl", "vary"],
    responseInitial: "Send a personalized request first, then clear the cookie with a guest request. The response should stay fresh and should not reuse attacker-controlled forwarding data.",
    victimBtnText: "Send guest request"
  }
};

function getInputValue(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

function clearInputs() {
  ["bannerInput", "forwardedHostInput", "demoUserInput"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });
}

function getNoBrowserCacheHeaders(headers = {}) {
  return { ...headers, "Cache-Control": "no-cache" };
}

function getRequestUrl(includeInputs, resetCache = false) {
  let url = getUrlForVulnerabilityLevel();
  let queryParams = new URLSearchParams();

  if (resetCache) {
    queryParams.set("resetCache", "true");
  }

  // Always include banner if it's visible and has a value,
  // as it's often part of the cache key (Level 3+)
  const banner = getInputValue("bannerInput");
  if (banner) {
    queryParams.set("banner", banner);
  }

  let queryString = queryParams.toString();
  return queryString ? url + "?" + queryString : url;
}

function getCustomHeaders() {
  const headers = getNoBrowserCacheHeaders();
  const forwardedHost = getInputValue("forwardedHostInput");
  if (forwardedHost) {
    headers["X-Forwarded-Host"] = forwardedHost;
  }
  return headers;
}

function setDemoUserCookie(value) {
  if (value) {
    document.cookie = `demo_user=${value}; path=/; SameSite=Lax`;
  } else {
    document.cookie = "demo_user=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  }
}

function updateDiagnostics(request) {
  const cacheStatus = request.getResponseHeader("X-Cache-Status") || "-";
  const cacheKey = request.getResponseHeader("X-Cache-Key") || "-";
  const cacheControl = request.getResponseHeader("Cache-Control") || "-";
  const vary = request.getResponseHeader("Vary") || "-";

  updateCacheStatusIndicator(cacheStatus);
  document.getElementById("cacheKey").textContent = cacheKey;
  if (document.getElementById("cacheControl")) document.getElementById("cacheControl").textContent = cacheControl;
  if (document.getElementById("varyHeader")) document.getElementById("varyHeader").textContent = vary;
  
  updateResetCacheButton(cacheStatus, cacheKey);
}

function updateCacheStatusIndicator(cacheStatus) {
  const el = document.getElementById("cacheStatus");
  const status = String(cacheStatus || "-").trim().toUpperCase();

  el.textContent = cacheStatus;
  el.classList.remove("cache-status-hit", "cache-status-miss", "cache-status-neutral");

  if (status === "HIT") el.classList.add("cache-status-hit");
  else if (status === "MISS") el.classList.add("cache-status-miss");
  else el.classList.add("cache-status-neutral");
}

function updateResetCacheButton(cacheStatus, cacheKey) {
  const btn = document.getElementById("resetCacheBtn");
  const hasCache = cacheKey !== "-" && cacheStatus !== "-" && cacheStatus !== "";
  btn.disabled = !hasCache;
}

function fetchDataCallback(data, request) {
  updateDiagnostics(request);
  document.getElementById("cachePoisoningResponse").innerHTML = data.content;
  // We don't clear inputs here to allow user to see what they sent, 
  // but some levels might prefer clearing. Let's keep it for now.
}

function initLevel() {
  const level = vulnerabilityLevelSelected || "LEVEL_1";
  const config = CONFIGS[level];

  // Set texts
  document.getElementById("cpTitle").innerHTML = config.title;
  document.getElementById("cpSubtitle").innerHTML = config.subtitle;
  document.getElementById("cachePoisoningResponse").innerHTML = config.responseInitial;
  document.getElementById("victimRequestBtn").textContent = config.victimBtnText;

  // Configure Inputs
  if (config.banner.show) {
    document.getElementById("bannerInputRow").classList.remove("hidden");
    document.getElementById("bannerLabel").textContent = config.banner.label;
    document.getElementById("bannerInput").placeholder = config.banner.placeholder;
  }

  if (config.forwardedHost.show) {
    document.getElementById("forwardedHostInputRow").classList.remove("hidden");
    document.getElementById("forwardedHostLabel").textContent = config.forwardedHost.label;
    document.getElementById("forwardedHostInput").placeholder = config.forwardedHost.placeholder;
  }

  if (config.demoUser.show) {
    document.getElementById("demoUserInputRow").classList.remove("hidden");
    document.getElementById("demoUserLabel").textContent = config.demoUser.label;
    document.getElementById("demoUserInput").placeholder = config.demoUser.placeholder;
  }

  // Configure Poison Button Position
  const poisonBtnId = config.poisonBtn === "inline" ? "poisonCacheBtnInline" : 
                     (config.poisonBtn === "demo" ? "poisonCacheBtnDemo" : "poisonCacheBtnBottom");
  const poisonBtnContainerId = config.poisonBtn === "inline" ? "poisonCacheBtnContainerInline" : 
                              (config.poisonBtn === "demo" ? "poisonCacheBtnContainerDemo" : "");
  
  document.getElementById(poisonBtnContainerId).classList.remove("hidden");
  
  const poisonHandler = function() {
    const demoUser = getInputValue("demoUserInput");
    if (config.demoUser.show && demoUser) {
        setDemoUserCookie(demoUser);
    }
    doGetAjaxCall(fetchDataCallback, getRequestUrl(true), true, getCustomHeaders());
  };

  document.getElementById(poisonBtnId).addEventListener("click", poisonHandler);

  // Other Events
  document.getElementById("resetCacheBtn").addEventListener("click", function() {
    doGetAjaxCall(fetchDataCallback, getRequestUrl(false, true), true, getNoBrowserCacheHeaders());
  });

  document.getElementById("victimRequestBtn").addEventListener("click", function() {
    if (config.demoUser.show) {
        setDemoUserCookie(null); // Clear cookie for victim request in demo user levels
    }
    doGetAjaxCall(fetchDataCallback, getRequestUrl(false), true, getNoBrowserCacheHeaders());
  });

  // Configure Signals
  if (config.signals.includes("cacheControl")) document.getElementById("cacheControlCard").classList.remove("hidden");
  if (config.signals.includes("vary")) document.getElementById("varyHeaderCard").classList.remove("hidden");
}

initLevel();
