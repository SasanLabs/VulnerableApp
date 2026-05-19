function buildPingRequest() {
  let ip = document.getElementById("ipAddressInput").value;
  let batched = document.getElementById("batchMode").checked;
  if (!batched) {
    return {
      query:
        "mutation($ip: String!) { runPing(ipaddress: $ip) { output ipaddress } }",
      variables: { ip: ip },
    };
  }
  // GraphQL-specific amplification: one request, five aliased calls — the injected
  // command runs five times even though the schema looks like a single mutation.
  let aliases = ["a", "b", "c", "d", "e"]
    .map((k) => `${k}: runPing(ipaddress: $ip) { output }`)
    .join("\n");
  return {
    query: `mutation($ip: String!) { ${aliases} }`,
    variables: { ip: ip },
  };
}

function addingEventListenerToRunPingButton() {
  document
    .getElementById("runPingButton")
    .addEventListener("click", function () {
      let url = getUrlForVulnerabilityLevel();
      let body = JSON.stringify(buildPingRequest());
      doPostAjaxCall(pingCallBack, url, true, body, {
        "Content-Type": "application/json",
      });
    });
}
addingEventListenerToRunPingButton();

function pingCallBack(response) {
  document.getElementById("pingResult").textContent = JSON.stringify(
    response,
    null,
    2
  );
}
