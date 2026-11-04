# HavenOS v3 Track A — Local LLM plan (Phase 3, NOT integrated)

Status: **seam + ScriptedBrain shipped; LocalLlmBrain NOT wired.**
This is the documented plan the builder prompt §6.2 asks for, not a
shipped integration. Nothing here is a claim that a model runs.

## What shipped
- `MascotBrain` seam (`ui/mascot/MascotBrain.kt`): `handle(input, ctx)`
  returning text + expression + proposed actions. The brain never
  executes anything; the layer performs actions, and state-changing
  actions require an explicit "Do it" / "No" confirm chip.
- `ScriptedBrain`: deterministic, instant, offline, zero model cost.
  It is the default path for everything, including free-form chat text
  typed into the mascot bubble.
- `LocalLlmBrain`: a NOT_CONFIGURED implementation that answers
  honestly ("The on-device chat model isn't set up yet…") and falls
  back to scripted answers. `MascotLlmConfig` holds null placeholders
  for the model download URL and SHA-256 checksum — by design, never
  invented values.
- Settings → Mascot → Chat model shows "Not set up" with an honest
  explanation. There is deliberately no download button that does
  nothing, no bundled model, no network call, and no query logging.

## Candidates considered and why none is wired
- **MediaPipe LLM Inference API** (maintained, Apache-2.0): the most
  plausible route — GPU/CPU inference with a quantized Gemma-style
  model. Rejected for this build: its artifact + model fetch cannot be
  verified from this offline sandbox, and latency/RAM on the owner's
  Helio G99 (Redmi Pad 2) is unmeasured. Wiring it blind would violate
  the prompt's "integrates cleanly" gate.
- **llama.cpp via JNI** (maintained, MIT): proven on-device runtime,
  but a native build + JNI bridge + model packaging is a whole
  project, not a clean integration inside this track's budget.
- **ExecuTorch** (maintained): its Android story for chat LLMs is
  thin, and performance on this device class is equally unmeasured.

## Setup gaps (owner decisions needed before any integration)
1. Model choice + download URL (HTTPS host, exact file).
2. SHA-256 checksum for integrity verification.
3. Runtime choice (recommend re-evaluating MediaPipe first).
4. Device-capability gate: measure RAM + tokens/sec on the Redmi Pad 2
   before promising chat; on a weak result, chat stays scripted and
   says so plainly (prompt §6.2).
5. Download UX (prompt §6.2): free-space + RAM checks, Wi-Fi
   recommendation, progress/pause/cancel/resume, checksum verify,
   delete, and the not-downloaded / downloading / ready / loading /
   generating / out-of-memory / failed / deleted states — none of
   this UI exists yet beyond the status row, because building it
   around a runtime that isn't chosen would be a placeholder feature
   that looks real.

## Constraints that already hold (and must hold for any future wiring)
- Wakes only when the user opens mascot chat; unloads after ~60s idle;
  never loads at boot; never listens.
- Structured output (intent + arguments) validated against a whitelist
  schema; invalid output falls back to ScriptedBrain.
- Confirmation rule for state-changing actions (already in the layer).
- Chat history in memory only; no query logging; no user data sent
  anywhere (the only network use would be the user-initiated model
  download, inbound only, over HTTPS, checksum-verified).
