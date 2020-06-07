function addingEventListenerToLoadImageButton() {
	document.getElementById("submit").addEventListener('click',
		function () {
			let url = getUrlForVulnerabilityLevel();
			doGetAjaxCall(appendResponseCallback, url + "?value=" + document.getElementById("textInput").value, false);
		});
};
addingEventListenerToLoadImageButton();

function appendResponseCallback(data) {
	document.getElementById("parentContainer").innerHTML = data;
}