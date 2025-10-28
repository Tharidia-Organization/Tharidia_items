# Weight System Documentation

## Overview
The weight system adds realistic inventory weight mechanics to Minecraft. Each item has a weight value, and carrying too much weight applies debuffs to the player.

## Features
- **Item Weights**: Every item has a configurable weight (default: 1.0)
- **Weight Display**: Shows weight under item name in tooltips
- **HUD Indicator**: Visual weight indicator in the lower left corner with color coding
- **Debuffs**: Penalties for carrying too much weight:
  - Reduced movement speed
  - Inability to swim upward when heavily encumbered
- **Datapack Configuration**: Fully customizable via datapacks

## HUD Indicator
The weight indicator appears in the **lower left corner** of the screen:
- **Green** (Normal): Weight is minimal, no penalties
- **Light Green** (Light): Slight weight, minor speed reduction
- **Yellow** (Medium): Moderate weight, noticeable speed reduction
- **Orange** (Heavy): Heavy weight, significant speed reduction + can't swim up
- **Red** (Overencumbered): Maximum weight, severe penalties

## Configuration via Datapacks

### File Location
Create a JSON file at:
```
data/[your_namespace]/weight_config/[filename].json
```

### JSON Structure

```json
{
  "item_weights": {
    "minecraft:stone": 2.0,
    "minecraft:iron_ingot": 3.0,
    "minecraft:feather": 0.1
  },
  "thresholds": {
    "light": 100.0,
    "medium": 200.0,
    "heavy": 300.0,
    "overencumbered": 400.0
  },
  "debuffs": {
    "light_speed_multiplier": 0.95,
    "medium_speed_multiplier": 0.85,
    "heavy_speed_multiplier": 0.70,
    "overencumbered_speed_multiplier": 0.50,
    "heavy_disable_swim_up": true,
    "overencumbered_disable_swim_up": true
  }
}
```

### Configuration Fields

#### item_weights
Maps item IDs to weight values:
- **Format**: `"namespace:item_id": weight_value`
- **Default**: 1.0 for items not specified
- **Examples**:
  - Light items: `0.1 - 0.5` (feathers, paper, string)
  - Normal items: `1.0 - 2.0` (tools, blocks)
  - Heavy items: `3.0 - 10.0` (ore blocks, armor)
  - Very heavy items: `50.0+` (anvils, large stacks)

#### thresholds
Defines weight levels for different status tiers:
- **light**: Weight threshold for "Light" status (default: 100)
- **medium**: Weight threshold for "Medium" status (default: 200)
- **heavy**: Weight threshold for "Heavy" status (default: 300)
- **overencumbered**: Weight threshold for "Overencumbered" status (default: 400)

#### debuffs
Configures penalties for each weight level:
- **light_speed_multiplier**: Speed multiplier when Light (default: 0.95 = 5% slower)
- **medium_speed_multiplier**: Speed multiplier when Medium (default: 0.85 = 15% slower)
- **heavy_speed_multiplier**: Speed multiplier when Heavy (default: 0.70 = 30% slower)
- **overencumbered_speed_multiplier**: Speed multiplier when Overencumbered (default: 0.50 = 50% slower)
- **heavy_disable_swim_up**: Disable swimming upward when Heavy (default: true)
- **overencumbered_disable_swim_up**: Disable swimming upward when Overencumbered (default: true)

## Example Configurations

### Hardcore Mode (Stricter Penalties)
```json
{
  "thresholds": {
    "light": 50.0,
    "medium": 100.0,
    "heavy": 150.0,
    "overencumbered": 200.0
  },
  "debuffs": {
    "light_speed_multiplier": 0.90,
    "medium_speed_multiplier": 0.75,
    "heavy_speed_multiplier": 0.60,
    "overencumbered_speed_multiplier": 0.40,
    "heavy_disable_swim_up": true,
    "overencumbered_disable_swim_up": true
  }
}
```

### Casual Mode (Lenient Penalties)
```json
{
  "thresholds": {
    "light": 200.0,
    "medium": 400.0,
    "heavy": 600.0,
    "overencumbered": 800.0
  },
  "debuffs": {
    "light_speed_multiplier": 0.98,
    "medium_speed_multiplier": 0.92,
    "heavy_speed_multiplier": 0.85,
    "overencumbered_speed_multiplier": 0.70,
    "heavy_disable_swim_up": false,
    "overencumbered_disable_swim_up": true
  }
}
```

### Realistic Weights
```json
{
  "item_weights": {
    "minecraft:stone": 2.5,
    "minecraft:cobblestone": 2.5,
    "minecraft:iron_ore": 5.0,
    "minecraft:iron_ingot": 4.0,
    "minecraft:iron_block": 36.0,
    "minecraft:gold_ore": 8.0,
    "minecraft:gold_ingot": 6.0,
    "minecraft:gold_block": 54.0,
    "minecraft:diamond": 3.0,
    "minecraft:netherite_ingot": 8.0,
    "minecraft:anvil": 80.0,
    "minecraft:feather": 0.05,
    "minecraft:paper": 0.05,
    "minecraft:wheat": 0.3,
    "minecraft:bread": 0.4,
    "minecraft:water_bucket": 10.0
  }
}
```

## How Weight is Calculated

Total weight = Sum of (Item Weight × Stack Count) for all items in:
- Main inventory (27 slots)
- Hotbar (9 slots)
- Armor slots (4 slots)
- Offhand (1 slot)

**Example**:
- 32 Stone (weight 2.0 each) = 64 weight
- 1 Iron Sword (weight 2.5) = 2.5 weight
- 1 Iron Chestplate (weight 5.0) = 5.0 weight
- **Total**: 71.5 weight (Normal status)

## Reloading Configuration

Use `/reload` command to reload datapacks and apply changes to weight configuration.

## Default Weights

See `data/tharidiathings/weight_config/default.json` for the complete list of default weights.

Key defaults:
- Most items: 1.0
- Stone/blocks: 2.0
- Iron items: 3.0
- Gold items: 4.0
- Diamonds: 5.0
- Netherite: 6.0
- Anvils: 50.0
- Light items (feathers, paper): 0.1

## Tips for Balancing

1. **Base Weight**: Keep common items around 1.0
2. **Stack Consideration**: Remember players can carry 64 of most items
3. **Armor Weight**: Balance so full armor sets are meaningful but not crippling
4. **Resources**: Make ore blocks heavier than their ingot equivalents
5. **Tools**: Weight should reflect material (wood < stone < iron < diamond < netherite)

## Technical Notes

- Weight calculation updates every second (every 20 ticks)
- Speed modifiers use Minecraft's attribute system
- Swimming restriction prevents upward water movement
- All configuration is server-side synced to clients
- HUD overlay is client-side only

## Troubleshooting

**Weight not showing?**
- Check that the datapack is loaded with `/datapack list`
- Verify JSON syntax is correct
- Check server logs for parsing errors

**Debuffs not applying?**
- Ensure server is running (single-player or multiplayer)
- Check that weight thresholds are exceeded
- Verify debuff values are between 0.0 and 1.0

**HUD not displaying?**
- Client-side only - check you're on the client
- Verify mod is loaded with F3+C (check mod list)
