Az's Companions — CCI example configs
=====================================

These JSON snippets are for iChun's Content Creator Integration (CCI).
They are NOT auto-loaded. Copy into the CCI Editor / config files.

Full guide: docs/CCI.md
Release: https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.6

Mod ID for IMC: azscompanions

IMC subjects (NeoForge CCI + Fabric CCI)
----------------------------------------
Chat / modes:
  companion_say / companion_greet / companion_wave
  companion_follow / companion_sit / companion_stay

AI (when provider enabled in azscompanions-ai.*):
  companion_ask / ai_ask          message=…  (LLM reply + optional actions)
  companion_ai_status / ai_status
  companion_ai_chat / stream_chat message=…;speaker=…
  companion_ai_config / ai_config chatListenMode=player;enableAiActions=true  (session only)

CCI-summoned companions use streamer chat input; AI applies when configured.
greet/wave → LLM when AI on (canned fallback). say exact unless ai=true.

Attitude / teams / summon / gear / modify / persona:
  companion_set_attitude     message = passive|hostile
  companion_set_team         message = team name or $username
  companion_summon           message = form=zombie;attitude=hostile;team=red;skin=Notch;whoAmI=…
  companion_summon_passive   message = form=chicken;team=blue
  companion_summon_hostile   message = form=skeleton;team=red
  companion_modify           message = form=wolf;followRadius=64;personalSpace=3;wanderRadius=12;
                             showArmor=true;chunkLoading=true;whoAmI=…;whatAmIDoing=…;howWillIBe=…
  companion_persona          message = whoAmI=…;whatAmIDoing=…;howWillIBe=…  OR op=get|clear
                             (marks initialized → skips first-create onboarding)
  companion_turn_evil        message = seconds=10   (5–15s playful HOSTILE, then revert)
  companion_set_mainhand     message = minecraft:diamond_sword | clear
  companion_set_offhand      message = minecraft:shield
  companion_set_armor        message = helmet=minecraft:iron_helmet;boots=minecraft:iron_boots
  companion_set_hand / companion_set_equipment
                             message = mainhand=…;offhand=…;helmet=…

Play:
  companion_rush / rush      message = seconds=6
  companion_hide_seek        message = role=hider|seeker;seconds=12
  companion_play             message = mode=rush|hide|seek|hide_seek|dance|peekaboo|stop

FTB Chunks (optional; ftbChunksAiClaim=true):
  claim_chunk / unclaim_chunk   optional chunkX=/chunkZ= (default: companion feet)

Hidden in-game: right-click companion with a fermented spider eye → same playful evil (~10s).

Team fights (CCI-first):
  /azscompanions teamfight on|off|status   (ops) — enable scoreboard + bits/subs spawns
  teamfight_enable / teamfight_disable / teamfight_toggle / teamfight_status
  teamfight_scoreboard   message = show|hide|reset|team1=red;team2=blue
  teamfight_score        message = team=red;points=1  OR  killer=Alice
  teamfight_top
  companion_spawn_leader message = name=Alice;form=zombie;subs=1;team=red
  companion_spawn_child  message = bits=500;count=1;maxChildren=8;name=Bit;team=red
  (maxChildren=/childCap= raises that parent's child slot cap; default 3)
  Bit tiers: 100 leather+stick … 1000 netherite. Cake RMB also calls spawnChild.

Example files
-------------
  imc-companion-*.json          (say/greet/wave/follow/sit/stay)
  imc-companion-set-team.json
  imc-companion-summon-hostile.json
  imc-companion-set-equipment.json
  imc-companion-modify.json
  imc-companion-persona.json
  imc-companion-rush.json
  imc-companion-hide-seek.json
  imc-claim-chunk.json
  imc-ai-config.json
  imc-companion-ask.json
  imc-companion-turn-evil.json
  imc-teamfight-enable.json
  imc-companion-spawn-leader.json
  imc-companion-spawn-child.json
  command-summon-wolf-alongside.json   (CCI-native /summon — not our bridge)
  command-azscci-greet-outcome.json    (Fabric /azscci fallback)

Twitch tips
-----------
- Channel points: match custom-reward-id; set message to $username or redemption input.
- Subs → companion_spawn_leader (teamfight ON). Bits/cheers → companion_spawn_child with bits=.
- Different teamIds fight each other; never the owner. Kills auto-score on the HUD.
- Persona via CCI on summon skips first-create onboarding for streamer setups.
