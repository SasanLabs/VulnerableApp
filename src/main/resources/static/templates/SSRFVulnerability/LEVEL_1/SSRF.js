function addEventListenerToShowButton() {
  document.getElementById("showBtn").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    console.log(url + "?imageurl=" + document.getElementById("urlInput").value);
    doGetAjaxCall(
      getImageData,
      url + "?imageurl=" + document.getElementById("urlInput").value,
      true
    );
  });
}
addEventListenerToShowButton();

function getImageData(data) {
  let imageResponseDiv = document.getElementById("imageResponse");
  while (
    imageResponseDiv.firstChild &&
    imageResponseDiv.removeChild(imageResponseDiv.firstChild)
  );
  if (!data.content) {
    document
      .getElementById("imageResponse")
      .append("That URL couldn't be processed.");
  } else {
    if (data.content.charAt(0) == "/") {
      var img = document.createElement("img");
      img.src = "data:image/jpeg;base64," + data.content;
      img.style.width = "500px";
      img.style.height = "auto";
      document.getElementById("imageResponse").append(img);
    } else if (data.content.charAt(0) == "i") {
      var img = document.createElement("img");
      img.src = "data:image/png;base64," + data.content;
      img.style.width = "500px";
      img.style.height = "auto";
      document.getElementById("imageResponse").append(img);
    } else {
      document.getElementById("imageResponse").append(atob(data.content));
    }
  }
}
