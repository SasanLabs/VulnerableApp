const detail = document.querySelector(".detail");
const detailTitle = document.querySelector(".detail-title");
const master = document.querySelector(".master");
const innerMaster = document.querySelector(".inner-master");

const variantTooltip = {
  secure: "Secure implementation",
  unsecure: "Unsecure implementation",
};

let vulnerabilitySelected = "";
let vulnerabilityLevelSelected = "";

let currentId;
let currentKey;

function _loadDynamicJSAndCSS(urlToFetchHtmlTemplate) {
  let dynamicScriptsElement = document.getElementById("dynamicScripts");
  let cssElement = document.createElement("link");
  cssElement.href = urlToFetchHtmlTemplate + ".css";
  cssElement.type = "text/css";
  cssElement.rel = "stylesheet";
  dynamicScriptsElement.appendChild(cssElement);
  if (urlToFetchHtmlTemplate === "error") {
    document.getElementById("hideHelp").style.display = "none";
    document.getElementById("showHelp").style.display = "none";
  } else {
    document.getElementById("hideHelp").style.display = "inline-block";
    document.getElementById("showHelp").style.display = "inline-block";
    let jsElement = document.createElement("script");
    jsElement.type = "module";
    jsElement.src = urlToFetchHtmlTemplate + ".js?p=" + new Date().getTime();
    dynamicScriptsElement.appendChild(jsElement);
  }
}

function _callbackForInnerMasterOnClickEvent(
  vulnerableAppEndPointData,
  id,
  key,
  vulnerabilitySelected
) {
  return function () {
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
    document.getElementById("vulnerabilityDescription").innerHTML =
      vulnerableAppEndPointData[id]["Description"];
    let urlToFetchHtmlTemplate = htmlTemplate
      ? "/VulnerableApp/templates/" + vulnerabilitySelected + "/" + htmlTemplate
      : "error";
    let parentNodeWithAllDynamicScripts = document.getElementById(
      "dynamicScripts"
    );
    let dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
    //Might not require to iterate but iterating for safe side. can be removed after proper testing.
    while (dynamicScriptNode) {
      dynamicScriptNode.remove();
      dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
    }
    doGetAjaxCall((responseText) => {
      detailTitle.innerHTML = responseText;
      _loadDynamicJSAndCSS(urlToFetchHtmlTemplate);
    }, urlToFetchHtmlTemplate + ".html");
  };
}

function _isSecureVariant(detailedInformation) {
  return detailedInformation["Variant"] === "SECURE";
}

function _getSvgElementForVariant(isSecure) {
  let svg = document.createElement("img");
  svg.classList.add("vector");
  svg.classList.add("tooltip");
  const svgVariantName = isSecure ? "secure" : "unsecure";

  svg.setAttribute("src", "vectors/" + svgVariantName + ".svg");

  return svg;
}

function createColumn(detailedInformationArray, key) {
  let detailedInformation = detailedInformationArray[key];
  let isSecure = _isSecureVariant(detailedInformation);

  let column = document.createElement("div");
  column.id = "0." + key;

  let svgWithTooltip = document.createElement("div");
  svgWithTooltip.classList.add("tooltip");

  let span = document.createElement("span");
  span.classList.add("tooltip-text");
  span.classList.add(
    isSecure ? "secure-variant-tooltip-text" : "unsecure-variant-tooltip-text"
  );
  span.innerHTML = isSecure ? variantTooltip.secure : variantTooltip.unsecure;

  svgWithTooltip.appendChild(span);
  svgWithTooltip.appendChild(_getSvgElementForVariant(isSecure));
  column.appendChild(svgWithTooltip);

  column.appendChild(document.createTextNode(detailedInformation["Level"]));
  column.classList.add("inner-master-item");

  if (isSecure) {
    column.classList.add("secure-vulnerability");
  }

  return column;
}

function appendNewColumn(vulnerableAppEndPointData, id) {
  let detailedInformationArray =
    vulnerableAppEndPointData[id]["Detailed Information"];
  let isFirst = true;

  for (let key in detailedInformationArray) {
    if (!detailedInformationArray.hasOwnProperty(key)) {
      continue;
    }
    let column = createColumn(detailedInformationArray, key);
    column.addEventListener(
      "click",
      _callbackForInnerMasterOnClickEvent(
        vulnerableAppEndPointData,
        id,
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

/**
 * Replace vulnerability description and append a column with all of the vulnerability levels.
 * Each level is assigned with an event listener that aims to handle specific level selection.
 * @param {Object} vulnerableAppEndPointData - data from the API describing the vulnerability
 * @param {number} id - master-item identifier
 */
function handleElementAutoSelection(vulnerableAppEndPointData, id = 0) {
  if (!vulnerableAppEndPointData.length) {
    return;
  }

  if (id === 0) {
    detailTitle.innerHTML = vulnerableAppEndPointData[id]["Description"];
  } else {
    innerMaster.innerHTML = "";
  }

  vulnerabilitySelected = vulnerableAppEndPointData[id]["Name"];
  detailTitle.innerHTML = vulnerableAppEndPointData[id]["Description"];
  appendNewColumn(vulnerableAppEndPointData, id);
}

function update(vulnerableAppEndPointData) {
  const masterItems = document.querySelectorAll(".master-item");
  handleElementAutoSelection(vulnerableAppEndPointData, 0);
  masterItems.forEach((item) => {
    item.addEventListener("click", function () {
      clearSelectedMaster();
      this.classList.add("active-item");
      detail.classList.remove("hidden-md-down");
      handleElementAutoSelection(vulnerableAppEndPointData, this.id);
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
    "/VulnerableApp/" + vulnerabilitySelected + "/" + vulnerabilityLevelSelected
  );
}

function genericResponseHandler(xmlHttpRequest, callBack, isJson) {
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
}

function doGetAjaxCall(callBack, url, isJson) {
  let xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function () {
    genericResponseHandler(xmlHttpRequest, callBack, isJson);
  };
  xmlHttpRequest.open("GET", url, true);
  xmlHttpRequest.setRequestHeader(
    "Content-Type",
    isJson ? "application/json" : "text/html"
  );
  xmlHttpRequest.send();
}

function doPostAjaxCall(callBack, url, isJson, data) {
  let xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function () {
    return genericResponseHandler(xmlHttpRequest, callBack, isJson);
  };
  xmlHttpRequest.open("POST", url, true);
  xmlHttpRequest.send(data);
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
  document.getElementById("showHelp").addEventListener("click", function () {
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

  document.getElementById("hideHelp").addEventListener("click", function () {
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
      document.getElementById("chooseMode").classList.add("hide-component");
    });

  document.getElementById("testScannerBtn").addEventListener("click", () => {
    document.getElementById("testScanner").classList.remove("hide-component");
    document.getElementById("learnAndPractice").classList.add("hide-component");
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

  //  document.getElementById("about").addEventListener("click", () => {
  //    document.getElementById("aboutContainer").scrollIntoView(true);
  //  });
})();
