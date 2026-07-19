function getQueryParam(name) {
  var params = new URLSearchParams(window.location.search);
  return params.get(name);
}

function getLevel() {
  var rawLevel = getQueryParam("level");
  var parsedLevel = parseInt(rawLevel, 10);
  if (Number.isNaN(parsedLevel) || parsedLevel < 1 || parsedLevel > 9) {
    return null;
  }
  return parsedLevel;
}

function getToken() {
  return getQueryParam("token");
}

function getContextPath() {
  var marker = "/password-reset/";
  var path = window.location.pathname || "";
  var markerIndex = path.indexOf(marker);
  if (markerIndex < 0) {
    return "";
  }
  return path.substring(0, markerIndex);
}

function getResetEndpoint(level) {
  return getContextPath() + "/PasswordResetVulnerability/LEVEL_" + level;
}

function toTitleCase(text) {
  if (!text) {
    return "";
  }
  return text
    .replace(/([A-Z])/g, " $1")
    .replace(/_/g, " ")
    .trim()
    .replace(/^./, function (character) {
      return character.toUpperCase();
    });
}

function appendDetailRow(container, key, value) {
  var row = document.createElement("div");
  row.className = "detail-row";

  var label = document.createElement("span");
  label.className = "detail-key";
  label.textContent = toTitleCase(key);

  var text = document.createElement("span");
  text.className = "detail-value";
  text.textContent =
    value === null || value === undefined ? "-" : String(value);

  row.appendChild(label);
  row.appendChild(text);
  container.appendChild(row);
}

function getVisibleDetailKeys(content) {
  if (!content || typeof content !== "object") {
    return [];
  }

  return Object.keys(content).filter(function (key) {
    return key === "username";
  });
}

function applySuccessState(content) {
  var badge = document.getElementById("resetLevelBadge");
  var title = document.getElementById("resetTitle");
  var subtitle = document.getElementById("resetSubtitle");
  var button = document.getElementById("applyResetBtn");

  badge.textContent = "Success";
  badge.style.background = "#dcfce7";
  badge.style.color = "#166534";
  title.textContent = "Password reset successful";
  subtitle.textContent =
    content && content.username
      ? "The password for " + content.username + " was updated successfully."
      : "The password was updated successfully.";

  button.textContent = "Password Reset Complete";
  button.disabled = true;
}

function setResult(data) {
  var resultCard = document.getElementById("resultCard");
  var resultStatus = document.getElementById("resultStatus");
  var resultMessage = document.getElementById("resultMessage");
  var resultDetails = document.getElementById("resultDetails");

  resultDetails.innerHTML = "";
  resultCard.classList.remove("hidden");

  var isValid = !!(data && data.isValid);
  resultStatus.textContent = isValid ? "Success" : "Failed";
  resultStatus.className = isValid
    ? "result-badge success"
    : "result-badge failure";

  var content = data ? data.content : null;
  if (typeof content === "string") {
    resultMessage.textContent = content;
    return;
  }

  resultMessage.textContent =
    content && content.message ? content.message : "Request completed.";
  if (content && typeof content === "object") {
    getVisibleDetailKeys(content).forEach(function (key) {
      appendDetailRow(resultDetails, key, content[key]);
    });
  }

  if (isValid) {
    applySuccessState(content);
  }
}

function setPageChrome(level) {
  var badge = document.getElementById("resetLevelBadge");
  var title = document.getElementById("resetTitle");
  var subtitle = document.getElementById("resetSubtitle");

  if (!level) {
    badge.textContent = "Invalid Link";
    title.textContent = "Reset link is incomplete";
    subtitle.textContent =
      "This URL is missing the required level information. Request a fresh reset email from the lab.";
    return;
  }

  badge.textContent = "Level " + level + " Reset";

  if (level === 9) {
    badge.style.background = "#dcfce7";
    badge.style.color = "#166534";
    title.textContent = "Choose a new secure password";
    subtitle.textContent =
      "This reset link relies on backend checks for expiry and one-time use. Submit a new password to complete the secure flow.";
  }
}

function maybeLoadReferrerLeakDemo(level) {
  if (level !== 7) {
    return;
  }

  var externalCard = document.getElementById("level7ExternalCard");
  var externalImage = document.getElementById("level7ExternalImage");
  if (!externalCard || !externalImage) {
    return;
  }

  externalCard.classList.remove("hidden");
  externalImage.referrerPolicy = "unsafe-url";
  externalImage.src =
    "https://dummyimage.com/320x120/e5e7eb/374151.png&text=Third-party+image";
}

function submitReset() {
  var level = getLevel();
  var token = getToken();
  var newPassword = document.getElementById("newPassword").value;

  if (!level || !token) {
    setResult({
      isValid: false,
      content:
        "Reset link is missing required parameters. Request a fresh email and open the new link.",
    });
    return;
  }

  if (!newPassword) {
    setResult({
      isValid: false,
      content: "New password is required.",
    });
    return;
  }

  var request = new XMLHttpRequest();
  request.open("POST", getResetEndpoint(level), true);
  request.setRequestHeader("Content-Type", "application/json");
  request.onreadystatechange = function () {
    if (request.readyState !== XMLHttpRequest.DONE) {
      return;
    }

    try {
      setResult(JSON.parse(request.responseText));
    } catch (error) {
      setResult({
        isValid: false,
        content: "Unable to parse backend response.",
      });
    }
  };

  request.send(
    JSON.stringify({
      action: "reset",
      token: token,
      newPassword: newPassword,
    })
  );

  document.getElementById("newPassword").value = "";
}

document.getElementById("applyResetBtn").addEventListener("click", submitReset);

setPageChrome(getLevel());
maybeLoadReferrerLeakDemo(getLevel());

var passwordInput = document.getElementById("newPassword");
if (passwordInput) {
  passwordInput.focus();
}
