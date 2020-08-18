function doPostAjaxCall(callBack, url, isJson, data) {
  let xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (xmlHttpRequest.readyState == XMLHttpRequest.DONE) {
      // XMLHttpRequest.DONE == 4
      if (xmlHttpRequest.status == 200 || xmlHttpRequest.status == 401) {
        if (isJson) {
          callBack(JSON.parse(xmlHttpRequest.responseText));
        } else {
          callBack(xmlHttpRequest.responseText);
        }
      } else if (xmlHttpRequest.status == 400) {
        alert("There was an error 400");
      } else {
        alert("something else other than 200/401 was returned");
      }
    }
  };

  xmlHttpRequest.open("POST", url, true);
  xmlHttpRequest.setRequestHeader(
    "Content-Type",
    isJson ? "application/json" : "text/html"
  );
  xmlHttpRequest.send(data);
}

function addingEventListenerToFetchCarInfoButton() {
  document
    .getElementById("fetchCarImageButton")
    .addEventListener("click", function() {
      var form = document.getElementById("sasan");
      var file = form.files[0];
      var formData = new FormData();
      formData.append("file", file);
      let url = getUrlForVulnerabilityLevel();
      doPostAjaxCall(fetchCarInfoCallBack, url, true, formData);
    });
}
addingEventListenerToFetchCarInfoButton();

function fetchCarInfoCallBack(data) {
  document.getElementById("carInformation").innerHTML =
    "<img src='" + data.imagePath + "' width='900'/>";
}
