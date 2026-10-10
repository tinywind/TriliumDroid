# Large attachment sync implementation plan

> Execution: inline, following systematic-debugging and test-driven-development. The user authorized implementation, build, installation, and device verification in this checkout.

**Goal:** Complete initial synchronization on the connected S25, including large attachments, without deleting the existing app or server data.

**Architecture:** Stream sync JSON and decode each content value to a temporary file. Apply one entity at a time, writing large blobs in bounded chunks. Commit each response and its checkpoint atomically. Install the existing debug application ID alongside the official release because the release signing key is unavailable.

**Tech stack:** Kotlin, Android SQLite, Jackson Core streaming parser, JUnit, Gradle, GitHub Actions, ADB.

**Spec:** User request in this conversation: fork under tinywind, clone under ~/Projects, fix, build, install, preserve existing data, and verify large attachments.

## Constraints and review focus

- Preserve the original package and server contents. Never clear app data or uninstall the official package.
- Do not place passwords, tokens, personal notes, or device dumps in Git.
- Keep Java heap usage bounded when parsing attachments larger than the 256 MiB app heap.
- Verify malformed/truncated responses clean temporary files and do not advance checkpoints.
- Verify binary zero bytes, null and empty content, arbitrary JSON field order, and unknown metadata.
- Verify a large SQLite blob by size and hash, not merely a success notification.

## Tasks

- [x] Reproduce baseline failure: official alpha19 crashes repeatedly allocating 47,504,464 bytes at Sync.kt blob decoding; 256 MiB heap limit, server checkpoint 14670.
- [x] Fork tinywind/TriliumDroid and clone C:/Users/tinywind/Projects/TriliumDroid; use bugfix/stream-large-sync-attachments.
- [x] Add streaming parser regressions, including a blob larger than the test JVM heap and interrupted input.
- [x] Implement bounded content ingestion in StreamingSyncReader.java, ConnectionUtil.kt, and Sync.kt; add atomic page transaction and cleanup.
- [x] Build a distinguishable debug APK using the existing .debug ID; run unit and focused Android storage tests.
- [x] Install alongside official release, configure privately, and synchronize against the existing server.
- [x] Verify completion, large attachment hashes, crash logs, and continued presence of the official app.
- [x] Review changes and publish the working branch to the requested fork; report evidence and limitations.

## Progress

2026-10-10: Original app is not debuggable; run-as cannot extract its private database. Side-by-side installation preserves its data without relying on an unavailable release signing key. Local JDK is available through IntelliJ; no Android SDK found in the usual locations, so use GitHub Actions for Android builds.

Parser verification: four JUnit tests passed under `-Xmx96m`, including a 280 MiB binary hash comparison, null/empty/escaped data, truncated input, consumer failure, and missing checkpoints.

Review ruling: SQLite concatenation keeps Java heap bounded but native allocation grows with the blob and cumulative copying is quadratic in the number of chunks. Accept this Android public-API tradeoff only after real-device verification of the largest existing attachments. Do not claim constant total process memory or unlimited attachment size.

## Verified result (2026-10-11)

The connected Samsung device completed the initial pull at checkpoint 51069 with zero outstanding changes. Restarting the app completed another pull with zero outstanding changes. No recurrence of the original out-of-memory crash was observed during this run.

| Check | Result |
| --- | --- |
| Parser with a 96 MiB JVM heap | Four tests passed, including exactly 391,643,136 bytes (373.5 MiB), the documented server content limit |
| GitHub Actions build | Eight JVM tests passed; debug and instrumentation APKs built at `5b3204450220e743cd4247195f7468f2ec563ed5` |
| Real synchronized database | Read-only instrumentation passed: 1,705 notes and 5,906 blobs matched the server counts |
| Five largest existing attachments | SHA-256 matched for 248,710,446; 222,676,010; 204,825,207; 204,825,206; and 160,956,173 bytes; verification took 194.297 seconds |
| Android maximum-size storage | Two tests passed in 222.932 seconds, including a 391,643,136-byte binary hash comparison, malformed-page rollback, unchanged checkpoint after failure, and metadata entities |
| Original installation | Official `eu.fliegendewurst.triliumdroid` alpha19 remained installed with its original update time; no uninstall or data-clear operation targeted it |

The original release is not debuggable and its signing key is unavailable. The fix is therefore installed alongside it as **TriliumDroid Large Sync**, package `eu.fliegendewurst.triliumdroid.debug`. The original private database was preserved in place, not extracted or migrated. The new app downloaded the server data independently.

Build evidence: https://github.com/tinywind/TriliumDroid/actions/runs/38062144666

Installed APK SHA-256: `FF1B87235CE3BDB3183415753A02DC4FE14DDA626E433972EF30203A8FF34545`.

The APKs were signed locally with the existing Android debug keystore so subsequent local builds can update this debug installation. An APK signed with a different CI debug key cannot replace it directly. The instrumentation package and external verification manifest were removed after testing.

Scope: this fixes the server-to-device sync ingestion path. It does not change the server's content limit, attachment viewing, or the device-to-server upload path. The five largest real blobs were fully hashed; every other blob was not individually hashed. The maximum-size Android test used an isolated temporary database and did not add synthetic notes or attachments to the user's server.
