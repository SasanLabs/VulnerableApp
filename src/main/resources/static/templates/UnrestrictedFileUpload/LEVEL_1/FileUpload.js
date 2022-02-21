function addingEventListenerToUploadImage() {
  document.getElementById("upload").addEventListener("click", function () {
    var form = document.getElementById("file");
    var file = form.files[0];
    if(file.size > 10485760){
          let fileSizeMB = (file.size/1024)/1024
          fileSizeMB = fileSizeMB.toFixed(3)
          document.getElementById("uploaded_file_info").innerHTML ="File Size " + fileSizeMB + "MB Is Too Big! Please choose a smaller file"
          document.getElementById("uploaded_file_info").style.color = "red";
          return
         }else{
          document.getElementById("uploaded_file_info").innerHTML = ""
          document.getElementById("uploaded_file_info").style.color = "black";
         }
    var formData = new FormData();
    formData.append("file", file);
    let url = getUrlForVulnerabilityLevel();
    doPostAjaxCall(uploadImage, url, true, formData);
  });
}
addingEventListenerToUploadImage();

function uploadImage(data) {
  document.getElementById("uploaded_file_info").innerHTML = data.isValid
    ? "File uploaded at location:" + data.content
    : data.content;
}
