Write-Host "==> 1. Atualizar branch com o master" -ForegroundColor Cyan
git fetch origin
git checkout feat/xss-challenge-mode
git merge origin/master --no-edit
if ($LASTEXITCODE -ne 0) {
    Write-Host "ATENCAO: o merge parou com conflitos. Resolve-os manualmente (git status) antes de continuar, depois corre este script outra vez." -ForegroundColor Yellow
    exit 1
}

$msgPath = "src/main/resources/i18n/messages.properties"
if (-not (Test-Path $msgPath)) {
    Write-Host "ERRO: nao encontrei $msgPath a partir desta pasta. Confirma que estas na raiz do projeto." -ForegroundColor Red
    exit 1
}

$content = Get-Content -Raw -Encoding UTF8 $msgPath

$replacements = @{
"XSS_HTML_TAG_INJECTION_LEVEL1_CHALLENGE=No filtering is applied before your input is placed inside a <div> tag. Get a JavaScript alert to fire." = "XSS_HTML_TAG_INJECTION_LEVEL1_CHALLENGE=Get a JavaScript alert box to fire on this page.`nXSS_HTML_TAG_INJECTION_LEVEL1_HINT1=Your input is placed inside a <div> tag without any filtering or escaping applied."

"XSS_HTML_TAG_INJECTION_LEVEL2_CHALLENGE=A regex now strips out certain tags before your input reaches the <div>. Find a tag name it doesn't recognize." = "XSS_HTML_TAG_INJECTION_LEVEL2_CHALLENGE=Get a JavaScript alert box to fire, even though this level filters out certain tags.`nXSS_HTML_TAG_INJECTION_LEVEL2_HINT1=A regex strips out certain tag names before your input reaches the <div>.`nXSS_HTML_TAG_INJECTION_LEVEL2_HINT2=Not every tag name that can trigger JavaScript is covered by that regex."

"PERSISTENT_XSS_LEVEL4_CHALLENGE=This level only scans for <img>/<input> up to a null byte in your comment, and if it finds nothing there, skips filtering entirely for the whole comment. Exploit that." = "PERSISTENT_XSS_LEVEL4_CHALLENGE=Get your persisted comment to execute JavaScript, even though this level filters for <img> and <input> tags.`nPERSISTENT_XSS_LEVEL4_HINT1=Look at how the filter scans your comment before it gets stored - does it check the whole comment, or does it stop early?`nPERSISTENT_XSS_LEVEL4_HINT2=In C/C++, how does a string know where it ends? Think about what happens if that same character shows up inside your input."

"XSS_IMG_TAG_LEVEL1_CHALLENGE=Your input goes straight into an unquoted src attribute. Break out of it to add a new attribute." = "XSS_IMG_TAG_LEVEL1_CHALLENGE=Steal the session cookie from this page and exfiltrate it to a URL you control.`nXSS_IMG_TAG_LEVEL1_HINT1=Your input goes straight into an unquoted src attribute - nothing escapes or quotes it.`nXSS_IMG_TAG_LEVEL1_HINT2=Break out of the attribute to inject a new attribute, or a whole new tag.`nXSS_IMG_TAG_LEVEL1_HINT3=Once you can run arbitrary JavaScript, read document.cookie and send it to an external endpoint (e.g. an <img> tag pointing at a server you control)."
}

$missing = @()
foreach ($old in $replacements.Keys) {
    if ($content -like "*$old*") {
        $content = $content.Replace($old, $replacements[$old])
    } else {
        $missing += $old.Substring(0, [Math]::Min(70, $old.Length)) + "..."
    }
}

Set-Content -Path $msgPath -Value $content -Encoding UTF8 -NoNewline

if ($missing.Count -gt 0) {
    Write-Host "`nAVISO: estas linhas originais NAO foram encontradas no ficheiro (pode ja ter sido editado por outro commit, ou ha conflito por resolver):" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host "`nSe isto aparecer, NAO faças commit ainda - cola-me o output para eu ajustar o script." -ForegroundColor Yellow
} else {
    Write-Host "`nTodas as substituicoes foram aplicadas com sucesso." -ForegroundColor Green
}

git add $msgPath
Write-Host "`n==> Diff para reveres:" -ForegroundColor Cyan
git diff --staged -- $msgPath

Write-Host "`n==> Se o diff acima estiver correto, corre:" -ForegroundColor Cyan
Write-Host "   git commit -m 'fix: separate challenge/hint/payload per review, reword level 4 hint'"
Write-Host "   git push"
Write-Host "`nAINDA FALTA (manual):" -ForegroundColor Yellow
Write-Host "  - Remover @AttackVector e @ChallengeCard do nivel seguro em XSSInImgTagAttribute.java"
