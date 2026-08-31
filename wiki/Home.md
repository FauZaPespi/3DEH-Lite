# 3DEH (Lite)

3DEH replaces armor-stand holograms with Minecraft's native display entities: `TextDisplay`,
`BlockDisplay` and `ItemDisplay`.

## Why display entities

An armor-stand hologram is a real entity. It ticks, it is saved to the region file, it takes part in
entity collision checks, and a few hundred of them show up in a timings report. A display entity does
none of that — it exists to be drawn by the client and nothing else. There is no AI, no physics, no
collision box, and no per-tick server cost worth measuring.

3DEH spawns them **non-persistently**, so the server never writes them into a region file. The
definitions live in `data.yml`, and the entities are rebuilt from that whenever a chunk loads. Two
consequences worth knowing:

- A crash, a `/stop -9`, or a world rollback cannot leave orphaned holograms in the world.
- Editing `data.yml` by hand and running `/3deh reload` is a supported workflow.

## Requirements

- **Paper 1.20.6 or newer**, or **Folia**. Spigot is not supported.
- **Java 21 or newer**.

Paper 1.20.6 is the floor because it is the first release exposing the Brigadier command API. The
Folia regional schedulers arrived slightly earlier, in 1.20.1, so 1.20.6 satisfies both.

## Folia

3DEH runs unmodified on Folia. There is no separate build and no compatibility mode.

Every scheduled task goes through the regional scheduler API — `RegionScheduler` for work anchored to
a position, `EntityScheduler` for work touching a live entity, `AsyncScheduler` for disk I/O. Paper
implements those interfaces on top of its own main thread, so the same code path is correct on both
platforms and `BukkitScheduler` appears nowhere in the plugin.

## Model

A hologram is a **named group of parts** sharing one anchor location:

```
shop                          <- name, anchored at one x/y/z in one world
├── part #0  text   "Market"  <- offset 0, 0, 0
└── part #1  item   DIAMOND   <- offset 0, 0.6, 0
```

Each part is one display entity with its own offset, scale, rotation and billboard mode. The anchor
is what `/3deh move` changes; part offsets are relative to it, so moving a hologram keeps its layout
intact.

A hologram always has at least one part. Removing the last one is refused — delete the hologram with
`/3deh remove` instead.

## First steps

```
/3deh create text welcome <gradient:#4facfe:#00f2fe><bold>Welcome</bold></gradient>
/3deh edit welcome scale 1.5
/3deh edit welcome billboard center
```

`/3deh create` selects the new hologram automatically, so the follow-up commands can drop the name:

```
/3deh edit scale 1.5
```

Look at an existing hologram and run `/3deh select` to target it the same way.

## Pages

- **[Commands and Permissions](Commands-and-Permissions.md)** — every command, every attribute
- **[Configuration](Configuration.md)** — `config.yml` and the `data.yml` format
- **[Developer API](Developer-API.md)** — using 3DEH from another plugin
