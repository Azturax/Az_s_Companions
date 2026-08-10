Az's Companions — CCI example configs
=====================================

These JSON snippets are for iChun's Content Creator Integration (CCI).
They are NOT auto-loaded. Copy into the CCI Editor / config files.

Full guide: docs/CCI_STREAMING_GUIDE.md
Release: https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.0

Mod ID for IMC: azscompanions

IMC subjects (NeoForge CCI + Fabric CCI)
----------------------------------------
Chat / modes:
  companion_say / companion_greet / companion_wave
  companion_follow / companion_sit / companion_stay

Attitude / teams / summon / gear:
  companion_set_attitude     message = passive|hostile
  companion_set_team         message = team name or $username
  companion_summon           message = form=zombie;attitude=hostile;team=red;skin=Notch
  companion_summon_passive   message = form=chicken;team=blue
  companion_summon_hostile   message = form=skeleton;team=red
  companion_set_mainhand     message = minecraft:diamond_sword | clear
  companion_set_offhand      message = minecraft:shield
  companion_set_armor        message = helmet=minecraft:iron_helmet;boots=minecraft:iron_boots
  companion_set_hand / companion_set_equipment
                             message = mainhand=…;offhand=…;helmet=…

Example files
-------------
  imc-companion-*.json          (say/greet/wave/follow/sit/stay)
  imc-companion-set-team.json
  imc-companion-summon-hostile.json
  imc-companion-set-equipment.json
  command-summon-wolf-alongside.json   (CCI-native /summon — not our bridge)
  command-azscci-greet-outcome.json    (Fabric /azscci fallback)

Twitch tips
-----------
- Channel points: match custom-reward-id; set message to $username or redemption input.
- Bits/cheers: greet or summon_hostile with form=…;team=…
- Different teamIds fight each other; never the owner.
