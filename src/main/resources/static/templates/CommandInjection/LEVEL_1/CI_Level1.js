function addingEventListenerToPingButton() {
  document.getElementById("pingBtn").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    doGetAjaxCall(
      pingUtilityCallback,
      url + "?ipaddress=" + document.getElementById("ipaddress").value,
      true
    );
  });
}
addingEventListenerToPingButton();

function pingUtilityCallback(data) {
  document.getElementById("pingUtilityResponse").innerHTML = data.content;
}
