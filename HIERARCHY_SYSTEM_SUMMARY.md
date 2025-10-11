# Realm Hierarchy System Implementation

## Overview
Implemented a complete hierarchy system for realm members that allows the realm owner to manage player ranks based on who has claims within the realm.

## Features Implemented

### 1. Hierarchy Ranks (Ordered by Level)
- **LORD** (Level 4) - Realm owner, cannot be changed
- **CONSIGLIERE** (Level 3) - Advisor
- **GUARDIA** (Level 2) - Guard
- **MILIZIANO** (Level 1) - Militia
- **COLONO** (Level 0) - Settler (default for new players)

### 2. Automatic Player Tracking
- System automatically detects when a player places a claim inside a realm
- Player is automatically added to the hierarchy with the default rank (COLONO)
- When a player removes all their claims from the realm, they are automatically removed from the hierarchy
- Uses the existing ClaimRegistry system for efficient player tracking

### 3. Pietro Block GUI - "Rivendicazioni" Tab Updates

#### Compacted Layout
- Title and total potatoes display compacted to save space
- Added divider line for clear section separation

#### Hierarchy Display Section
- Shows "Gerarchia del Regno:" (Realm Hierarchy) title
- Dynamic list of all players with claims in the realm
- Each entry shows:
  - Player name (truncated if too long)
  - Current hierarchy rank with color coding:
    - Gold for LORD
    - Purple for CONSIGLIERE
    - Blue for GUARDIA
    - Green for MILIZIANO
    - Gray for COLONO

#### Owner-Only Controls
- Only the realm owner sees "Cambia" (Change) buttons next to player names
- Clicking "Cambia" opens a rank selection menu
- Menu shows all available ranks (except LORD)
- Clicking a rank applies it to the selected player
- Changes are immediately synced to all nearby players

#### Scrolling Support
- List supports scrolling when more than 6 players are present
- Scroll indicators (▲▼) show when there's more content
- Mouse wheel scrolling enabled

### 4. Backend Architecture

#### New Files Created
1. **HierarchyRank.java** - Enum defining all hierarchy ranks with levels and display names
2. **UpdateHierarchyPacket.java** - Client→Server packet for rank changes
3. **HierarchySyncPacket.java** - Server→Client packet for hierarchy data synchronization

#### Modified Files
1. **PietroBlockEntity.java**
   - Added `playerHierarchy` map to store player ranks
   - Added `ownerUUID` field to track realm owner
   - `updatePlayerHierarchy()` - Automatically updates hierarchy when claims change
   - `getPlayersWithClaimsInRealm()` - Queries ClaimRegistry for players in the realm
   - `setPlayerHierarchy()` - Sets a player's rank and syncs to clients
   - `getAllPlayerHierarchies()` - Returns complete hierarchy data
   - Updated NBT serialization to persist hierarchy data

2. **ClaimBlockEntity.java**
   - `findAndLinkToRealm()` now calls `realm.updatePlayerHierarchy()` when claim is placed
   - `setRemoved()` now updates realm hierarchy when claim is removed

3. **PietroBlock.java**
   - `setPlacedBy()` updated to store both player name and UUID

4. **PietroMenu.java**
   - Constructor now sends HierarchySyncPacket to client when menu opens

5. **PietroScreen.java**
   - Complete redesign of `renderClaimsTab()` with hierarchy list
   - Added `mouseClicked()` handler for button interactions
   - Added `mouseScrolled()` handler for list scrolling
   - Added `renderRankSelectionMenu()` for the rank picker
   - Added `removed()` to clear hierarchy cache on close

6. **ClientPacketHandler.java**
   - Added `handleHierarchySync()` to process hierarchy data from server
   - Added cache methods for hierarchy data (get/clear)

7. **TharidiaThings.java**
   - Registered new packets (UpdateHierarchyPacket, HierarchySyncPacket)

### 5. Network Protocol
- **HierarchySyncPacket** (Server→Client): Sends complete hierarchy data when GUI opens or changes occur
- **UpdateHierarchyPacket** (Client→Server): Owner requests to change a player's rank
- Only realm owner can send UpdateHierarchyPacket (verified server-side)

### 6. Data Persistence
- Hierarchy data is saved in PietroBlockEntity NBT data
- Survives server restarts
- Automatically rebuilds from claims when necessary

## Usage

1. **As Realm Owner:**
   - Open the Pietro block GUI
   - Switch to "Rivendicazioni" tab
   - See all players who have claims in your realm
   - Click "Cambia" next to any player (except yourself)
   - Select their new rank from the menu
   - Changes apply immediately

2. **As Player in a Realm:**
   - Place a claim inside a realm
   - Automatically added to the realm hierarchy with COLONO rank
   - Remove all claims to be removed from hierarchy

## Technical Notes

- Uses existing ClaimRegistry for efficient player tracking (O(n) where n = number of claims)
- Thread-safe with proper server-side validation
- All hierarchy changes are logged for debugging
- Hierarchy data syncs automatically when players open the GUI
- System integrates seamlessly with existing claim management

## Future Enhancement Possibilities

- Rank-based permissions (e.g., only GUARDIA+ can modify certain things)
- Rank requirements for specific actions
- Custom rank names per realm
- Rank-based resource access
- Hierarchy commands for server admins
