# Hot Metal Smithing System - Implementation Pattern

This document describes the generalized pattern for adding new hot metals (copper, bronze, steel, etc.) to the smithing system.

## Overview

The smithing system uses a **generalized interface-based approach** that allows any hot metal to work with the same GUI, event handlers, and interactions. This eliminates code duplication and makes adding new materials straightforward.

## Core Architecture

### 1. **Interface: `IHotMetalAnvilEntity`**
Located at: `block/entity/IHotMetalAnvilEntity.java`

All hot metal block entities must implement this interface:
```java
public interface IHotMetalAnvilEntity {
    int getHammerStrikes();
    String getSelectedComponent();
    void setSelectedComponent(String component);
    boolean isFinished();
    float getProgress();
    boolean hasGuiBeenOpened();
    void setGuiOpened(boolean opened);
    void onHammerStrike(Player player);
    String getMaterialType(); // Returns "iron", "gold", "copper", etc.
}
```

### 2. **Shared GUI System**
- `ComponentSelectionMenu` - Works with any `IHotMetalAnvilEntity`
- `ComponentSelectionScreen` - Material-agnostic UI
- `SelectComponentPacket` - Generic network packet for component selection

### 3. **Unified Event Handling**
- `SmithingHandler` uses the interface to handle all hot metals uniformly
- `PinzaItem` uses the interface for pickup/placement operations

## Adding a New Hot Metal (e.g., Copper)

Follow these steps to add a new hot metal material:

### Step 1: Create the Hot Metal Item
```java
// item/HotCopperItem.java
public class HotCopperItem extends Item {
    public HotCopperItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Makes it glow
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && !level.isClientSide) {
            player.getInventory().removeItem(stack);
            stack.shrink(stack.getCount());
            player.hurt(level.damageSources().inFire(), 2.0F);
            player.displayClientMessage(
                Component.translatable("item.tharidiathings.hot_copper.burned_hands"), 
                true
            );
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
```

### Step 2: Create the Marker Block
```java
// block/HotCopperMarkerBlock.java
public class HotCopperMarkerBlock extends Block implements EntityBlock {
    public HotCopperMarkerBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .noCollission()
            .noOcclusion()
            .air()
            .noLootTable()
        );
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotCopperAnvilEntity(pos, state);
    }
}
```

### Step 3: Create the Block Entity (Implements Interface!)
```java
// block/entity/HotCopperAnvilEntity.java
public class HotCopperAnvilEntity extends BlockEntity implements IHotMetalAnvilEntity {
    
    private int hammerStrikes = 0;
    private String selectedComponent = "lama_lunga";
    private boolean finished = false;
    private boolean guiOpened = false;
    
    public HotCopperAnvilEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HOT_COPPER_ANVIL_ENTITY.get(), pos, state);
    }
    
    public HotCopperAnvilEntity(BlockPos pos, Level level) {
        this(pos, level.getBlockState(pos));
    }
    
    @Override
    public void onHammerStrike(Player player) {
        if (finished) return;
        
        hammerStrikes++;
        setChanged();
        
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            if (hammerStrikes >= 4) {
                finished = true;
                if (player != null) {
                    TharidiaThings.LOGGER.info(player.getName().getString() + " forged a copper piece");
                }
                level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
            
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    @Override
    public int getHammerStrikes() { return hammerStrikes; }
    
    @Override
    public String getSelectedComponent() { return selectedComponent; }
    
    @Override
    public void setSelectedComponent(String component) {
        this.selectedComponent = component;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public boolean isFinished() { return finished; }
    
    @Override
    public float getProgress() { return hammerStrikes / 4.0f; }
    
    @Override
    public boolean hasGuiBeenOpened() { return guiOpened; }
    
    @Override
    public void setGuiOpened(boolean opened) {
        this.guiOpened = opened;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public String getMaterialType() {
        return "copper"; // IMPORTANT: Return the material type identifier
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HammerStrikes", hammerStrikes);
        tag.putString("SelectedComponent", selectedComponent);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("GuiOpened", guiOpened);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hammerStrikes = tag.getInt("HammerStrikes");
        selectedComponent = tag.getString("SelectedComponent");
        finished = tag.getBoolean("Finished");
        guiOpened = tag.getBoolean("GuiOpened");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

### Step 4: Create the Renderer
```java
// client/renderer/HotCopperAnvilRenderer.java
public class HotCopperAnvilRenderer implements BlockEntityRenderer<HotCopperAnvilEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    private BakedModel hotCopperModel;
    
    public HotCopperAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotCopperModel = null;
    }
    
    @Override
    public boolean shouldRenderOffScreen(HotCopperAnvilEntity entity) {
        return true;
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
    
    @Override
    public void render(HotCopperAnvilEntity entity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        
        if (hotCopperModel == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_copper_anvil")
            );
            hotCopperModel = modelManager.getModel(modelLocation);
        }
        
        poseStack.pushPose();
        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotCopperModel,
            1.0f, 1.0f, 1.0f,
            combinedLight,
            combinedOverlay,
            ModelData.EMPTY,
            null
        );
        
        poseStack.popPose();
    }
}
```

### Step 5: Register in TharidiaThings.java
```java
// Add to TharidiaThings.java

