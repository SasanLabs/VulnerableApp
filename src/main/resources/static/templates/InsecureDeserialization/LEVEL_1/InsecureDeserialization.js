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
  if (data.isValid) {
    challengeDiv.className = "challenge-secure";
  } else {
    challengeDiv.className = "challenge-vulnerable";
  }
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

function showControlsForCurrentLevel() {
  let level = getCurrentVulnerabilityLevel();
  let level1Controls = document.getElementById("level1Controls");
  let level2Controls = document.getElementById("level2Controls");
  let level34Controls = document.getElementById("level34Controls");

  level1Controls.classList.add("hide-component");
  level2Controls.classList.add("hide-component");
  level34Controls.classList.add("hide-component");

  if (level === "LEVEL_1") {
    level1Controls.classList.remove("hide-component");
  } else if (level === "LEVEL_2") {
    level2Controls.classList.remove("hide-component");
  } else if (level === "LEVEL_3" || level === "LEVEL_4") {
    level34Controls.classList.remove("hide-component");
  }
}

function afterSave(data) {
  displayResult(data);
  loadChallenge();
}

function addingEventListenerToSaveButtons() {
  document
    .getElementById("saveButtonLevel1")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let payload = new FormData();
      payload.append("theme", document.getElementById("theme").value);
      payload.append(
        "notificationsEnabled",
        document.getElementById("notificationsEnabled").checked
      );

      doPostAjaxCall(afterSave, url, true, payload);
    });

  document
    .getElementById("saveButtonLevel2")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let payload = new FormData();
      payload.append("section", document.getElementById("section").value);

      doPostAjaxCall(afterSave, url, true, payload);
    });

  document
    .getElementById("saveButtonLevel34")
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

showControlsForCurrentLevel();
addingEventListenerToSaveButtons();
addingEventListenerToCheckButton();
loadChallenge();
