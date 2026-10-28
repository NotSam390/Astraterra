# How the mod is organised

```
com.astraterra.mod
├── Astraterra.java          entry point: only hooks the registers up
├── registry/                every DeferredRegister (one file per kind of thing)
│   ├── ModBlocks            all blocks, grouped by planet with comment headers
│   ├── ModItems             all items, grouped by planet
│   ├── ModBlockEntities
│   ├── ModMenus
│   ├── ModCreativeTabs      fills itself: never edit it to add an item
│   └── ModStructures        structure types (crater...)
├── planet/                  the planet system
│   ├── Planet               what a planet is (gravity, air, sky)
│   ├── Planets              THE list of planets  <- you add planets here
│   ├── PlanetGravity        gravity for every planet
│   └── PlanetOxygen         suffocation for every airless planet
├── block/  block/entity/    blocks with behaviour (oxygen station)
├── item/                    items with behaviour (space suit)
├── menu/                    container menus
├── worldgen/structure/      structure code (craters), reused by every planet
├── client/
│   ├── sky/                 sky renderer, dimension effects, sky textures
│   ├── screen/              GUIs
│   └── renderer/            block entity renderers
└── mixin/
```

The rule: **a Java class is written for a *kind* of thing, never for one planet.**
Gravity, oxygen, sky, night darkness and craters are all generic and read their
settings from `Planets` or from JSON. A new planet is data, not code.

## Adding a planet (example: Mars)

### 1. Java: one entry in `planet/Planets.java`

```java
public static final Planet MARS = register(Planet.builder("mars")
        .gravity(0.38)
        .noAtmosphere()                       // needs a suit, dark nights
        .sky(Planet.space()
                .stars(15000)
                .sunSize(0.7F)
                .body("phobos_map", 0.4F, 0.6F, 0.2F, 2.0F, 0.0F))
        .build());
```

That's it for code. Gravity, oxygen, particles, fall damage, the sky, the
lighting mixin and the dimension effects all pick it up.

### 2. Blocks/items: a section in `ModBlocks` / `ModItems`

```java
// ------------------------------------------------------------ Mars
public static final RegistryObject<Block> MARS_SAND = registerBlock("mars_sand", () -> ...);
```

They appear in the creative tab automatically. Only write a new block *class*
if the block does something special.

### 3. Data and assets

Everything for a planet goes under a folder named after it where Minecraft allows it:

| What | Where |
|---|---|
| Dimension | `data/astraterra/dimension/mars.json` (name must match the planet) |
| Dimension type | `data/astraterra/dimension_type/mars.json` with `"effects": "astraterra:mars"` |
| Terrain | `data/astraterra/worldgen/noise_settings/mars.json` |
| Biome(s) | `data/astraterra/worldgen/biome/mars.json` |
| Craters | `data/astraterra/worldgen/structure/mars/crater_*.json` (set `floor_block` / `rock_block`) |
| Crater placement | `data/astraterra/worldgen/structure_set/mars_craters.json` |
| Block textures | `assets/astraterra/textures/block/mars/` |
| Item textures | `assets/astraterra/textures/item/mars/` |
| Sky body maps | `assets/astraterra/textures/environment/phobos_map.png` (2:1) |

Plus, per block: blockstate, block model, item model, loot table, a line in
`en_us.json` and a mining tag. Copy a Moon one and rename.

## Next step to cut the JSON work: data generators

`build.gradle` already has a `runData` task. Writing a few providers
(`BlockStateProvider`, `ItemModelProvider`, `BlockLootSubProvider`,
`LanguageProvider`, `BlockTagsProvider`) would generate all the per-block JSON
from `ModBlocks`, so a simple block becomes one line of Java plus a texture.
