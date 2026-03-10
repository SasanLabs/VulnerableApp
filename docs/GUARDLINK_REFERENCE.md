# GuardLink — Annotation Reference

> Canonical reference for **VulnerableApp**. All agent instruction files point here.
> Full specification: [docs/SPEC.md](https://github.com/Bugb-Technologies/guardlink/blob/main/docs/SPEC.md)

## Quick Reference

```
DEFINE   @asset <Component.Path> (#id) -- "description"
         @threat <Name> (#id) [severity] cwe:CWE-NNN -- "description"
         @control <Name> (#id) -- "description"

RELATE   @mitigates <Asset> against <#threat> using <#control> -- "how"
         @exposes <Asset> to <#threat> [severity] cwe:CWE-NNN -- "what's wrong"
         @accepts <#threat> on <Asset> -- "why acceptable"
         @transfers <#threat> from <Source> to <Target> -- "who handles it"

FLOW     @flows <Source> -> <Target> via <mechanism> -- "details"
         @boundary <AssetA> | <AssetB> (#id) -- "trust boundary"
         @boundary between <AssetA> and <AssetB> (#id) -- "trust boundary"

LIFECYCLE
         @validates <#control> for <Asset> -- "test evidence"
         @audit <Asset> -- "what needs review"
         @owns <team-id> for <Asset> -- "responsible team"
         @handles <pii|phi|financial|secrets|internal|public> on <Asset>
         @assumes <Asset> -- "unverified assumption"

COMMENT  @comment -- "security-relevant developer note"

PROTECT  @shield -- "reason"
         @shield:begin -- "reason"   ... code ...   @shield:end
```

## Severity

`[P0]` = critical, `[P1]` = high, `[P2]` = medium, `[P3]` = low

## External References

Append after severity: `cwe:CWE-89`, `owasp:A03:2021`, `capec:CAPEC-66`, `attack:T1190`

## Rules

1. **Define once, reference everywhere.** `@asset`, `@threat`, `@control` with `(#id)` go in `.guardlink/definitions.java`. Source files use relationship verbs only (`@mitigates`, `@exposes`, etc.).
2. **Read definitions before adding.** Check for existing IDs first — avoid duplicates.
3. **Every `@exposes` needs a response.** Match with `@mitigates` (fix exists), `@audit` (flag for review), or `@accepts` (human-only risk acceptance). AI agents must NEVER write `@accepts` — use `@audit` instead.
4. **Always annotate security-relevant code.** At minimum, add `@comment` to document intent.
5. **Use the full verb set.** `@flows` for data movement, `@handles` for data classification, `@boundary` for trust boundaries.

## When Writing Code

| Situation | Annotation |
|-----------|------------|
| New service/component | `@asset` in definitions, then reference in source |
| Security gap exists | `@exposes Asset to #threat` |
| Risk with no fix yet | `@audit Asset` + `@comment` explaining potential controls |
| Implementing a fix | `@mitigates Asset against #threat using #control` |
| Processing sensitive data | `@handles pii on Asset` |
| Proprietary algorithm | `@shield:begin` ... `@shield:end` |
| Unsure which annotation | `@comment -- "describe what you see"` |

## Commands

```bash
guardlink validate .          # Check for errors
guardlink report .            # Generate threat-model.md
guardlink status .            # Coverage summary
guardlink suggest <file>      # Get annotation suggestions
```

## MCP Tools

When connected via `.mcp.json`, use:
- `guardlink_parse` — parse annotations, return threat model
- `guardlink_lookup` — query threats, controls, exposures by ID
- `guardlink_suggest` — get annotation suggestions for a file
- `guardlink_validate` — check for syntax errors
- `guardlink_status` — coverage stats
