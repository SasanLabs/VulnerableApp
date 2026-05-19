function addingEventListenerToGreetButton() {
  document.getElementById("greetBtn").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    doGetAjaxCall(
      greetUtilityCallback,
      url +
        "?name=" +
        encodeURIComponent(document.getElementById("name").value),
      true
    );
  });
}
addingEventListenerToGreetButton();

function greetUtilityCallback(data) {
  document.getElementById("sstiResponse").innerHTML = data.content;
}
