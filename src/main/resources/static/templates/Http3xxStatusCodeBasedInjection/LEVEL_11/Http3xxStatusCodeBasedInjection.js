const input = document.getElementById("returnToInput");
const button = document.getElementById("testRedirectBtn");
const sampleLinks = document.querySelectorAll(".sample-link");
const resultBox = document.getElementById("resultBox");
const resultTitle = document.getElementById("resultTitle");
const resultMessage = document.getElementById("resultMessage");

function testLevel11Redirect(value) {
  const encoded = encodeURIComponent(value);
  const url =
    "/VulnerableApp/Http3xxStatusCodeBasedInjection/LEVEL_11?returnTo=" +
    encoded;

  fetch(url, {
    method: "GET",
    redirect: "follow"
  })
    .then(function (response) {
      if (response.redirected) {
        window.location.href = response.url;
        return;
      }

      if (response.status === 403) {
        return response.text().then(function (message) {
          resultTitle.innerHTML =
            '<span class="resultIcon">!</span><span>Redirect Blocked</span>';
          resultMessage.textContent = message + ": " + value;
          resultBox.style.display = "block";
        });
      }
    })
    .catch(function () {
      resultTitle.innerHTML =
        '<span class="resultIcon">!</span><span>Redirect Blocked</span>';
      resultMessage.textContent = "Unable to test redirect right now.";
      resultBox.style.display = "block";
    });
}

if (button && input) {
  button.addEventListener("click", function () {
    testLevel11Redirect(input.value);
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