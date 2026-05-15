const input = document.getElementById("returnToInput");
const button = document.getElementById("testRedirectBtn");
const sampleLinks = document.querySelectorAll(".sample-link");

function goToLevel11(value) {
    const encoded = encodeURIComponent(value);
    window.location.href =
        "/VulnerableApp/Http3xxStatusCodeBasedInjection/LEVEL_11?returnTo=" + encoded;
}

if (button && input) {
    button.addEventListener("click", function () {
        goToLevel11(input.value);
    });
}

sampleLinks.forEach(function (link) {
    link.addEventListener("click", function (event) {
        event.preventDefault();
        const value = link.getAttribute("data-value");
        input.value = value;
        goToLevel11(value);
    });
});