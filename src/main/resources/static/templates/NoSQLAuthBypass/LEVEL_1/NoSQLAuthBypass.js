function maybeParseJson(raw) {
  try {
    return JSON.parse(raw);
  } catch (e) {
    return raw;
  }
}

function buildLoginRequest() {
  let level = getCurrentVulnerabilityLevel();
  let usernameInput = document.getElementById("usernameInput").value;
  let passwordInput = document.getElementById("passwordInput").value;
  if (level === "LEVEL_1") {
    return {
      query:
        "query($u: Any!, $p: Any!) { login(username: $u, password: $p) { loggedIn username } }",
      variables: {
        u: maybeParseJson(usernameInput),
        p: maybeParseJson(passwordInput),
      },
    };
  }
  return {
    query:
      "query($u: String!, $p: String!) { login(username: $u, password: $p) { loggedIn username } }",
    variables: { u: usernameInput, p: passwordInput },
  };
}

function addingEventListenerToLoginButton() {
  document.getElementById("loginButton").addEventListener("click", function () {
    let url = getUrlForVulnerabilityLevel();
    let body = JSON.stringify(buildLoginRequest());
    doPostAjaxCall(loginCallBack, url, true, body, {
      "Content-Type": "application/json",
    });
  });
}
addingEventListenerToLoginButton();

function loginCallBack(response) {
  let login = response && response.data && response.data.login;
  if (login && login.loggedIn) {
    document.getElementById("loginResult").innerHTML =
      "<div>Logged in as " + login.username + "</div>";
  } else {
    document.getElementById("loginResult").innerHTML =
      "<div>Invalid credentials</div>";
  }
}
