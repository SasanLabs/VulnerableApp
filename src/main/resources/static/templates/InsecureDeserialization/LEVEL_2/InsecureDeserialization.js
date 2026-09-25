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

function startSession() {
  doPostAjaxCall(loadChallenge, getUrlForVulnerabilityLevel(), true);
}

function addingEventListenerToCheckButton() {
  document.getElementById("checkButton").addEventListener("click", function () {
    loadChallenge();
  });
}

addingEventListenerToCheckButton();
startSession();