// Blocks
public static final DeferredBlock<HotCopperMarkerBlock> HOT_COPPER_MARKER = 
    BLOCKS.register("hot_copper_marker", () -> new HotCopperMarkerBlock());

// Block Entities
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotCopperAnvilEntity>> HOT_COPPER_ANVIL_ENTITY =
    BLOCK_ENTITIES.register("hot_copper_anvil", () -> 
        BlockEntityType.Builder.of(HotCopperAnvilEntity::new, HOT_COPPER_MARKER.get()).build(null));

// Items
public static final DeferredItem<Item> HOT_COPPER = 
    ITEMS.register("hot_copper", () -> 
        new HotCopperItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

// Creative Tab
.displayItems((parameters, output) -> {
    // ... existing items ...
    output.accept(HOT_COPPER.get());
})
```

### Step 6: Register Client-Side (TharidiaThingsClient.java)
```java
// In onClientSetup
BlockEntityRenderers.register(TharidiaThings.HOT_COPPER_ANVIL_ENTITY.get(), HotCopperAnvilRenderer::new);

// In onRegisterAdditionalModels
event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
    ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_copper_anvil")
));
```

### Step 7: Update SmithingHandler - Marker Detection
```java
// In onRightClickBlock, update the marker check:
boolean isCopperMarker = level.getBlockState(pos).is(TharidiaThings.HOT_COPPER_MARKER.get());
boolean isMarkerBlock = isIronMarker || isGoldMarker || isCopperMarker;

// And in the second check:
isCopperMarker = level.getBlockState(checkPos).is(TharidiaThings.HOT_COPPER_MARKER.get());
isMarkerBlock = isIronMarker || isGoldMarker || isCopperMarker;

// The rest of SmithingHandler already works via the interface!
```

### Step 8: Update SmithingHandler - Table Interaction
```java
// In onPlayerInteractBlock, add copper detection:
if (!slotItem.isEmpty() && slotItem.is(TharidiaThings.HOT_COPPER.get())) {
    hasHotMetal = true;
    isCopper = true;
    break;
}

// And in the pinza grab section:
else if (isCopper) {
    grabHotCopperWithPinza(container, pos, level, event.getEntity(), heldItem);
}

// Create grabHotCopperWithPinza method (copy from grabHotIronWithPinza, change item type)
```

### Step 9: Update SmithingHandler - Item Crafting Prevention
```java
// In onItemCrafted:
if (event.getCrafting().is(TharidiaThings.HOT_COPPER.get())) {
    event.getCrafting().shrink(event.getCrafting().getCount());
    if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
        event.getEntity().displayClientMessage(
            Component.translatable("item.tharidiathings.hot_copper.too_hot"),
            true
        );
    }
}
```

### Step 10: Update SmithingHandler - Player Inventory & Entity Spawn
```java
// In onPlayerTick:
boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());
if (isHotIron || isHotGold || isHotCopper) {
    // ... rest of logic with additional checks for isHotCopper
}

// In onEntityJoinWorld:
boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());
if (isHotIron || isHotGold || isHotCopper) {
    // ... rest of logic with additional checks for isHotCopper
}
```

### Step 11: Update PinzaItem
```java
// Add to HoldingType enum:
HOT_COPPER,

// In setHoldingWithMaterial method:
else if (type == HoldingType.HOT_COPPER) {
    stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(5)); // Next available ID
}

// In placeHotMetalOnAnvil method:
else if (holdingType == HoldingType.HOT_COPPER) {
    level.setBlock(above, TharidiaThings.HOT_COPPER_MARKER.get().defaultBlockState(), 3);
}

// In getComponentStack method - ADD new material case:
case "copper" -> switch (componentId) {
    case "lama_lunga" -> new ItemStack(TharidiaThings.COPPER_LAMA_LUNGA.get());
    case "lama_corta" -> new ItemStack(TharidiaThings.COPPER_LAMA_CORTA.get());
    default -> ItemStack.EMPTY;
};
```

**NOTE**: Material tracking is now automatic! The `getMaterialType()` from the interface is used to track which metal the component is made from.

### Step 12: Update ComponentSelectionMenu
```java
// In stillValid method:
return (state.is(TharidiaThings.HOT_IRON_MARKER.get()) || 
        state.is(TharidiaThings.HOT_GOLD_MARKER.get()) ||
        state.is(TharidiaThings.HOT_COPPER_MARKER.get())) &&
       player.distanceToSqr(...) <= 64.0;
