function addingEventListenerToUploadedImages() {
  document.getElementById("upload").addEventListener("click", function () {
    const totalFiles = document.getElementById("file").files.length;
    if (totalFiles > 0) {
      let formData = new FormData();
      let totalLength = 0;
      for (let i = 0; i < totalFiles; ++i) {
        let file = document.getElementById("file").files[i];
        totalLength += file.size;
        if (file.size > 10485760 || totalLength > 10485760) {
          let size;
          file.size > 10485760 ? (size = file.size) : (size = totalLength);
          document.getElementById("uploaded_files_info").innerHTML =
            "Max upload size exceeded exception : " +
            "the request was rejected because its size (" +
            size +
            ") exceeds the configured maximum (10485760)";
          return;
        } else {
          formData.append("file", file);
        }
      }
      let url = getUrlForVulnerabilityLevel();
      doPostAjaxCall(uploadImages, url, true, formData);
    }
  });
}

addingEventListenerToUploadedImages();

function uploadImages(data) {
  document.getElementById("uploaded_files_info").innerHTML = data.isValid
    ? "Files uploaded at location:" + data.content
    : data.content;
}
