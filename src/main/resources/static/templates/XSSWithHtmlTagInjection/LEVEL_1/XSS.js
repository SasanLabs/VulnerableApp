function addingEventListenerToLoadImageButton() {
  document.getElementById("submit").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    doGetAjaxCall(
      appendResponseCallback,
      url + "?value=" + document.getElementById("textInput").value,
      false
    );
  });
}
addingEventListenerToLoadImageButton();

function appendResponseCallback(data) {
  var parentContainer = document.getElementById("parentContainer");
  parentContainer.innerHTML = data;
  if (parentContainer.childNodes.length > 0) {
    parentContainer.childNodes[0].classList.add(
      document.getElementById("fonts").value
    );
  }
}
