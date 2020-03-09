const detail = document.querySelector('.detail');
const detailTitle = document.querySelector('.detail-title');
const master = document.querySelector('.master');
const innerMaster = document.querySelector('.inner-master');


function handleFirstElementAutoSelection(vulnerableAppEndPointData) {
	if (vulnerableAppEndPointData.length > 0) {
		detailTitle.innerHTML = vulnerableAppEndPointData[0]["Description"];
		for (let key in vulnerableAppEndPointData[0]["Detailed Information"]) {
			let column = document.createElement("div");
			let textNode = document.createTextNode(vulnerableAppEndPointData[0]["Detailed Information"][key]["Level"]);
			column.appendChild(textNode);
			column.className = "inner-master-item";
			column.addEventListener('click', function () {
				clearSelectedInnerMaster();
				this.classList.add('active-item');
				doGetAjaxCall((responseText) => { detailTitle.innerHTML = responseText; }, "templates/" + vulnerableAppEndPointData[0]["Detailed Information"][key]["HtmlTemplate"]);
			});
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
			for (let key in vulnerableAppEndPointData[this.id]["Detailed Information"]) {
				let column = document.createElement("div");
				let textNode = document.createTextNode(vulnerableAppEndPointData[this.id]["Detailed Information"][key]["Level"]);
				column.appendChild(textNode);
				column.className = "inner-master-item";
				let that = this;
				column.addEventListener('click', function () {
					clearSelectedInnerMaster();
					this.classList.add('active-item');
					doGetAjaxCall((responseText) => { detailTitle.innerHTML = responseText; }, "templates/" + vulnerableAppEndPointData[that.id]["Detailed Information"][key]["HtmlTemplate"]);
				});
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



/*
function doAjaxCall(tableElement) {
	let xmlHttpRequest = new XMLHttpRequest();
	xmlHttpRequest.onreadystatechange = function() {
        if (xmlHttpRequest.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
           if (xmlHttpRequest.status == 200) {
        	   let vulnerableAppEndPointData = JSON.parse(xmlHttpRequest.responseText);
               generateTableFromJson(tableElement, vulnerableAppEndPointData);
           }
           else if (xmlHttpRequest.status == 400) {
              alert('There was an error 400');
           }
           else {
               alert('something else other than 200 was returned');
           }
        }
    };

    xmlHttpRequest.open("GET", "allEndPointJson", true);
    xmlHttpRequest.setRequestHeader("Content-Type", "application/json");
    xmlHttpRequest.send();
} */

/*
function generateTableFromJson(tableElement, vulnerableAppEndPointData) {
	let row = document.createElement("tr");
	if(vulnerableAppEndPointData.length > 0) {
		for(let key in vulnerableAppEndPointData[0]) {
			let headerColumn = document.createElement("th");
			headerColumn.setAttribute("style", "border-width:1px; border-spacing:2px; border-style:solid");
			let textNode = document.createTextNode(key);
			headerColumn.appendChild(textNode);
			row.appendChild(headerColumn);
		}
	}
	tableElement.appendChild(row);

	for(let index in vulnerableAppEndPointData) {
		row = document.createElement("tr");
		for(let key in vulnerableAppEndPointData[index]) {
			let column = document.createElement("td");
			column.setAttribute("style", "border-width:1px; border-spacing:2px; border-style:solid");
			if(key !== "Detailed Information") {
				let textNode = document.createTextNode(vulnerableAppEndPointData[index][key]);
				column.setAttribute("align", "center");
				column.appendChild(textNode);
			} else {
				var levels = vulnerableAppEndPointData[index][key];
				let orderedList = document.createElement("ol");
				for(let levelIndex in levels) {
					let list = document.createElement("li");
					let linkNode = document.createElement("a");
					let linkText = document.createTextNode(levels[levelIndex]["Description"]);
					linkNode.appendChild(linkText);
					linkNode.title = levels[levelIndex]["Description"];
					linkNode.href = "/vulnerable/" + vulnerableAppEndPointData[index]["Name"] + "/" + levels[levelIndex]["Level"];
					list.append(linkNode);
					orderedList.append(list);
				}
				column.append(orderedList);
			}
			row.appendChild(column);
		}
		tableElement.appendChild(row);
	}
}
*/