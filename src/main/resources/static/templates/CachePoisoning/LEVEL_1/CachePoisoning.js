import {
  clearCacheAndFetchFreshResponse,
  clearInputs,
  fetchDataCallback,
  getNoBrowserCacheHeaders,
  getRequestUrl,
} from "../Common/CachePoisoningCommon.js";

document
  .getElementById("poisonCacheBtn")
  .addEventListener("click", function () {
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl(),
      true,
      getNoBrowserCacheHeaders()
    );
    clearInputs(["bannerInput"]);
  });

document.getElementById("resetCacheBtn").addEventListener("click", function () {
  clearCacheAndFetchFreshResponse();
});

document
  .getElementById("victimRequestBtn")
  .addEventListener("click", function () {
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl({ bannerInputId: null }),
      true,
      getNoBrowserCacheHeaders()
    );
  });
