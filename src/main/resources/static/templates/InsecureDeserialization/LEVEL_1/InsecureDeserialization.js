function loadChallenge() {
  let url = getUrlForVulnerabilityLevel();
  doGetAjaxCall(displayChallenge, url, true);
}

function renderStrongText(container, text) {
  container.innerHTML = "";
  let strong = document.createElement("strong");
  strong.textContent = text;
  container.appendChild(strong);
}

function displayChallenge(data) {
  let challengeDiv = document.getElementById("challenge");
  renderStrongText(challengeDiv, data.content);
  if (data.isValid) {
    challengeDiv.className = "challenge-secure";
  } else {
    challengeDiv.className = "challenge-vulnerable";
  }
}

function addingEventListenerToGetTokenButton() {
  let optionsDiv = document.getElementById("preferencesOptions");
  let getTokenButton = document.getElementById("getTokenButton");
  if (!optionsDiv || !getTokenButton) {
    return;
  }
  if (getCurrentVulnerabilityLevel() !== "LEVEL_1") {
    optionsDiv.style.display = "none";
    return;
  }

  getTokenButton.addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    let theme = document.getElementById("theme").value;
    let notificationsEnabled = document.getElementById(
      "notificationsEnabled"
    ).checked;

    let params = new URLSearchParams();
    params.append("theme", theme);
    params.append("notificationsEnabled", notificationsEnabled);

    doGetAjaxCall(displayChallenge, url + "?" + params.toString(), true);
  });
}

function addingEventListenerToSubmitButton() {
  document
    .getElementById("submitButton")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let payload = document.getElementById("payload").value;

      if (!payload) {
        let resultDiv = document.getElementById("result");
        renderStrongText(resultDiv, "Please paste a Base64 payload.");
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
  resultDiv.innerHTML = "";
  let label = document.createElement("strong");
  label.textContent = "Result:";
  resultDiv.appendChild(label);
  resultDiv.appendChild(document.createTextNode(" " + data.content));
  resultDiv.className = data.isValid ? "result-success" : "result-failure";
}

addingEventListenerToSubmitButton();
addingEventListenerToGetTokenButton();
loadChallenge();
