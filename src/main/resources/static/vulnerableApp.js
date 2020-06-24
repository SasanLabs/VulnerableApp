const detail = document.querySelector(".detail");
const detailTitle = document.querySelector(".detail-title");
const master = document.querySelector(".master");
const innerMaster = document.querySelector(".inner-master");

let vulnerabilitySelected = "";
let vulnerabilityLevelSelected = "";

let currentId;
let currentKey;

function _loadDynamicJSAndCSS(urlToFetchHtmlTemplate) {
  let dynamicScriptsElement = document.getElementById("dynamicScripts");
  let jsElement = document.createElement("script");
  jsElement.type = "module";
  jsElement.src = urlToFetchHtmlTemplate + ".js?p=" + new Date().getTime();
  dynamicScriptsElement.appendChild(jsElement);
  let cssElement = document.createElement("link");
  cssElement.href = urlToFetchHtmlTemplate + ".css";
  cssElement.type = "text/css";
  cssElement.rel = "stylesheet";
  dynamicScriptsElement.appendChild(cssElement);
}

function _callbackForInnerMasterOnClickEvent(
  vulnerableAppEndPointData,
  id,
  key,
  vulnerabilitySelected
) {
  return function() {
    if (currentId == id && currentKey == key) {
      return;
    }
    currentId = id;
    currentKey = key;
    clearSelectedInnerMaster();
    vulnerabilityLevelSelected =
      vulnerableAppEndPointData[id]["Detailed Information"][key]["Level"];
    this.classList.add("active-item");
    let htmlTemplate =
      vulnerableAppEndPointData[id]["Detailed Information"][key][
        "HtmlTemplate"
      ];
    let vulnerabilityDescription =
      vulnerableAppEndPointData[id]["Detailed Information"][key]["Description"];
    document.getElementById("vulnerabilityDescription").innerHTML =
      vulnerableAppEndPointData[id]["Description"];
    document.getElementById(
      "vulnerabilityLevelDescription"
    ).innerHTML = vulnerabilityDescription;
    let urlToFetchHtmlTemplate = htmlTemplate
      ? "templates/" + vulnerabilitySelected + "/" + htmlTemplate
      : "sasan";
    let parentNodeWithAllDynamicScripts = document.getElementById(
      "dynamicScripts"
    );
    let dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
    //Might not require to iterate but iterating for safe side. can be removed after proper testing.
    while (dynamicScriptNode) {
      dynamicScriptNode.remove();
      dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
    }
    doGetAjaxCall(responseText => {
      detailTitle.innerHTML = responseText;
      _loadDynamicJSAndCSS(urlToFetchHtmlTemplate);
    }, urlToFetchHtmlTemplate + ".html");
  };
}

function handleFirstElementAutoSelection(vulnerableAppEndPointData) {
  if (vulnerableAppEndPointData.length > 0) {
    vulnerabilitySelected = vulnerableAppEndPointData[0]["Name"];
    detailTitle.innerHTML = vulnerableAppEndPointData[0]["Description"];
    let isFirst = true;
    for (let key in vulnerableAppEndPointData[0]["Detailed Information"]) {
      let column = document.createElement("div");
      let textNode = document.createTextNode(
        vulnerableAppEndPointData[0]["Detailed Information"][key]["Level"]
      );
      column.appendChild(textNode);
      column.className = "inner-master-item";
      column.addEventListener(
        "click",
        _callbackForInnerMasterOnClickEvent(
          vulnerableAppEndPointData,
          0,
          key,
          vulnerabilitySelected
        )
      );
      if (isFirst) {
        column.click();
        isFirst = false;
      }
      innerMaster.appendChild(column);
    }
  }
}

function update(vulnerableAppEndPointData) {
  const masterItems = document.querySelectorAll(".master-item");
  handleFirstElementAutoSelection(vulnerableAppEndPointData);
  masterItems.forEach(item => {
    item.addEventListener("click", function() {
      clearSelectedMaster();
      this.classList.add("active-item");
      detail.classList.remove("hidden-md-down");
      innerMaster.innerHTML = "";
      vulnerabilitySelected = vulnerableAppEndPointData[this.id]["Name"];
      let isFirst = true;
      for (let key in vulnerableAppEndPointData[this.id][
        "Detailed Information"
      ]) {
        let column = document.createElement("div");
        let textNode = document.createTextNode(
          vulnerableAppEndPointData[this.id]["Detailed Information"][key][
            "Level"
          ]
        );
        column.appendChild(textNode);
        column.className = "inner-master-item";
        column.addEventListener(
          "click",
          _callbackForInnerMasterOnClickEvent(
            vulnerableAppEndPointData,
            this.id,
            key,
            vulnerabilitySelected
          )
        );
        if (isFirst) {
          column.click();
          isFirst = false;
        }
        innerMaster.appendChild(column);
      }
    });
  });
  _addingEventListenerToShowHideHelpButton(vulnerableAppEndPointData);
}

function _clearActiveItemClass(items) {
  for (let item of items) {
    //to clear out the already active item
    item.classList.remove("active-item");
  }
}

