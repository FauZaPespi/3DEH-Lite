# Commands and Permissions

All commands live under `/3deh`, with the alias `/3dehologram`. They are registered through
Brigadier, so completion and argument validation come from the server: an out-of-range number is
rejected as you type it, and branches you lack permission for never appear in completion.

## Targeting a hologram

Most commands accept the hologram name, or fall back to your **selection** when you omit it.

```
/3deh select              # selects whatever you are looking at
/3deh edit scale 2        # applies to the selection
/3deh edit shop scale 2   # applies to "shop" regardless of selection
```

Selection is per-player and lasts for the session. `/3deh create` selects what it just made.

Because display entities have no hitbox, `/3deh select` cannot raytrace them. It instead picks the
hologram closest to the centre of your view within a cone — tunable via `selection.max-distance` and
`selection.max-angle` in `config.yml`.

## Managing holograms

| Command | Permission |
|---|---|
| `/3deh create <text\|block\|item> <name> <value>` | `3deh.command.create` |
| `/3deh remove [name]` | `3deh.command.remove` |
| `/3deh move [name]` | `3deh.command.move` |
| `/3deh teleport <name>` | `3deh.command.teleport` |
| `/3deh list [page]` | `3deh.command.list` |
| `/3deh info [name]` | `3deh.command.info` |
| `/3deh select` / `/3deh deselect` | `3deh.command.select` |
| `/3deh reload` | `3deh.command.reload` |
| `/3deh help` | `3deh.command.help` (default `true`) |

`3deh.admin` is a parent permission granting all of the above and defaults to op.

Names are 1–32 characters from `a-z`, `A-Z`, `0-9`, `_` and `-`. They are matched
case-insensitively, so `Shop` and `shop` are the same hologram.

`/3deh create` anchors the hologram at your exact position, including your Y. For a floating
hologram, stand where you want it or adjust afterwards with a part offset.

## Managing parts

| Command | Description |
|---|---|
| `/3deh part add <name> <text\|block\|item> <value>` | Append a part; reports its index |
| `/3deh part remove <name> <index>` | Remove a part |
| `/3deh part list <name>` | Show every part with its index |
| `/3deh part offset <name> <index> <x> <y> <z>` | Place a part relative to the anchor |
| `/3deh part edit <name> <index> <attribute> <value>` | Edit one specific part |

All of these need `3deh.command.part`.

`/3deh edit` without a part index always targets part `#0`. Use `/3deh part edit` to reach the
others.

Adding or removing a part renumbers the parts after it and respawns the hologram. Editing an
attribute does not respawn anything — the change is pushed to the live entity, so it is seamless for
anyone already looking at it.

## Attributes

### Every part type

| Attribute | Arguments | Notes |
|---|---|---|
| `scale` | `<x>` or `<x> <y> <z>` | One value scales uniformly |
| `rotation` | `<yaw> <pitch> [roll]` | Degrees. Yaw turns around Y, pitch around X, roll around Z |
| `translation` | `<x> <y> <z>` | Shifts the model inside the entity, before billboarding |
| `billboard` | `fixed\|vertical\|horizontal\|center` | How the part turns to face the viewer |
| `viewrange` | `<multiplier>` | Multiplies the client render distance. `0` hides it |
| `shadow` | `<radius> <strength>` | `0 0` removes the ground shadow |
| `glow` | `<colour>` or `none` | Setting a colour also turns the glow outline on |
| `brightness` | `<block> <sky>` or `none` | Light levels 0–15. `none` follows world lighting |
| `width` / `height` | `<value>` | Culling box. `0` means "never cull" |
| `interpolation` | `<delay> <duration>` | Ticks. Smooths later transformation changes |
| `teleportduration` | `<ticks>` | 0–59; vanilla rejects anything outside that |

### Text parts

| Attribute | Arguments | Notes |
|---|---|---|
| `text` | `<value...>` | MiniMessage, legacy `&` codes, or plain text |
| `linewidth` | `<pixels>` | Width before wrapping. Default 200 |
| `opacity` | `<-1..255>` | `-1` restores the default |
| `textshadow` | `<true\|false>` | Drop shadow behind the glyphs |
| `seethrough` | `<true\|false>` | Renders through blocks |
| `background` | `<colour>`, `none`, or `default` | `none` is fully transparent |
| `alignment` | `center\|left\|right` | Only visible on multi-line text |

### Block and item parts

| Attribute | Arguments |
|---|---|
| `block` | `<block state>`, e.g. `oak_log[axis=y]` |
| `item` | `<material>` |
| `itemtransform` | `none`, `head`, `gui`, `ground`, `fixed`, `thirdperson_lefthand`, … |

Pointing a type-specific attribute at the wrong part reports the mismatch and changes nothing.

## Colours

`glow` and `background` accept:

- `#rrggbb` or bare `rrggbb`
- `#aarrggbb` for `background`, to control transparency
- any of the sixteen vanilla colour names — `red`, `gold`, `aqua`, …
- `none` to clear the override

## Text formatting

The dialect is detected from the string itself:

| Input | Read as |
|---|---|
| `<gold><bold>HUB</bold></gold>` | MiniMessage |
| `&6&lHUB` | Legacy codes |
| `&#ff0055pink` | Legacy hex |
| `Plain words` | Plain text |

MiniMessage wins whenever a tag is present, so a mixed string keeps its tags rather than being eaten
by the legacy pass. Ordinary prose containing `<` or `>` is not mistaken for markup.

Use `\n` for a line break, since a real newline cannot be typed into chat:

```
/3deh create text rules &6Server rules\n&7Be nice\n&7Have fun
```

A single `TextDisplay` renders all the lines, so a multi-line hologram is still one entity.

## Examples

A floating item spinning above a sign:

```
/3deh create item shop diamond
/3deh edit shop scale 0.6
/3deh edit shop billboard fixed
/3deh edit shop rotation 45 0 0
```

A glowing, always-lit title above a block:

```
/3deh create block pedestal minecraft:polished_blackstone
/3deh part add pedestal text <gradient:#ff0055:#ffaa00><bold>SHOP</bold></gradient>
/3deh part offset pedestal 1 0 1.4 0
/3deh part edit pedestal 1 brightness 15 15
/3deh part edit pedestal 1 background none
```
