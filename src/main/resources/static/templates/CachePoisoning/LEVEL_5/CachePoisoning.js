import {
  clearInputs,
  fetchDataCallback,
  getHeadersWithForwardedHost,
  getInputValue,
  getNoBrowserCacheHeaders,
  getRequestUrl,
  setDemoUserCookie,
} from "../Common/CachePoisoningCommon.js";

document.getElementById("poisonCacheBtn").addEventListener("click", function () {
  const demoUser = getInputValue("demoUserInput");
  if (demoUser) {
    setDemoUserCookie(demoUser);
  }

  doGetAjaxCall(
    fetchDataCallback,
    getRequestUrl(),
    true,
    getHeadersWithForwardedHost()
  );
  clearInputs(["demoUserInput"]);
});

document.getElementById("resetCacheBtn").addEventListener("click", function () {
  doGetAjaxCall(
    fetchDataCallback,
    getRequestUrl({ resetCache: true }),
    true,
    getNoBrowserCacheHeaders()
  );
});

document
  .getElementById("victimRequestBtn")
  .addEventListener("click", function () {
    setDemoUserCookie(null);
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl(),
      true,
      getNoBrowserCacheHeaders()
    );
  });
