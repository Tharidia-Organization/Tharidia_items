# Debug Instructions - Hot Iron Issue (v1.1.1)

## Changes Made

I've added extensive debug logging to trace exactly what's happening when you try to grab hot iron with the Pinza.

### Log Points Added:

1. **SmithingHandler.onPlayerInteractBlock()**
   - "HOT IRON DETECTED IN TABLE!" - When hot iron is found
   - "EVENT CANCELED" - When event is canceled
   - "PINZA DETECTED! Holding type: X" - When Pinza is detected
   - "CALLING grabHotIronWithPinza" - Before calling grab method

2. **SmithingHandler.grabHotIronWithPinza()**
   - "grabHotIronWithPinza called" - Method entry
   - "Found hot iron in slot: X" - When hot iron found in container
   - "Removing hot iron from slot: X" - Before removal
   - "Setting Pinza to hold hot iron" - Before setHolding
   - "Pinza stack before: X" - Pinza state before
   - "Pinza stack after: X" - Pinza state after
   - "Pinza holding type: X" - Holding type after setHolding
   - "grabHotIronWithPinza completed" - Method exit

3. **SmithingHandler.onItemCrafted()**
   - "HOT IRON CRAFTED EVENT FIRED!" - When hot iron is crafted
   - "Hot iron stack: X" - The crafted stack
   - "After shrink: X" - After shrinking to 0

4. **HotIronItem.inventoryTick()**
   - "HOT IRON IN PLAYER INVENTORY! Slot: X" - When hot iron is in inventory

## How to Debug

### Step 1: Install the Mod
Copy `build/libs/tharidiathings-1.1.1.jar` to your mods folder

### Step 2: Run Minecraft
Start the game and watch the console/logs

### Step 3: Test the Scenario
1. Pour molten iron into mold (creates hot iron in Slag table)
2. Right-click the table with an empty Pinza
3. **Watch the console output**

### Step 4: Analyze the Logs

Look for the sequence of events in the logs:

#### Expected Sequence (if our event works):
```
[INFO] HOT IRON DETECTED IN TABLE!
[INFO] EVENT CANCELED
[INFO] PINZA DETECTED! Holding type: NONE
[INFO] CALLING grabHotIronWithPinza
[INFO] grabHotIronWithPinza called
[INFO] Found hot iron in slot: X
[INFO] Removing hot iron from slot: X
[INFO] Setting Pinza to hold hot iron
[INFO] Pinza stack before: ...
[INFO] Pinza stack after: ...
[INFO] Pinza holding type: HOT_IRON
[INFO] grabHotIronWithPinza completed
```

#### If Hot Iron Crafted Event Fires:
```
[INFO] HOT IRON CRAFTED EVENT FIRED!
[INFO] Hot iron stack: 1 tharidiathings:hot_iron
[INFO] After shrink: 0 air
```

#### If Hot Iron Gets Into Inventory:
```
[WARN] HOT IRON IN PLAYER INVENTORY! Slot: X, Stack: ...
```

## What to Look For

### Scenario 1: Event Not Firing At All
If you don't see "HOT IRON DETECTED IN TABLE!" → The event isn't being triggered
- **Possible cause**: Not interacting with slag:table
- **Possible cause**: Hot iron not actually in the table

### Scenario 2: Event Fires But grabHotIronWithPinza Not Called
If you see "HOT IRON DETECTED" but not "CALLING grabHotIronWithPinza"
- **Possible cause**: Pinza not detected as PinzaItem
- **Possible cause**: Holding type not NONE
- **Possible cause**: Client side execution

### Scenario 3: grabHotIronWithPinza Runs But Pinza Not Updated
If you see all the grab logs but "Pinza holding type" shows NONE
- **Possible cause**: setHolding() not working
- **Possible cause**: DataComponents not being set properly

### Scenario 4: Hot Iron Still Gets Into Inventory
If you see "HOT IRON IN PLAYER INVENTORY!"
- **Possible cause**: Event canceled but Slag table uses different mechanism
- **Possible cause**: ItemCraftedEvent fires BEFORE our RightClickBlock event

### Scenario 5: ItemCrafted Event Fires
If you see "HOT IRON CRAFTED EVENT FIRED!"
- **This shows when hot iron is created by Slag**
- Should happen when casting completes
- We shrink it to 0 to prevent it being given

## Send Me the Logs

After testing, send me the console output that includes:
1. The moment you right-click the table
2. Any lines with "HOT IRON" or "PINZA" or "grabHot"
3. Any error messages

This will tell us exactly where the process is failing.

## Build Info

**Version**: 1.1.1 (Debug Build)
**Status**: ✅ BUILD SUCCESSFUL
**Output**: `build/libs/tharidiathings-1.1.1.jar`

## Note

This is a **debug build** with lots of logging. Once we figure out the issue, I'll remove the logs and create a clean version.
