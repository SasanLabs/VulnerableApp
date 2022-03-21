function genericResponseHandler2(xmlHttpRequest, callBack, isJson) {
  if (xmlHttpRequest.readyState == XMLHttpRequest.DONE) {
    if (xmlHttpRequest.status == 200 || xmlHttpRequest.status == 401) {
      if (isJson) {
        callBack(JSON.parse(xmlHttpRequest.responseText));
      } else {
        callBack(xmlHttpRequest.responseText);
      }
    } else if (xmlHttpRequest.status == 400) {
      alert("There was an error 400");
    } else if (xmlHttpRequest.status == 500) {
      document.getElementById("uploaded_file_info").innerHTML =
        "No space left on the device";
    } else {
      alert("something else other than 200/401 was returned");
    }
  }
}

function addingEventListenerToUploadImage() {
  document.getElementById("upload").addEventListener("click", function () {
    const form = document.getElementById("file");
    const file = form.files[0];
    if (file.size > 1048576) {
      document.getElementById("uploaded_file_info").innerHTML =
        "Max upload size exceeded exception : " +
        "the request was rejected because its size (" +
        file.size +
        ") exceeds the configured maximum (1048576)";
    } else {
      let formData = new FormData();
      formData.append("file", file);
      let url = getUrlForVulnerabilityLevel();
      let xmlHttpRequest = new XMLHttpRequest();
      xmlHttpRequest.onreadystatechange = function () {
        return genericResponseHandler2(xmlHttpRequest, uploadImage, true);
      };
      xmlHttpRequest.open("POST", url, true);
      xmlHttpRequest.send(formData);
    }
  });
}

addingEventListenerToUploadImage();

function uploadImage(data) {
  document.getElementById("uploaded_file_info").innerHTML = data.isValid
    ? "File uploaded at location:" + data.content
    : data.content;
}
