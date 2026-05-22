//console.log("phishing.js loaded");

function initPhishing() {
  var mailList = document.getElementById("mailList");
  var previewSubject = document.getElementById("mailPreviewSubject");
  var previewBody = document.getElementById("mailPreviewBody");
  
  // Diagnostic log
  console.log("=== PHISHING PAGE LOADED ===");
  console.log("mailList element found:", !!mailList);
  console.log("previewSubject element found:", !!previewSubject);
  console.log("previewBody element found:", !!previewBody);
  
  if (!mailList) {
    console.error("CRITICAL: mailList element not found!");
    if (previewBody) previewBody.textContent = "ERROR: mailList element not found in HTML";
    return;
  }

  // Function to convert URLs and email addresses in the mail body to clickable links
  function linkify(inputText) {
    var replacedText, replacePattern1, replacePattern2, replacePattern3;

    //URLs starting with http://, https://, or ftp://
    replacePattern1 = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
    replacedText = inputText.replace(replacePattern1, '<a href="$1" target="_blank">$1</a>');

    //URLs starting with "www." (without // before it, or it'd re-link the ones done above).
    replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
    replacedText = replacedText.replace(replacePattern2, '$1<a href="http://$2" target="_blank">$2</a>');

    //Change email addresses to mailto:: links.
    replacePattern3 = /(([a-zA-Z0-9\-\_\.])+@[a-zA-Z\_]+?(\.[a-zA-Z]{2,6})+)/gim;
    replacedText = replacedText.replace(replacePattern3, '<a href="mailto:$1">$1</a>');

    return replacedText;
  }

  
  var selectedMailId = localStorage.getItem("selectedMailId");

  // Initial mail contents
  var mailContents = [
    {
      subject: "[Action Required] Your account has been updated",
      sender: "acme@support.com",
      body:
        "Hi there,\n\nWe have updated your account settings. Please review the details and confirm your login information to avoid interruption.\n\nThanks,\nSupport Team",
    },
    {
      subject: "Invoice attached for your recent purchase",
      sender: "billing@tvshow.com",
      body:
        "Dear Customer,\n\nYour invoice for the recent subscription payment is attached. Please open the link to verify the transaction and download your receipt.\n\nBest regards,\nBilling Department",
    },
    {
      subject: "Urgent security alert: verify your mailbox now",
      sender: "security@company.com",
      body:
        "Hello,\n\nWe detected unusual activity on your account. Click the verification link immediately to secure your mailbox and prevent unauthorized access.\n\nSecurity Team",
    },
  ];

  function updatePreview(id) {
    var index = Number(id);
    if (!Number.isInteger(index) || index < 0 || index >= mailContents.length) {
      return;
    }
    previewSubject.textContent = mailContents[index].subject;
    previewBody.innerHTML = linkify(mailContents[index].body);
  }

  // Set selected button and update preview
  function setSelectedButton(button) {
    var buttons = document.querySelectorAll(".mailItem");
    buttons.forEach(function (item) {
      item.classList.remove("selected");
    });
    button.classList.add("selected");
    localStorage.setItem("selectedMailId", button.dataset.mailId);
    updatePreview(button.dataset.mailId);
  }

  // Create mail buttons
  function createMailButton(mail, index) {
    var listItem = document.createElement("li");
    var button = document.createElement("button");
    button.type = "button";
    button.className = "mailItem";
    button.dataset.mailId = String(index);

    var subjectSpan = document.createElement("span");
    subjectSpan.className = "mailSubject";
    subjectSpan.textContent = mail.subject;

    var senderSpan = document.createElement("span");
    senderSpan.className = "mailSender";
    senderSpan.textContent = mail.sender;

    button.appendChild(subjectSpan);
    button.appendChild(senderSpan);
    button.addEventListener("click", function () {
      setSelectedButton(button);
    });

    listItem.appendChild(button);
    mailList.appendChild(listItem);
    return button;
  }

  // Create buttons for initial mail contents
  mailContents.forEach(createMailButton);

  // Add Mail Form Handler
  var addMailBtn = document.getElementById("addMailBtn");
  var newMailSubject = document.getElementById("newMailSubject");
  var newMailSender = document.getElementById("newMailSender");
  var newMailBody = document.getElementById("newMailBody");

  if (addMailBtn) {
    addMailBtn.addEventListener("click", function () {
      var subject = newMailSubject.value.trim();
      var sender = newMailSender.value.trim();
      var body = newMailBody.value.trim();

      if (!subject || !sender || !body) {
        alert("Please fill in all fields");
        return;
      }

      // Add new mail to mailContents
      var newMail = {
        subject: subject,
        sender: sender,
        body: body,
      };
      mailContents.push(newMail);
      var newIndex = mailContents.length - 1;

      // Create button for new mail
      var newButton = createMailButton(newMail, newIndex);

      // Select the new mail
      setSelectedButton(newButton);

      // Clear form
      newMailSubject.value = "";
      newMailSender.value = "";
      newMailBody.value = "";

      console.log("New mail added. Total mails:", mailContents.length);
    });
  }

  if (selectedMailId !== null) {
    var selectedButton = document.querySelector(
      ".mailItem[data-mail-id='" + selectedMailId + "']"
    );
    if (selectedButton) {
      setSelectedButton(selectedButton);
    }
  }
}

// Run initialization if DOM is already loaded or wait for it
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initPhishing);
} else {
  initPhishing();
}
