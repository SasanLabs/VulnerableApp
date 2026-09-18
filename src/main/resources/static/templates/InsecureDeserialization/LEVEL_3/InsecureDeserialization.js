function renderStrongText(container, text) {
  container.innerHTML = "";
  let strong = document.createElement("strong");
  strong.textContent = text;
  container.appendChild(strong);
}

function loadChallenge() {
  let url = getUrlForVulnerabilityLevel();
  doGetAjaxCall(displayChallenge, url, true);
}

function displayChallenge(data) {
  let challengeDiv = document.getElementById("challenge");
  renderStrongText(challengeDiv, data.content);
  challengeDiv.className = data.isValid
    ? "challenge-secure"
    : "challenge-vulnerable";
}

function displayResult(data) {
  let resultDiv = document.getElementById("result");
  resultDiv.innerHTML = "";
  let label = document.createElement("strong");
  label.textContent = "Result:";
  resultDiv.appendChild(label);
  resultDiv.appendChild(document.createTextNode(" " + data.content));
  resultDiv.className = data.isValid ? "result-success" : "result-failure";
}

function afterSave(data) {
  displayResult(data);
  loadChallenge();
}

function addingEventListenerToSaveButton() {
  document
    .getElementById("saveButtonLevel3")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let payload = new FormData();
      payload.append("username", document.getElementById("username").value);

      doPostAjaxCall(afterSave, url, true, payload);
    });
}

function addingEventListenerToCheckButton() {
  document.getElementById("checkButton").addEventListener("click", function () {
    loadChallenge();
  });
}

addingEventListenerToSaveButton();
addingEventListenerToCheckButton();
loadChallenge();
