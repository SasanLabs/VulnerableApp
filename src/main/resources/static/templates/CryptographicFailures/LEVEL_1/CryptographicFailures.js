function addingEventListenerToSubmitButton() {
  document
    .getElementById("submitButton")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let password = document.getElementById("password").value;
      let data = document.getElementById("data").value;
      let params = new URLSearchParams();

      if (password) {
        params.append("password", password);
      }
      if (data) {
        params.append("data", data);
      }

      doGetAjaxCall(
        appendResponseCallback,
        url + "?" + params.toString(),
        true
      );
    });
}
addingEventListenerToSubmitButton();

function appendResponseCallback(data) {
  let resultDiv = document.getElementById("result");
  if (data.isValid) {
    resultDiv.innerHTML = "<strong>Result:</strong> " + data.content;
    resultDiv.style.color = "green";
  } else {
    resultDiv.innerHTML = "<strong>Result:</strong> " + data.content;
    resultDiv.style.color = "red";
  }
}
