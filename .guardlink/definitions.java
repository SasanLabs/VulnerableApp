// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// GuardLink Shared Definitions — VulnerableApp
//
// ALL @asset, @threat, and @control declarations live here.
// Source files reference by #id only (e.g. @mitigates X against #sqli).
// Never redeclare an ID that exists in this file.
// Before adding: read this file to check for duplicates.
//
// Run: guardlink validate .
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// ─── Examples (uncomment and customize for your project) ────────
//
//   // @asset App.API (#api) -- "Main REST endpoint"
//   // @asset App.Database (#db) -- "Primary data store"
//
//   // @threat SQL_Injection (#sqli) [critical] cwe:CWE-89 -- "Unsanitized input reaches SQL query"
//   // @threat Cross_Site_Scripting (#xss) [high] cwe:CWE-79 -- "Unsanitized input rendered in browser"
//   // @threat Broken_Access_Control (#bac) [critical] cwe:CWE-284 -- "Missing or bypassable authorization"
//
//   // @control Parameterized_Queries (#prepared-stmts) -- "SQL queries use bound parameters"
//   // @control Input_Validation (#input-validation) -- "Input validated against schema/allowlist"
//   // @control RBAC (#rbac) -- "Role-based access control"
//
// ─── Your Definitions ──────────────────────────────────────────

// @asset VulnerableApp.Application (#app) -- "Spring Boot application runtime and configuration"
// @asset External.Browser (#browser) -- "End-user browser interacting with the application"
// @asset External.Attacker (#attacker) -- "Threat actor interacting with endpoints to exploit intentionally vulnerable flows"
// @asset VulnerableApp.Web.Controllers (#web-controllers) -- "HTTP controllers exposing vulnerability endpoints"
// @asset VulnerableApp.Web.Templates (#web-templates) -- "HTML/JS templates served to browsers"
// @asset VulnerableApp.Database.H2 (#db-h2) -- "Embedded H2 database storing application data"
// @asset VulnerableApp.Filesystem (#fs) -- "Host filesystem access used by some vulnerabilities (uploads/path traversal)"
// @asset VulnerableApp.Network.Egress (#net-egress) -- "Outbound HTTP/network capability (SSRF/RFI style flows)"
// @asset VulnerableApp.Crypto.JWT (#jwt) -- "JWT issuance and validation components"
// @asset VulnerableApp.XML.Parsing (#xml) -- "XML parsing / JAXB components"

// @threat SQL_Injection (#sqli) [P0] cwe:CWE-89 owasp:A03:2021 -- "Untrusted input reaches SQL query construction/execution"
// @threat Cross_Site_Scripting (#xss) [P1] cwe:CWE-79 owasp:A03:2021 -- "Untrusted input is rendered into HTML/JS context"
// @threat Server_Side_Request_Forgery (#ssrf) [P0] cwe:CWE-918 owasp:A10:2021 attack:T1190 -- "Untrusted input controls outbound requests"
// @threat XML_External_Entity (#xxe) [P0] cwe:CWE-611 owasp:A05:2021 -- "XML parser allows external entity expansion / SSRF / file read"
// @threat Path_Traversal (#path-traversal) [P0] cwe:CWE-22 owasp:A01:2021 -- "User-controlled path escapes intended directory"
// @threat Unrestricted_File_Upload (#file-upload) [P0] cwe:CWE-434 owasp:A05:2021 -- "Attacker uploads executable/unsafe files"
// @threat OS_Command_Injection (#cmdi) [P0] cwe:CWE-78 owasp:A03:2021 -- "Untrusted input reaches OS command execution"
// @threat Open_Redirect (#open-redirect) [P2] cwe:CWE-601 owasp:A10:2021 -- "Untrusted input controls navigation/redirect target"
// @threat Weak_JWT_Validation (#jwt-weak) [P1] cwe:CWE-345 owasp:A07:2021 -- "JWTs can be forged/accepted due to weak validation"
// @threat Cryptographic_Failures (#crypto-fail) [P1] owasp:A02:2021 -- "Use of weak/incorrect crypto primitives or configurations"

// @control Input_Validation (#input-validation) -- "Validate request parameters using allowlists/schemas and length constraints"
// @control Output_Encoding (#output-encoding) -- "Contextual HTML/JS/CSS/URL encoding before rendering to clients"
// @control Parameterized_Queries (#prepared-stmts) -- "Use bound parameters / ORM methods that prevent injection"
// @control Disable_XXE (#disable-xxe) -- "Configure XML parsers to disallow DTD/external entity resolution"
// @control Path_Normalization_And_Allowlist (#path-allowlist) -- "Normalize paths and restrict to allowlisted directories/files"
// @control Content_Type_And_Extension_Validation (#upload-validation) -- "Validate uploads (type, extension) and store outside web root"
// @control Command_Avoidance (#cmd-avoidance) -- "Avoid shelling out; use safe APIs; if unavoidable, strict allowlists"
// @control Egress_Filtering (#egress-filtering) -- "Restrict outbound network destinations; block internal metadata ranges"
// @control Redirect_Allowlist (#redirect-allowlist) -- "Allowlist redirect destinations or use relative-only redirects"
// @control JWT_Strong_Validation (#jwt-strong-validation) -- "Require strong algorithms, verify signatures/claims, reject 'none'"
// @control Salted_SHA256 (#salted-sha256) -- "SHA-256 with salt (demo only; real password hashing should use a slow KDF)"

