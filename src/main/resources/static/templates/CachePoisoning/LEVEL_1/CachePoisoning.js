import {
  clearCacheAndFetchFreshResponse,
  clearInputs,
  fetchDataCallback,
  getRequestUrl,
} from "../Common/CachePoisoningCommon.js";

document
  .getElementById("poisonCacheBtn")
  .addEventListener("click", function () {
    doGetAjaxCall(fetchDataCallback, getRequestUrl(), true, {});
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
      {}
    );
  });
