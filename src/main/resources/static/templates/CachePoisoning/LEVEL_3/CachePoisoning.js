import {
  clearInputs,
  fetchDataCallback,
  getHeadersWithForwardedHost,
  getNoBrowserCacheHeaders,
  getRequestUrl,
} from "../Common/CachePoisoningCommon.js";

document.getElementById("poisonCacheBtn").addEventListener("click", function () {
  doGetAjaxCall(
    fetchDataCallback,
    getRequestUrl(),
    true,
    getHeadersWithForwardedHost()
  );
  clearInputs(["forwardedHostInput"]);
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
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl(),
      true,
      getNoBrowserCacheHeaders()
    );
  });
