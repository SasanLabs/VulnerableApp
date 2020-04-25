function addingEventListenerToLoadImageButton() {
	document.getElementById("loadImage").addEventListener('click',
		function () {
			let url = getUrlForVulnerabilityLevel();
			doGetAjaxCall(appendResponseCallback, url + "?value=images/" + document.getElementById("imageInputSrc").value, false);
		});
};
addingEventListenerToLoadImageButton();

function appendResponseCallback(data) {
	let div = document.createElement("div");
	document.getElementById("image").appendChild(div);
	div.innerHTML = data;
}