```

### Step 13: Create Material-Specific Component Items
```java
// item/CopperLamaLungaItem.java
public class CopperLamaLungaItem extends Item {
    public CopperLamaLungaItem(Properties properties) {
        super(properties);
    }
}

// item/CopperLamaCortaItem.java
public class CopperLamaCortaItem extends Item {
    public CopperLamaCortaItem(Properties properties) {
        super(properties);
    }
}

// Register in TharidiaThings.java:
public static final DeferredItem<Item> COPPER_LAMA_LUNGA = 
    ITEMS.register("copper_lama_lunga", () -> new CopperLamaLungaItem(new Item.Properties()));
public static final DeferredItem<Item> COPPER_LAMA_CORTA = 
    ITEMS.register("copper_lama_corta", () -> new CopperLamaCortaItem(new Item.Properties()));

// Add to creative tab
output.accept(COPPER_LAMA_LUNGA.get());
output.accept(COPPER_LAMA_CORTA.get());
```

### Step 14: Create Slag Recipe Override
```
data/slag/recipe/casting/table/copper_ingot.json
```
```json
{
  "type": "slag:table_casting",
  "cast": "slag:cast/ingots",
  "ingredient": {
    "amount": 72,
    "id": "slag:molten_copper"
  },
  "result": {
    "count": 1,
    "id": "tharidiathings:hot_copper"
  }
}
```

### Step 15: Create Assets
Required files:
- `textures/item/hot_copper.png`
- `textures/item/pinza_hot_copper.png` 
- `textures/block/hot_copper.png`
- `models/item/hot_copper.json`
- `models/item/pinza_hot_copper.json`
- `models/block/hot_copper_anvil.json`

Update `models/item/pinza.json`:
```json
{
  "predicate": {
    "custom_model_data": 4
  },
  "model": "tharidiathings:item/pinza_hot_copper"
}
```

## Key Benefits of This Pattern

1. **No GUI Duplication** - ComponentSelectionMenu works for all metals
2. **No Network Code Duplication** - SelectComponentPacket is generic
3. **Unified Event Handling** - SmithingHandler logic is mostly shared
4. **Easy Maintenance** - Interface changes propagate to all metals
5. **Scalable** - Adding 10 more metals doesn't multiply complexity
6. **Automatic Material Tracking** - Components remember their metal type via the interface
7. **Material-Specific Results** - Cooling iron components gives iron blades, gold gives gold blades

## Critical Implementation Details

### GUI Anti-Flicker System
The GUI has a 20-tick (1 second) delay system to prevent rapid open/close:
- `guiOpened` flag prevents reopening on subsequent strikes
- Must call `setGuiOpened(true)` before opening menu
- Must sync the flag to client via `level.sendBlockUpdated()`

### Client-Server Synchronization
All entity state changes must explicitly sync:
```java
if (level != null && !level.isClientSide) {
    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
}
```

### Pinza Custom Model Data
Assign unique IDs sequentially:
- 1 = HOT_IRON
- 2 = COMPONENT  
- 3 = HOT_GOLD
- 4 = (reserved for future use)
- 5 = HOT_COPPER (next available)
- etc.

### Material Tracking System
When a component is picked up from the anvil:
1. `getMaterialType()` is called on the entity to get "iron", "gold", etc.
2. Material is stored in the pinza's NBT data along with component type
3. When cooling, `getComponentStack(componentId, materialType)` creates the correct item
4. This automatically ensures iron components → iron blades, gold → gold blades

## Testing Checklist

When adding a new metal, verify:
- [ ] Slag table creates hot metal (not vanilla ingot)
- [ ] Hot metal cannot be held in inventory (burns player)
- [ ] Pinza can pick up hot metal from slag table
- [ ] Pinza can place hot metal on anvil
- [ ] First hammer strike opens GUI only once
- [ ] GUI doesn't flicker on subsequent strikes
- [ ] 4 hammer strikes completes the forging
- [ ] Pinza can pick up finished component
- [ ] Component can be cooled in water cauldron
- [ ] Hot metal cannot be crafted/smelted
- [ ] Hot metal cannot spawn as item entity

## Future Enhancements

Consider these potential improvements:
- Material-specific hammer strike requirements (harder metals = more strikes)
- Material-specific component quality (copper = lower tier components)
- Temperature decay system (hot metal cools over time)
- Reheating mechanism (place back in forge)
- Alloy system (combine multiple hot metals)
