function safeSetInnerText(id, text) {
    const el = document.getElementById(id);
    if (el) el.innerText = text;
}

function detectCurrentLevel() {
    const element = document.querySelector(".breadcrumb-item.active") ||
        document.querySelector(".vulnerability-level-header") ||
        document.querySelector("h2");

    const textToSearch = localStorage.getItem("vulnerabilityLevelSelected");

    const match = textToSearch.match(/LEVEL_?(\d+)/i) || textToSearch.match(/Level\s*(\d+)/i);

    return (match && match[1]) ? match[1] : "1";
}

function updateLevelContent() {
    const level = detectCurrentLevel();
    console.log("Detecting Level:", level);

    const config = {
        "1": { title: "Level 1: String Breakout", goal: "Bypass authentication via string concatenation.", hint: "Try: <code>admin\" , \"password\": {\"$ne\": \"1\"}</code>" },
        "2": { title: "Level 2: Object Injection", goal: "Direct injection into the query without quotes.", hint: "Try: <code>{\"$gt\": \"\"}</code>" },
        "3": { title: "Level 3: Blind Injection", goal: "Infer data from server responses (True/False).", hint: "Try Regex for guessing: <code>admin\" , \"username\": {\"$regex\": \"^a.*\"}</code>" },
        "4": { title: "Level 4: Data Exfiltration", goal: "Use logical operators to fetch all records.", hint: "Use <code>$or</code> to fetch more than one user." },
        "5": { title: "Level 5: Pipeline Stage Injection", goal: "Break Aggregation and inject new stages.", hint: "Try breaking Match to add a Group stage." },
        "6": { title: "Level 6: Secure Mode", goal: "Test hardened defenses using Criteria.is().", hint: "This level is programmatically protected." }
    };

    if (config[level]) {
        safeSetInnerText("displayLevelTitle", config[level].title);
        safeSetInnerText("targetGoal", config[level].goal);
        const hintEl = document.getElementById("targetHint");
        if (hintEl) hintEl.innerHTML = config[level].hint;
    }
}

window.executeNoSQLAttack = function () {
    const level = detectCurrentLevel();
    const userInput = document.getElementById("nosql_user_input").value;
    const responseArea = document.getElementById("nosql_response_area");

    if (!responseArea) return;
    responseArea.innerText = "Executing attack...";

    const url = `/VulnerableApp/NoSQLInjection/LEVEL_${level}?username=${encodeURIComponent(userInput)}`;

    fetch(url)
        .then(res => res.json())
        .then(data => {
            if (data.isValid) {
                try {
                    const json = JSON.parse(data.content);
                    responseArea.innerText = JSON.stringify(json, null, 4);
                } catch (e) {
                    responseArea.innerText = data.content;
                }
            } else {
                responseArea.innerText = "Query error: " + data.content;
            }
        })
        .catch(err => {
            responseArea.innerText = "Connection failed: " + err;
        });
};

document.addEventListener("DOMContentLoaded", updateLevelContent);

setTimeout(updateLevelContent, 1000);