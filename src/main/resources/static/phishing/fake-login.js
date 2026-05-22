function harvest() {
  var user = document.getElementById("username").value;
  var pass = document.getElementById("password").value;
  if (!user && !pass) return;
  // Attacker receives credentials here
  console.log(
    "[Phishing] Harvested credentials — username: " +
      user +
      ", password: " +
      pass
  );
  document.getElementById("password").value = "";
  document.getElementById("errorMsg").style.display = "block";
}
