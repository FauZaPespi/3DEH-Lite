# Configuration

3DEH keeps two files in `plugins/3DEH-Lite/`:

- `config.yml` — settings and messages
- `data.yml` — the holograms themselves

Both are re-read by `/3deh reload`, which despawns every hologram, reloads from disk, and respawns
whatever sits in a loaded chunk.

## config.yml

### defaults

Applied to newly created parts only. Changing them does not touch existing holograms.

```yaml
defaults:
  billboard: CENTER          # FIXED, VERTICAL, HORIZONTAL, CENTER
  view-range: 1.0
  shadow-radius: 0.0
  shadow-strength: 1.0
  text:
    line-width: 200
    see-through: false
    text-shadow: false
    alignment: CENTER
```

`billboard` decides how a part turns toward the viewer:

| Mode | Behaviour |
|---|---|
| `FIXED` | Never rotates. Use for parts whose orientation you set by hand |
| `VERTICAL` | Turns around the Y axis only, staying upright |
| `HORIZONTAL` | Turns around the horizontal axis only |
| `CENTER` | Always faces the player fully. The usual choice for text |

`view-range` multiplies the client's entity render distance rather than setting a block distance.
`1.0` is vanilla; `0.5` halves it; `0` makes the part invisible.

### selection

```yaml
selection:
  max-distance: 24.0
  max-angle: 12.0
```

`/3deh select` picks the hologram nearest the centre of your view. `max-angle` is the half-angle of
that cone in degrees — raise it if selecting feels fussy, lower it if you keep grabbing the wrong
hologram in a dense area. Values are clamped to 1–89.

### storage

```yaml
storage:
  autosave-interval: 300
```

Seconds between automatic saves. Every command that changes something also queues an immediate
asynchronous save, and a save always runs on shutdown, so this is a safety net for a server that dies
without a clean stop. `0` disables the timer.

### messages

Every operator-facing string, in MiniMessage. `prefix` is prepended to all of them except the
multi-line help and list output.

Placeholders are substituted literally, so a hologram named with markup characters cannot inject
formatting:

| Placeholder | Used in |
|---|---|
| `<name>` | Most messages |
| `<attribute>` | `updated`, `wrong-part-type` |
| `<index>` | Part messages |
| `<expected>`, `<actual>` | `wrong-part-type` |
| `<count>` | `reloaded` |
| `<page>`, `<pages>` | `list-header` |

## data.yml

Holograms are stored under a single `holograms` section keyed by name. **Only attributes you
actually changed are written**, so a freshly created hologram is a handful of lines rather than
twenty-five.

```yaml
holograms:
  shop:
    world: world
    x: 120.5
    y: 65.0
    z: -43.5
    parts:
      - type: text
        text: '&6&lMarket'
      - type: item
        offset: [0.0, 0.6, 0.0]
        item: DIAMOND
        item-transform: HEAD
```

### Part keys

| Key | Type | Default |
|---|---|---|
| `type` | `text`, `block`, `item` | required |
| `offset` | `[x, y, z]` | `[0, 0, 0]` |
| `scale` | `[x, y, z]` | `[1, 1, 1]` |
| `translation` | `[x, y, z]` | `[0, 0, 0]` |
| `rotation` | `[yaw, pitch, roll]` in degrees | `[0, 0, 0]` |
| `billboard` | `FIXED`/`VERTICAL`/`HORIZONTAL`/`CENTER` | `CENTER` |
| `view-range` | number | `1.0` |
| `shadow-radius` / `shadow-strength` | number | `0.0` / `1.0` |
| `width` / `height` | number | `0.0` |
| `interpolation-delay` / `interpolation-duration` | ticks | `0` |
| `teleport-duration` | ticks, 0–59 | `0` |
| `glow` | `#rrggbb` or a colour name | unset |
| `brightness` | `[block, sky]`, each 0–15 | unset |

Text parts add `text` (required), `line-width`, `opacity`, `text-shadow`, `see-through`,
`default-background`, `background`, `alignment`. Block parts require `block`. Item parts require
`item` and accept `item-transform`.

### Editing by hand

This is a supported workflow. Edit the file, then run `/3deh reload`.

Values are validated on load, and a bad entry is skipped with a warning naming the hologram, the part
index and the offending key — the rest of the file still loads. A typo in one hologram never costs
you the others:

```
[3DEH-Lite] Skipping hologram 'shop': Hologram 'shop' part #1: Unknown billboard mode 'SIDEWAYS'
```

Writes go through a temporary file and an atomic rename, so an interrupted save cannot truncate
`data.yml`.

## Orphaned entities

Parts are spawned with persistence disabled, so the server never writes them into a region file.
That removes the usual source of orphaned holograms entirely — there is nothing on disk to restore
after a crash.

As a second line of defence, every spawned entity carries a persistent-data tag naming its hologram.
When a chunk's entities load, any tagged entity found there is removed and the holograms belonging to
that chunk are respawned from `data.yml`. Leftovers from an older install, or entities summoned by
hand, are cleaned up the first time their chunk loads.
