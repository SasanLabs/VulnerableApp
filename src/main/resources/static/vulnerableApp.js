const detail = document.querySelector('.detail');
const detailTitle = document.querySelector('.detail-title');
const master = document.querySelector('.master');
const innerMaster = document.querySelector('.inner-master');

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

function _callbackForInnerMasterOnClickEvent(vulnerableAppEndPointData, id, key, vulnerabilitySelected) {
	return function () {
		if(currentId == id && currentKey == key) {
			return;
		}
		currentId = id;
		currentKey = key;
		clearSelectedInnerMaster();
		vulnerabilityLevelSelected = vulnerableAppEndPointData[id]["Detailed Information"][key]["Level"];
		this.classList.add('active-item');
		let htmlTemplate = vulnerableAppEndPointData[id]["Detailed Information"][key]["HtmlTemplate"];
		let vulnerabilityDescription = vulnerableAppEndPointData[id]["Detailed Information"][key]["Description"];
		document.getElementById("vulnerabilityDescription").innerHTML = vulnerabilityDescription;
		let urlToFetchHtmlTemplate = "templates/" + vulnerabilitySelected + "/" +  htmlTemplate;
		let parentNodeWithAllDynamicScripts = document.getElementById("dynamicScripts");
		var dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
		//Might not require to iterate but iterating for safe side. can be removed after proper testing. 
		while (dynamicScriptNode) {
			dynamicScriptNode.remove();
			dynamicScriptNode = parentNodeWithAllDynamicScripts.lastElementChild;
		}
		doGetAjaxCall((responseText) => { detailTitle.innerHTML = responseText; _loadDynamicJSAndCSS(urlToFetchHtmlTemplate); }, urlToFetchHtmlTemplate + ".html");
	}
}

function handleFirstElementAutoSelection(vulnerableAppEndPointData) {
	if (vulnerableAppEndPointData.length > 0) {
		vulnerabilitySelected = vulnerableAppEndPointData[0]["Name"];
		detailTitle.innerHTML = vulnerableAppEndPointData[0]["Description"];
		for (let key in vulnerableAppEndPointData[0]["Detailed Information"]) {
			let column = document.createElement("div");
			let textNode = document.createTextNode(vulnerableAppEndPointData[0]["Detailed Information"][key]["Level"]);
			column.appendChild(textNode);
			column.className = "inner-master-item";
			column.addEventListener('click', _callbackForInnerMasterOnClickEvent(vulnerableAppEndPointData, 0, key, vulnerabilitySelected));
			innerMaster.appendChild(column);
		}
	}
}

function update(vulnerableAppEndPointData) {
	const masterItems = document.querySelectorAll('.master-item');
	handleFirstElementAutoSelection(vulnerableAppEndPointData);
	masterItems.forEach(item => {
		item.addEventListener('click', function () {
			clearSelectedMaster();
			this.classList.add('active-item');
			detail.classList.remove('hidden-md-down');
			innerMaster.innerHTML = "";
			vulnerabilitySelected = vulnerableAppEndPointData[this.id]["Name"];
			for (let key in vulnerableAppEndPointData[this.id]["Detailed Information"]) {
				let column = document.createElement("div");
				let textNode = document.createTextNode(vulnerableAppEndPointData[this.id]["Detailed Information"][key]["Level"]);
				column.appendChild(textNode);
				column.className = "inner-master-item";
				column.addEventListener('click', _callbackForInnerMasterOnClickEvent(vulnerableAppEndPointData, this.id, key, vulnerabilitySelected));
				innerMaster.appendChild(column);
			}
		});
	});
}

function _clearActiveItemClass(items) {
	for (let item of items) {
		//to clear out the already active item
		item.classList.remove('active-item');
	}
}

function clearSelectedMaster() {
	//console.log('Clicked item');
	const masterItems = document.querySelectorAll('.master-item');
	_clearActiveItemClass(masterItems);
}

function clearSelectedInnerMaster() {
	//console.log('Clicked item');
	const innerMasterItems = document.querySelectorAll('.inner-master-item');
	_clearActiveItemClass(innerMasterItems);
}

function back() {
	detail.classList.add('hidden-md-down');
	clearSelected();
}

function getUrlForVulnerabilityLevel() {
	return "/vulnerable/" + vulnerabilitySelected + "/" + vulnerabilityLevelSelected;
}

function doGetAjaxCallForVulnerabilityLevel(callBack, isJson) {
	let url = getUrlForVulnerabilityLevel();
	this.doGetAjaxCall(callBack, url, isJson);
}

function doGetAjaxCall(callBack, url, isJson) {
	let xmlHttpRequest = new XMLHttpRequest();
	xmlHttpRequest.onreadystatechange = function () {
		if (xmlHttpRequest.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
			if (xmlHttpRequest.status == 200) {
				if (isJson) {
					callBack(JSON.parse(xmlHttpRequest.responseText));
				} else {
					callBack(xmlHttpRequest.responseText);
				}
			}
			else if (xmlHttpRequest.status == 400) {
				alert('There was an error 400');
			}
			else {
				alert('something else other than 200 was returned');
			}
		}
	};

	xmlHttpRequest.open("GET", url, true);
	xmlHttpRequest.setRequestHeader("Content-Type", isJson ? "application/json" : "text/html");
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
		let textNode = document.createTextNode(vulnerableAppEndPointData[index]["Name"]);
		column.appendChild(textNode);
		master.appendChild(column);
	}
	update(vulnerableAppEndPointData);
}