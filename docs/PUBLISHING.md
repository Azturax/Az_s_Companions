# Publishing Az's Companions to Modrinth & CurseForge

Automated upload of **all release jars** (one per Minecraft × loader; CCI soft-compat in-jar) via GitHub Actions + [mc-publish](https://github.com/Kira-NT/mc-publish).

Workflow: [`.github/workflows/publish.yml`](../.github/workflows/publish.yml)

Tokens and project IDs must belong to the **Az's Companions** Modrinth / CurseForge projects (suggested slug `azs-companions`), not other mods.

## Status (Aug 2026)

| Item | Status |
|------|--------|
| GitHub Releases | **Live** (e.g. [v1.0.7](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.7)) |
| Modrinth project | **Not created yet** under Azturax (slug `azs-companions` 404) — create it, then set `MODRINTH_PROJECT_ID` (or `MODRINTH`) |
| CurseForge project | **Not created / ID not set** — create Az's Companions, then set `CURSEFORGE_PROJECT_ID` (or `CURSEFORGE`) |
| Upload tokens | Prefer `PUBLISH_MODRINTH_TOKEN` / `PUBLISH_CURSEFORGE_TOKEN` (legacy `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` still accepted) |

Suggested slugs when creating store pages:

- Modrinth: `azs-companions` → `https://modrinth.com/mod/azs-companions`
- CurseForge: `azs-companions` → `https://www.curseforge.com/minecraft/mc-mods/azs-companions`

Paste-ready store copy: [STORE_DESCRIPTION.md](STORE_DESCRIPTION.md).

## What gets uploaded

On each publish run, the workflow downloads jars from the **GitHub Release** and uploads **one Modrinth/CurseForge version per jar (8 files for `v1.0.8`):

| Jar pattern | Loader | Minecraft | Edition |
|-------------|--------|-----------|---------|
| `azscompanions-neoforge-*-1.21.1.jar` | NeoForge | 1.21.1 | standalone |
| `azscompanions-neoforge-cci-*-1.21.1.jar` | NeoForge | 1.21.1 | CCI |
| `azscompanions-fabric-*-1.21.1.jar` | Fabric | 1.21.1 | standalone |
| `azscompanions-fabric-cci-*-1.21.1.jar` | Fabric | 1.21.1 | CCI |
| same ×4 for **1.21.5** | NeoForge / Fabric | 1.21.5 | ±CCI |
| `azscompanions-forge-*-1.20.1.jar` | Forge | 1.20.1 | standalone |
| `azscompanions-forge-cci-*-1.20.1.jar` | Forge | 1.20.1 | CCI |
| `azscompanions-fabric-*-1.20.1.jar` | Fabric | 1.20.1 | standalone |
| `azscompanions-fabric-cci-*-1.20.1.jar` | Fabric | 1.20.1 | CCI |
| `azscompanions-neoforge-*-26.1.2.jar` | NeoForge | 26.1.2 | standalone |
| `azscompanions-neoforge-*-26.2.jar` | NeoForge | 26.2 | standalone |

Version IDs on the stores look like `1.0.7+1.21.1-neoforge` / `1.0.7+1.21.1-neoforge-cci` so CCI and standalone stay distinct. Display names match the GitHub release version (e.g. `1.0.7 — neoforge CCI 1.21.1`).

CCI jars declare required dependencies on **CCI** + **iChunUtil**; Fabric jars require **Fabric API**.

## Secrets (Actions → Secrets)

Add under **Settings → Secrets and variables → Actions → Secrets**:

| Secret | Where to get it |
|--------|-----------------|
| `PUBLISH_MODRINTH_TOKEN` | [Modrinth → Settings → Authorization](https://modrinth.com/settings/account) → create a PAT with **Create versions** (and project write as needed) for the Az's Companions account |
| `PUBLISH_CURSEFORGE_TOKEN` | [CurseForge → Settings → API Tokens](https://console.curseforge.com/) → generate an API token for the Az's Companions account |

Legacy names `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` still work if `PUBLISH_*` is unset. Do **not** commit tokens. Do not put them in `gradle.properties`.

## Variables (Actions → Variables) — still required

Add under **Settings → Secrets and variables → Actions → Variables**. These must be the **Az's Companions** project IDs (slug `azs-companions`), not placeholders and not IDs from another mod:

| Variable | Example | Notes |
|----------|---------|-------|
| `MODRINTH_PROJECT_ID` or `MODRINTH` | `AbCdEfGh` or `azs-companions` | Modrinth project **id** (8-char) or slug — from the Az's Companions project URL / API after creation. Short name `MODRINTH` is accepted if `MODRINTH_PROJECT_ID` is unset. |
| `CURSEFORGE_PROJECT_ID` or `CURSEFORGE` | `1234567` | Numeric CurseForge project id for Az's Companions (from the project page / CF console). Short name `CURSEFORGE` is accepted if `CURSEFORGE_PROJECT_ID` is unset. |

Until both a token **and** the matching project id exist for a platform, that platform is skipped. If **neither** platform is fully configured, the workflow finishes with a warning and does not upload.

Placeholders also live in `gradle.properties` (`modrinth_project_id` / `curseforge_project_id`) for documentation only — the workflow reads **GitHub Variables**, not Gradle.

## How to trigger

1. **Automatic:** publish a GitHub Release (`release` → `published`). Jars must already be attached to that release (as with `v1.0.7`).
2. **Manual:** **Actions → Publish Modrinth / CurseForge → Run workflow** → set `tag` (e.g. `v1.0.7`) and optional `version_type`.

The workflow does **not** rebuild jars and does **not** re-upload to GitHub; it only mirrors release assets to Modrinth/CurseForge.

### Publish an existing release (e.g. v1.0.7)

After secrets + Az's Companions project IDs are set:

1. Actions → **Publish Modrinth / CurseForge** → **Run workflow**
2. Tag: `v1.0.7`
3. Version type: `release`

## One-time project setup checklist

1. Create empty Modrinth project for **Az's Companions** (slug `azs-companions` recommended); copy project id → `MODRINTH_PROJECT_ID` (or `MODRINTH`)
2. Create empty CurseForge Minecraft mod project for **Az's Companions**; copy numeric id → `CURSEFORGE_PROJECT_ID` (or `CURSEFORGE`)
3. Add `PUBLISH_MODRINTH_TOKEN` and `PUBLISH_CURSEFORGE_TOKEN` secrets (or legacy names)
4. Run the workflow against the latest release tag
5. Update README download links once the store pages exist

## Local / Gradle note

This repo does **not** use Minotaur / CurseGradle / mod-publish-plugin for CI. Publishing is Actions-only so one matrix covers all 14 jars without wiring every subproject. Building remains:

```bash
./gradlew buildAll buildAll215 buildAll1201 buildNeoForge26 buildNeoForge261
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Workflow summary: “Publish skipped — missing …” | Add the listed secret(s) / variable(s) |
| `gh release download` fails | Ensure the tag exists and the jar name matches the matrix (version from tag without `v`) |
| CurseForge rejects `26.1.2` / `26.2` | CF may lag on new game versions — upload those jars manually or retry later |
| Duplicate version error | That `version` id was already uploaded; bump mod version or delete the store version |
| CCI dependency warnings | Confirm CCI / iChunUtil exist for that loader + MC on the target store |