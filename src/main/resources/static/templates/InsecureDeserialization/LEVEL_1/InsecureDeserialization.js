function loadChallenge() {
  let url = getUrlForVulnerabilityLevel();
  doGetAjaxCall(displayChallenge, url, true);
}

function displayChallenge(data) {
  let challengeDiv = document.getElementById("challenge");
  challengeDiv.innerHTML = "<strong>" + data.content + "</strong>";
  if (data.isValid) {
    challengeDiv.className = "challenge-secure";
  } else {
    challengeDiv.className = "challenge-vulnerable";
  }
}

function addingEventListenerToSubmitButton() {
  document
    .getElementById("submitButton")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let payload = document.getElementById("payload").value;

      if (!payload) {
        let resultDiv = document.getElementById("result");
        resultDiv.innerHTML = "<strong>Please paste a Base64 payload.</strong>";
        resultDiv.style.color = "red";
        return;
      }

      let params = new URLSearchParams();
      params.append("payload", payload);

      doGetAjaxCall(
        appendResponseCallback,
        url + "?" + params.toString(),
        true
      );
    });
}

function appendResponseCallback(data) {
  let resultDiv = document.getElementById("result");
  if (data.isValid) {
    resultDiv.innerHTML = "<strong>Result:</strong> " + data.content;
    resultDiv.className = "result-success";
  } else {
    resultDiv.innerHTML = "<strong>Result:</strong> " + data.content;
    resultDiv.className = "result-failure";
  }
}

addingEventListenerToSubmitButton();
loadChallenge();
