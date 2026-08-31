# Developer API

3DEH exposes its manager and model directly through the plugin instance. There is no separate API
artifact and no `ServicesManager` registration — depend on the plugin jar and cast.

The public surface is `dev.fauza.tdeh.ThreeDehPlugin`, the `model` package, and
`dev.fauza.tdeh.manager.HologramManager`. Everything else is internal and may change between
releases.

## Depending on 3DEH

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly(files("libs/3DEH-Lite-1.0.0.jar"))
}
```

Declare the dependency in your `plugin.yml` so load order is correct:

```yaml
depend: [3DEH-Lite]      # or softdepend, if your plugin works without it
```

## Getting the manager

```java
ThreeDehPlugin threeDeh = (ThreeDehPlugin) getServer().getPluginManager().getPlugin("3DEH-Lite");
HologramManager manager = threeDeh.manager();
```

## Creating a hologram

```java
TextPart title = new TextPart("<gold><bold>Arena</bold></gold>");
title.billboard(BillboardMode.CENTER);
title.scale(Vec3.of(1.5));

ItemPart icon = new ItemPart("diamond_sword");
icon.offset(new Vec3(0, 0.8, 0));
icon.transform(ItemTransform.HEAD);

Hologram hologram = new Hologram(
        "arena",
        new HologramLocation("world", 120.5, 65.0, -43.5),
        List.of(title, icon));

manager.create(hologram);
manager.requestSave();
```

`create` registers the hologram, indexes it by chunk, and spawns it if that chunk is loaded. It does
not save on its own — call `requestSave()` when you are done with a batch of changes.

## Editing

Two entry points, and picking the right one matters:

```java
// Attribute change: pushed to the live entity, no respawn, no flicker.
manager.editPart(hologram, 0, part -> part.scale(Vec3.of(2)));

// Structural change: the anchor, or the part list. Despawns and respawns.
manager.restructure(hologram, () -> hologram.addPart(new ItemPart("emerald")));

manager.requestSave();
```

Part indices back the live entity list positionally, so anything that shifts them — adding a part,
removing one, changing an offset, moving the anchor — has to go through `restructure`.

## Reading

```java
Optional<Hologram> found = manager.find("arena");   // case-insensitive
Collection<Hologram> all = manager.all();
boolean live = manager.isSpawned("arena");
```

## Threading

3DEH is written for Folia, and any plugin touching its model has to respect the same rules.

**Model objects are mutable and shared.** `HologramManager` synchronises on the `Hologram` instance
while mutating or encoding it, so the autosave never sees a half-applied edit. If you mutate a part
outside `editPart`/`restructure`, synchronise on the hologram yourself:

```java
synchronized (hologram) {
    hologram.part(0).viewRange(2.0f);
}
manager.markDirty();
```

**Never touch a display entity directly.** Use `editPart`; it dispatches through the entity's own
scheduler, which is the only form that stays correct when an entity crosses a region boundary.

**Do not use `BukkitScheduler`.** It has no meaning on Folia. `dev.fauza.tdeh.manager.Scheduling`
wraps the regional API if you need the same primitives.

## Validation

Model setters enforce their own bounds and throw `IllegalArgumentException`, so invalid state cannot
reach the renderer:

```java
part.teleportDuration(120);          // rejected: vanilla allows 0-59
new Brightness(16, 0);               // rejected: light levels are 0-15
new TextPart("hi").lineWidth(0);     // rejected: must be positive
```

Catch it and report to your user; 3DEH's own commands do exactly that.

## Text formatting

`dev.fauza.tdeh.text.TextFormat` is a pure utility and safe to call from anywhere:

```java
Component rendered = TextFormat.render("&6&lHUB");        // detects the dialect
String preview = TextFormat.plain("<gold>HUB</gold>");    // "HUB"
TextFormat.Dialect dialect = TextFormat.dialectOf(input);
```

Store the raw string an operator typed, not a serialised component, and let `TextFormat` render it.
That is what 3DEH does, so the text survives a round-trip through `data.yml` unchanged.

## Serialisation

`dev.fauza.tdeh.codec.HologramCodec` converts between a `Hologram` and a plain
`Map<String, Object>` — useful for exporting to your own storage. It has no Bukkit dependency:

```java
Map<String, Object> encoded = HologramCodec.encode(hologram);
Hologram decoded = HologramCodec.decode("arena", encoded);  // throws CodecException on bad input
```
