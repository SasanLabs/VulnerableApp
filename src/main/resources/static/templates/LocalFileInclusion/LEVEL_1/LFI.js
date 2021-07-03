function addingEventListenerToVerifyUrl() {
  document.getElementById("verifyUrl").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    const queryString = location.search;

    url = url + queryString;

    console.log(url);

    doGetAjaxCall(updateUIWithVerifyResponse, url, false);
  });
}
addingEventListenerToVerifyUrl();

function updateUIWithVerifyResponse(data) {
  if (data) {
    document.getElementById("verificationResponse").innerHTML = data;
  } else {
    document.getElementById("verificationResponse").innerHTML = "Try again.";
  }
}
