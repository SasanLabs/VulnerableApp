function addEventListenerToSearchButton() {
    document.getElementById("searchButton").addEventListener("click", function () {
        let username = document.getElementById("username").value;
        let url = getUrlForVulnerabilityLevel() + "?username=" + encodeURIComponent(username);
        doGetAjaxCall(updateUIWithResponse, url, true);
    });
}

function updateUIWithResponse(data) {
    if (data.isValid) {
        document.getElementById("resultContainer").innerHTML = JSON.stringify(JSON.parse(data.content), null, 4);
    } else {
        document.getElementById("resultContainer").innerHTML = "Error: " + data.content;
    }
}

addEventListenerToSearchButton();
