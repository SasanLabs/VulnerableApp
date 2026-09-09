const input = document.getElementById("returnToInput");
const button = document.getElementById("testRedirectBtn");
const sampleLinks = document.querySelectorAll(".sample-link");
const resultBox = document.getElementById("resultBox");
const resultTitle = document.getElementById("resultTitle");
const resultMessage = document.getElementById("resultMessage");

function showBlocked(message, value) {
  resultTitle.innerHTML =
    '<span class="resultIcon">!</span><span>Redirect Blocked</span>';
  resultMessage.textContent = value ? message + ": " + value : message;
  resultBox.style.display = "block";
}

function testLevel12Redirect(value) {
  const encoded = encodeURIComponent(value);
  const url =
    "/VulnerableApp/Http3xxStatusCodeBasedInjection/LEVEL_12?returnTo=" +
    encoded;

  fetch(url, {
    method: "GET",
    redirect: "follow",
  })
    .then(function (response) {
      if (response.redirected) {
        window.location.href = response.url;
        return;
      }

      return response.text().then(function (message) {
        showBlocked(message || "Redirect blocked", value);
      });
    })
    .catch(function () {
      showBlocked("Unable to test redirect right now.", "");
    });
}

if (button && input) {
  button.addEventListener("click", function () {
    testLevel12Redirect(input.value);
  });
}

sampleLinks.forEach(function (link) {
  link.addEventListener("click", function (event) {
    event.preventDefault();
    const value = link.getAttribute("data-value");
    input.value = value;
    resultBox.style.display = "none";
    resultMessage.textContent = "";
  });
});
