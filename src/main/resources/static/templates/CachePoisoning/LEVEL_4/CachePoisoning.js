import {
  clearInputs,
  fetchDataCallback,
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
    getRequestUrl({ bannerInputId: null }),
    true,
    getNoBrowserCacheHeaders()
  );
  clearInputs(["demoUserInput"]);
});

document.getElementById("resetCacheBtn").addEventListener("click", function () {
  doGetAjaxCall(
    fetchDataCallback,
    getRequestUrl({ bannerInputId: null, resetCache: true }),
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
      getRequestUrl({ bannerInputId: null }),
      true,
      getNoBrowserCacheHeaders()
    );
  });
