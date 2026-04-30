import {
  clearCacheAndFetchFreshResponse,
  clearInputs,
  fetchDataCallback,
  getInputValue,
  getRequestUrl,
  setDemoUserCookie,
} from "../Common/CachePoisoningCommon.js";

document
  .getElementById("poisonCacheBtn")
  .addEventListener("click", function () {
    const demoUser = getInputValue("demoUserInput");
    if (demoUser) {
      setDemoUserCookie(demoUser);
    }
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl({ bannerInputId: null }),
      true,
      {}
    );
    clearInputs(["demoUserInput"]);
  });

document.getElementById("resetCacheBtn").addEventListener("click", function () {
  setDemoUserCookie(null);
  clearCacheAndFetchFreshResponse();
});

document
  .getElementById("victimRequestBtn")
  .addEventListener("click", function () {
    setDemoUserCookie(null);
    doGetAjaxCall(
      fetchDataCallback,
      getRequestUrl({ bannerInputId: null }),
      true,
      {}
    );
  });
