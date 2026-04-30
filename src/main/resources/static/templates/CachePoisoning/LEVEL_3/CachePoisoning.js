import {
  clearCacheAndFetchFreshResponse,
  clearInputs,
  fetchDataCallback,
  getHeadersWithForwardedHost,
  getRequestUrl,
} from "../Common/CachePoisoningCommon.js";

document
  .getElementById("poisonCacheBtn")
  .addEventListener("click", function () {
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl(),
      true,
      getHeadersWithForwardedHost()
    );
    clearInputs(["forwardedHostInput"]);
  });

document.getElementById("resetCacheBtn").addEventListener("click", function () {
  clearCacheAndFetchFreshResponse();
});

document
  .getElementById("victimRequestBtn")
  .addEventListener("click", function () {
    doGetAjaxCall(fetchDataCallback, getRequestUrl(), true, {});
  });
