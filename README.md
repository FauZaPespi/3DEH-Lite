# 3DEH (Lite)

3D Display Entity Holograms — holograms built on Minecraft's native display entities instead of
armor stands.

Display entities are rendered entirely client-side. They are not ticked, they have no collision box
and no AI, so a server running a hundred of them spends effectively no time on them per tick. 3DEH
spawns them non-persistently and rebuilds them from its own data file, which means the world save
never accumulates hologram entities and a crash cannot leave orphans behind.

## Requirements

| | |
|---|---|
| Server | Paper 1.20.6 – 1.21.x, or Folia |
| Java | 21 or newer |

Spigot is not supported. 3DEH uses the Paper command API (Brigadier), Adventure components, and the
regional schedulers, none of which exist in Spigot.

Folia is supported natively: every task is dispatched through the regional scheduler API, which
Paper implements on top of its own main thread. The same jar runs unmodified on both.

## Installation

1. Download `3DEH-Lite.jar` from the [releases](https://github.com/FauZaPespi/3DEH-Lite/releases),
   or from the artifacts of any build on the Actions tab.
2. Drop it into `plugins/`.
3. Start the server. `config.yml` and `data.yml` are created on first run.

## Concepts

A **hologram** is a named group of **parts** anchored at one location. Each part is one display
entity — text, block, or item — positioned relative to that anchor by its offset. Moving the
hologram moves every part with it.

```
/3deh create text shop &6&lMarket
/3deh part add shop item diamond
/3deh part offset shop 1 0 0.6 0
/3deh edit shop billboard vertical
```

Parts are centred on their position, block parts included, and they hold still. A part only turns
towards the player once you ask it to, per part: `/3deh edit shop billboard center`.

Text supports MiniMessage, legacy `&` codes including `&#rrggbb`, or plain text; the dialect is
detected per hologram, so text pasted from an older plugin keeps working. Use `\n` for a line break.

## Commands

Every command is registered through Brigadier, so tab completion, argument validation and error
messages are native. Arguments in `[brackets]` are optional and fall back to the hologram you have
selected with `/3deh select`.

| Command | Description |
|---|---|
| `/3deh help` | List the commands you have access to |
| `/3deh create <text\|block\|item> <name> <value>` | Create a hologram where you stand |
| `/3deh remove [name]` | Delete a hologram |
| `/3deh list [page]` | List every hologram |
| `/3deh info [name]` | Show a hologram's parts and attributes |
| `/3deh select` | Select the hologram you are looking at |
| `/3deh deselect` | Clear your selection |
| `/3deh move [name]` | Re-anchor a hologram at your position |
| `/3deh teleport <name>` | Teleport yourself to a hologram |
| `/3deh edit [name] <attribute> <value>` | Edit the hologram's first part |
| `/3deh part add <name> <text\|block\|item> <value>` | Add a part |
| `/3deh part remove <name> <index>` | Remove a part |
| `/3deh part list <name>` | List a hologram's parts |
| `/3deh part offset <name> <index> <x> <y> <z>` | Position a part relative to the anchor |
| `/3deh part edit <name> <index> <attribute> <value>` | Edit one part |
| `/3deh reload` | Reload `config.yml` and `data.yml` |

### Attributes

Available to every part type:

`scale`, `rotation`, `translation`, `billboard`, `viewrange`, `shadow`, `glow`, `brightness`,
`width`, `height`, `interpolation`, `teleportduration`

Text parts only:

`text`, `linewidth`, `opacity`, `textshadow`, `seethrough`, `background`, `alignment`

Block parts only: `block`. Item parts only: `item`, `itemtransform`.

Aiming a type-specific attribute at the wrong part reports the mismatch rather than doing nothing.

## Permissions

| Permission | Default | Grants |
|---|---|---|
| `3deh.admin` | op | Every command below |
| `3deh.command.create` | false | `/3deh create` |
| `3deh.command.remove` | false | `/3deh remove` |
| `3deh.command.edit` | false | `/3deh edit` |
| `3deh.command.part` | false | `/3deh part` |
| `3deh.command.list` | false | `/3deh list` |
| `3deh.command.info` | false | `/3deh info` |
| `3deh.command.select` | false | `/3deh select`, `/3deh deselect` |
| `3deh.command.move` | false | `/3deh move` |
| `3deh.command.teleport` | false | `/3deh teleport` |
| `3deh.command.reload` | false | `/3deh reload` |
| `3deh.command.help` | true | `/3deh help` |

Branches you lack permission for are hidden from tab completion.

## Building locally

```
git clone https://github.com/FauZaPespi/3DEH-Lite.git
cd 3DEH-Lite
./gradlew build
```

The jar lands in `build/libs/`. Gradle provisions a JDK 21 toolchain automatically if none is
installed. `./gradlew test` runs the unit suite on its own.

## Documentation

Detailed guides live on the [GitHub wiki](https://github.com/FauZaPespi/3DEH-Lite/wiki):
[Home](https://github.com/FauZaPespi/3DEH-Lite/wiki/Home),
[Commands and Permissions](https://github.com/FauZaPespi/3DEH-Lite/wiki/Commands-and-Permissions),
[Configuration](https://github.com/FauZaPespi/3DEH-Lite/wiki/Configuration),
[Developer API](https://github.com/FauZaPespi/3DEH-Lite/wiki/Developer-API).
