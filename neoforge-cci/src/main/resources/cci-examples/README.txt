Az's Companions — CCI example configs
=====================================

These JSON snippets are for iChun's Content Creator Integration (CCI).
They are NOT auto-loaded. Copy the pieces you want into a CCI Event
Configuration via the in-game CCI Editor, or merge into your CCI config files.

Full guide: docs/CCI_STREAMING_GUIDE.md (in the source repo)

Mod IDs
-------
- Az's Companions: azscompanions
- CCI: contentcreatorintegration
- Library: ichunutil

IMC subjects handled TODAY by the CCI edition jar
-------------------------------------------------
Use an IMCOutcome targeting modId = azscompanions:

  companion_say     message = text the companion should say
  companion_greet   message = subscriber / tipper display name
  companion_wave    message = optional name to wave at
  companion_follow  message ignored (mode FOLLOW)
  companion_sit     message ignored (mode SIT)
  companion_stay    message ignored (mode STAY)

Short aliases also work: say, greet, wave, follow, sit, stay.

Files in this folder
--------------------
  imc-companion-say-outcome.json
  imc-companion-greet-outcome.json
  imc-companion-wave-outcome.json
  imc-companion-follow-outcome.json
  imc-companion-sit-outcome.json
  imc-companion-stay-outcome.json
  command-summon-wolf-alongside.json   (CCI-native /summon — not our bridge)

Example IMCOutcome object
-------------------------
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}

Notes
-----
- CCI documents IMCOutcome as client-side; the CCI edition forwards actions
  to the server so the companion entity can react.
- Companion must be summoned and within ~96 blocks of the streamer.
- Mob spawning is CCI CommandOutcome (/summon), not an Az's Companions IMC subject.
- Install azscompanions-neoforge-cci-*.jar OR the standalone NeoForge jar,
  never both. Also install CCI 1.13.0 + iChunUtil 1.0.3 for 1.21.1 NeoForge.
