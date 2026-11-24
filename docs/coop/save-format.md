# Cooperative checkpoint format 1

The host's private application directory contains `coop-saves-v1/<session UUID>/`. The engine executor takes a checkpoint only between completed actions, at an input offer or a terminal party defeat. A halted engine or an actor graph containing an unsupported transient actor cannot be published. The supported world is branch 0, depth 1, without challenges. A checkpoint has a native game version as well as a cooperative schema number. There is no migration from unknown schemas.

## Engine image

The original Dungeon world bundle includes the primary hero, generation state, statistics and the next actor ID. The guest and the actual Level are separate native Bundlables in the same image. Restoration loads both heroes and the level without entering the new-game or starting-equipment paths. The cooperative schedule maps every actor ID to its concrete class, priority, clock and frozen flag. The selected input actor and rebased world time are restored after level registration. Frozen clocks retain the remaining delay until revival. The game verifies the roster, identity uniqueness, supported floor dimensions, life states and input offer before opening the room.

Each seat has its mapped and visited bitsets and last observed terrain. Visible cells, combat zones and seat projections are recomputed. Native heap order and each hero's native belongings traversal identify the physical objects in this schema. The companion table validates object class and quantity and reattaches private item identity and owner. The item-generation ceiling survives stack consumption. The native save and the companion table belong to the same generation.

The cooperative random stack stores the 48-bit state of each Java-compatible generator in deque order. This restores the next draw from the captured streams. Future unseeded generator creation, unordered gameplay collection iteration and unsupported content have no cross-runtime deterministic replay guarantee.

## Disk commit

One generation file contains a four-byte big-endian length, a SHA-256 digest and an uncompressed UTF-8 Bundle envelope binding schema, session UUID, generation and payload. The payload limit is 4 MiB and JSON nesting is bounded. Checksums detect file corruption. The directory is private application storage and is not a network import interface.

A single writer holds an OS file lock. It writes and syncs a temporary generation, verifies the image, renames it in the session directory, syncs that directory, then writes and syncs the replacement HEAD. HEAD is published by same-directory rename followed by directory sync. HEAD contains the current and backup numbers and their bitwise complements. Interrupted staging files and unpublished generations never select a game. The next writer overwrites its staging files. Generation commits retain the previous published generation. A second, synced BACKUP record names the last known published fallback. If HEAD is missing or damaged, normal resume fails and only an explicit previous-generation restore can use that record. Losing both records requires manual investigation. A damaged current generation requires explicit selection of a validated backup. Rollback is surfaced to the controller.

JVM tests exercise process death on Linux. On Android the directory sync path uses android.system.Os. Device storage durability under power loss requires device testing.
