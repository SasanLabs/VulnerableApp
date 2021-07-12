function addingEventListenerToVerifyUrl() {
  document.getElementById("verifyUrl").addEventListener("click", function () {
    let urlInput = document.getElementById("url").value;
    let params = "?file=" + getParameterByName("file", urlInput);
    let urlLevel = getUrlForVulnerabilityLevel() + params;
    doGetAjaxCall(updateUIWithVerifyResponse, urlLevel, false);
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

function getParameterByName(name, url) {
  name = name.replace(/[\[\]]/g, "\\$&");
  var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
    results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return "";
  return results[2];
}
