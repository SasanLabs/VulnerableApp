function updatePlaceholderDiv() {
  var returnTo = "/VulnerableApp/phishing/fake-login.html";
  var url = getUrlForVulnerabilityLevel() + "?returnTo=" + returnTo;

  document
    .getElementById("openRedirectBtn")
    .addEventListener("click", function () {
      document.getElementById("redirectUrlBox").textContent = returnTo;
      document.getElementById("redirectModal").style.display = "flex";

      document.getElementById("continueBtn").onclick = function () {
        window.open(url, "_blank");
        document.getElementById("redirectModal").style.display = "none";
      };

      document.getElementById("closeBtn").onclick = function () {
        document.getElementById("redirectModal").style.display = "none";
      };
    });
}

updatePlaceholderDiv();
