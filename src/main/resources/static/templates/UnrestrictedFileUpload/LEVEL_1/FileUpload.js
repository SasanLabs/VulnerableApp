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
  xmlHttpRequest.send(data);
}

function addingEventListenerToFetchCarInfoButton() {
  document
    .getElementById("upload")
    .addEventListener("click", function() {
      var form = document.getElementById("file");
      var file = form.files[0];
      var formData = new FormData();
      formData.append("file", file);
      let url = getUrlForVulnerabilityLevel();
      doPostAjaxCall(fetchCarInfoCallBack, url, false, formData);
    });
}
addingEventListenerToFetchCarInfoButton();

function fetchCarInfoCallBack(data) {
  document.getElementById("uploaded_file_info").innerHTML = "File uploaded at location:" + data;
}
