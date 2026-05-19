function addingEventListenersToCriteriaQBButtons() {
  document.getElementById("qbFilterBtn").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    let field = document.getElementById("qbField").value;
    let value = document.getElementById("qbValue").value;
    doGetAjaxCall(
      criteriaQBCallback,
      url +
        "?field=" +
        encodeURIComponent(field) +
        "&value=" +
        encodeURIComponent(value),
      true
    );
  });

  document.getElementById("qbSortBtn").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    let sort = document.getElementById("qbSort").value;
    doGetAjaxCall(
      criteriaQBCallback,
      url + "?sort=" + encodeURIComponent(sort),
      true
    );
  });
}
addingEventListenersToCriteriaQBButtons();

function criteriaQBCallback(data) {
  let target = document.getElementById("qbResponse");
  if (!data || !data.content || data.content.length === 0) {
    target.innerText = "No matching employees.";
    return;
  }
  let rows = data.content
    .map(function (e) {
      return e.id + " | " + e.name + " | " + e.department;
    })
    .join("\n");
  target.innerText = rows;
}