function clearSelectedMaster() {
  //console.log('Clicked item');
  const masterItems = document.querySelectorAll(".master-item");
  //Making back to learning vulnerability
  document.getElementById("vulnLearnBtn").click();
  _clearActiveItemClass(masterItems);
  _clearHelp();
}

function clearSelectedInnerMaster() {
  //console.log('Clicked item');
  const innerMasterItems = document.querySelectorAll(".inner-master-item");
  _clearActiveItemClass(innerMasterItems);
  _clearHelp();
}

function back() {
  detail.classList.add("hidden-md-down");
  clearSelected();
}

function getUrlForVulnerabilityLevel() {
  return (
    "/vulnerable/" + vulnerabilitySelected + "/" + vulnerabilityLevelSelected
  );
}

function doGetAjaxCallForVulnerabilityLevel(callBack, isJson) {
  let url = getUrlForVulnerabilityLevel();
  this.doGetAjaxCall(callBack, url, isJson);
}

function doGetAjaxCall(callBack, url, isJson) {
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

  xmlHttpRequest.open("GET", url, true);
  xmlHttpRequest.setRequestHeader(
    "Content-Type",
    isJson ? "application/json" : "text/html"
  );
  xmlHttpRequest.send();
}

function generateMasterDetail(vulnerableAppEndPointData) {
  let isFirst = true;
  for (let index in vulnerableAppEndPointData) {
    let column = document.createElement("div");
    if (isFirst) {
      column.className = "master-item  active-item";
      isFirst = false;
    } else {
      column.className = "master-item";
    }
    column.id = index;
    let textNode = document.createTextNode(
      vulnerableAppEndPointData[index]["Name"]
    );
    column.appendChild(textNode);
    master.appendChild(column);
  }
  update(vulnerableAppEndPointData);
}

function _clearHelp() {
  document.getElementById("showHelp").disabled = false;
  document.getElementById("helpText").innerHTML = "";
  document.getElementById("hideHelp").disabled = true;
}

function _addingEventListenerToShowHideHelpButton(vulnerableAppEndPointData) {
  document.getElementById("showHelp").addEventListener("click", function() {
    document.getElementById("showHelp").disabled = true;
    let helpText = "<ol>";
    for (let index in vulnerableAppEndPointData[currentId][
      "Detailed Information"
    ][currentKey]["AttackVectors"]) {
      let attackVector =
        vulnerableAppEndPointData[currentId]["Detailed Information"][
          currentKey
        ]["AttackVectors"][index];
      let curlPayload = attackVector["CurlPayload"];
      let description = attackVector["Description"];
      helpText =
        helpText +
        "<li><b>Description about the attack:</b> " +
        description +
        "<br/><b>Payload:</b> " +
        curlPayload +
        "</li>";
    }
    helpText = helpText + "</ol>";
    document.getElementById("helpText").innerHTML = helpText;
    document.getElementById("hideHelp").disabled = false;
  });

  document.getElementById("hideHelp").addEventListener("click", function() {
    _clearHelp();
  });
}

/**
 * Autoregistered Event listeners
 */
(function _autoRegister() {
  document
    .getElementById("learnAndPracticeBtn")
    .addEventListener("click", () => {
      document.getElementById("testScanner").classList.add("hide-component");
      document
        .getElementById("learnAndPractice")
        .classList.remove("hide-component");
      document.getElementById("analyseReport").classList.add("hide-component");
      document.getElementById("chooseMode").classList.add("hide-component");
    });

  document.getElementById("testScannerBtn").addEventListener("click", () => {
    document.getElementById("testScanner").classList.remove("hide-component");
    document.getElementById("learnAndPractice").classList.add("hide-component");
    document.getElementById("analyseReport").classList.add("hide-component");
    document.getElementById("chooseMode").classList.add("hide-component");
  });

  document.getElementById("analyseReportBtn").addEventListener("click", () => {
    document.getElementById("analyseReport").classList.remove("hide-component");
    document.getElementById("learnAndPractice").classList.add("hide-component");
    document.getElementById("testScanner").classList.add("hide-component");
    document.getElementById("chooseMode").classList.add("hide-component");
  });

  document.getElementById("vulnPracticeBtn").addEventListener("click", () => {
    document.getElementById("vulnPractice").classList.remove("hide-component");
    document
      .getElementById("vulnerabilityDescription")
      .classList.add("hide-component");
    document.getElementById("vulnLearnBtn").classList.remove("hide-component");
    document.getElementById("vulnPracticeBtn").classList.add("hide-component");
  });

  document.getElementById("vulnLearnBtn").addEventListener("click", () => {
    document.getElementById("vulnPractice").classList.add("hide-component");
    document
      .getElementById("vulnerabilityDescription")
      .classList.remove("hide-component");
    document
      .getElementById("vulnPracticeBtn")
      .classList.remove("hide-component");
    document.getElementById("vulnLearnBtn").classList.add("hide-component");
  });

  document
    .getElementById("uploadScannerReport")
    .addEventListener("click", () => {
      //Do a post call to the vulnerableApp and compare the results.
    });
  document.getElementById("about").addEventListener("click", () => {
    document.getElementById("aboutContainer").scrollIntoView(true);
  });
})();
