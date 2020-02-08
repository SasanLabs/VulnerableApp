function generateTable() {
	let tableElement = document.createElement("table");
	tableElement.setAttribute("align", "center");
	doAjaxCall(tableElement);
	document.getElementById("app").appendChild(tableElement);
};


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
}

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