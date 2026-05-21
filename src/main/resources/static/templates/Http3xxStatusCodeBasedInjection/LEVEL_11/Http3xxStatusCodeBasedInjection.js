const input = document.getElementById("returnToInput");
const button = document.getElementById("testRedirectBtn");
const sampleLinks = document.querySelectorAll(".sample-link");
const resultBox = document.getElementById("resultBox");

function testLevel11Redirect(value) {
  const encoded = encodeURIComponent(value);
  const url =
    "/VulnerableApp/Http3xxStatusCodeBasedInjection/LEVEL_11?returnTo=" +
    encoded;

  fetch(url, {
    method: "GET",
    redirect: "manual"
  })
    .then(function (response) {
      if (response.status === 403) {
        return response.text().then(function (message) {
          resultBox.textContent = message + ": " + value;
          resultBox.style.display = "block";
        });
      }

      if (response.status >= 300 && response.status < 400) {
        const location = response.headers.get("Location");
        if (location) {
          window.location.href = location;
        }
      }
    })
    .catch(function () {
      resultBox.textContent = "Unable to test redirect right now.";
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
    testLevel11Redirect(value);
  });
});