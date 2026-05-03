function addingEventListenerToFetchCarInfoButton() {
  document
    .getElementById("checkIfCarPresentButton")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let id = document.getElementById("carId").value;
      let body = JSON.stringify({
        query: "query($id: String!) { car(id: $id) { id name imagePath } }",
        variables: { id: id },
      });
      doPostAjaxCall(fetchCarInfoCallBack, url, true, body, {
        "Content-Type": "application/json",
      });
    });
}
addingEventListenerToFetchCarInfoButton();

function fetchCarInfoCallBack(data) {
  if (car.isDataPresent) {
    document.getElementById("carInformation").innerHTML =
      "<div>Car is Present: " +
      car.name +
      "</div>" +
      "<img src='" +
      car.imagePath +
      "' width='900'/>";
  } else {
    document.getElementById("carInformation").innerHTML =
      "<div>Car is not Present</div>";
  }
}
