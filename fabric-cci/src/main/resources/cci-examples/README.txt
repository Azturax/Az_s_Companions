Az's Companions — CCI example configs (Fabric CCI edition)
==========================================================

These JSON snippets are for iChun's Content Creator Integration (CCI).
They are NOT auto-loaded. Copy the pieces you want into a CCI Event
Configuration via the in-game CCI Editor, or merge into your CCI config files.

Full guide: docs/CCI_STREAMING_GUIDE.md (in the source repo)

Mod IDs
-------
- Az's Companions: azscompanions
- CCI: contentcreatorintegration
- Library: ichunutil

How Fabric receives stream actions
----------------------------------
iChunUtil's Fabric loader does not implement Forge InterModComms
(sendIMCMessage always returns false). This jar installs a client mixin on
CCI's IMCOutcome so the SAME IMC subjects as NeoForge still work:

  companion_say / companion_greet / companion_wave
  companion_follow / companion_sit / companion_stay

Aliases: say, greet, wave, follow, sit, stay.

Fallback: CCI CommandOutcome → /azscci <subject> [message]
  (see command-azscci-greet-outcome.json)

Files in this folder
--------------------
  imc-companion-say-outcome.json
  imc-companion-greet-outcome.json
  imc-companion-wave-outcome.json
  imc-companion-follow-outcome.json
  imc-companion-sit-outcome.json
  imc-companion-stay-outcome.json
  command-azscci-greet-outcome.json    (Fabric CommandOutcome fallback)
  command-summon-wolf-alongside.json   (CCI-native /summon — not our bridge)

Example IMCOutcome object (works on Fabric CCI edition via mixin)
-----------------------------------------------------------------
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}

Notes
-----
- Companion must be summoned and within ~96 blocks of the streamer.
- Install azscompanions-fabric-cci-*.jar OR the standalone Fabric jar, never both.
- Also install CCI 1.13.0 + iChunUtil 1.0.3 Fabric builds for 1.21.1.
