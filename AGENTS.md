# Qist repository instructions

These instructions apply to the entire repository, including future chats and fresh checkouts.

## Read before working

Before planning, reviewing, editing code, or running QA, read the complete `.agent/skills/kotlin-android-excellence/SKILL.md` and follow its instructions throughout the task. Follow any references it requires for the affected work. Do not rely on chat memory or a summary of the skill.

Read any additional instructions in the directories you will change, inspect the current branch and working diff, and preserve unrelated work. If the skill cannot be accessed, explain the missing file instead of claiming to have followed it. Explicit user instructions and higher-priority instructions take precedence.

## Qist behavior to preserve

- Preserve the approved design, initial skeleton, shared custom dialogs and navigation unless the user requests a change.
- Show the top pull-to-refresh indicator only for a user-initiated pull. Resume and notification updates must refresh silently; avoid duplicate requests.
- Keep retailer details and contact actions off Profile. Place them in Help and support, and hide call/WhatsApp actions when no valid retailer phone is available.
- Reuse existing components, state owners and repository logic. Avoid speculative abstractions, dependency upgrades and performance claims without evidence.

## Verification and handoff

Use the repository's actual toolchain and run checks proportional to the change. Report what passed, failed or was not run, distinguishing host tests, emulator checks and physical-device testing. Never store test credentials, tokens or raw customer responses in source or documentation.
