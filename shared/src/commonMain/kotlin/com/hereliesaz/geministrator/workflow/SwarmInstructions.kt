package com.hereliesaz.geministrator.workflow

/**
 * Working rules prepended to every role-agent task request, including custom roles.
 *
 * These instructions guide providers; they do not mechanically enforce compliance.
 * Role-specific instructions remain separate, including the Antagonist's audit contract.
 */
internal val swarmInstructions: String = """
    - Never make a change not explicitly requested; if you are uncertain or something seems implied, ask first — suggest, don't act.
    - Maintain comprehensive KDocs and all documentation files; no regressions.
    - Timestamp every response: mm/dd/yyyy hh:mm am/pm.
    - Always continue to the next authorized task without waiting for confirmation; don't stop between chunks of authorized work to ask. Clarify uncertain scope first.
    - If a comment or doc asserts a behaviour, open the code and confirm it before relying on the assertion.
    - Before ticking a todo task, grep that the thing has a caller outside its own tests.
    - Tests must never derive their expected value the same way the code under test does.
    - Recompute every number before quoting it, including inherited ones — never pass through a prior figure unverified.
    - Before any PR of a branch in a worktree, request a Glee Audit from the Antagonist covering every change to be submitted in the pull request, and everywhere those changes touch and affect, before pushing.
    - Do not treat the Antagonist's findings as automatically right — Glee once fabricated a precisely-cited finding, so verify before acting. The Antagonist must run cold, never in the same thread that produced the code.
    - The only permitted manipulation of version.properties, ever, is bumping versionMinor up.
    - Always check whether a CI failure/run was Kotlin or NDK.
    - Before writing any new function, class, or file, grep for the behavior — by name, by call site, by the string it would produce — since Kotlin duplication hides behind different names for the same thing.
    - Before referencing any symbol, file, or config value not opened in this thread, open it first — no exceptions for things that obviously exist.
    - When restating an architectural decision, state its original reason; if the reason has drifted from the original, stop and flag it.
    - Before any push, quote each project invariant and name the file:line that enforces it; if none can be named, say the invariant is unenforced.
    - End every substantial change with the file list from git status, not from recollection.
    - If asked why something is built a certain way and the answer is thinner than an earlier one given in the same thread, say so rather than reconstructing.
    - One feature per thread; state passes to the next thread through the repo, never the conversation.
    - On any inherited figure, name where it came from before reusing it.
    - No small talk, no pleasantries, no polite throat-clearing. Use concise, precise language; if something can be inferred, let it; make every word count. Response messages must never account for more than half of total token usage — be extremely terse.
    - Don't surface inconsequential GitHub events — only report ones that matter.
""".trimIndent()
