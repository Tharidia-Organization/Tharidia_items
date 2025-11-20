# Lobby Commands Reference

## Overview
All lobby commands are now unified under a single command structure with proper permission handling.
**All commands now work correctly in both game chat and server console.**

## Configuration Required

### Lobby Server
Edit `config/tharidiathings-common.toml`:
```toml
isLobbyServer = true
```

### Main Server
Edit `config/tharidiathings-common.toml`:
```toml
isLobbyServer = false
```

## Available Commands

### For All Players (No OP Required)

#### `/thqueue`
- **Description**: Check your position in the queue
- **Usage**: `/thqueue`
- **Output**: Shows your queue position, total queue size, and wait time
- **Available to**: All players

---

### For Admins Only (Requires OP Level 2)

All admin commands are under `/thqueueadmin` and require OP level 2.
**✅ All commands now display feedback in game chat when executed by players.**

#### `/thqueueadmin info`
- **Description**: Display complete queue system information
- **Usage**: `/thqueueadmin info`
- **Shows**:
  - Lobby mode status
  - Queue system status
  - Auto-transfer status
  - Max players setting
  - Current queue size
- **Works in**: Game chat and server console

#### `/thqueueadmin enable`
- **Description**: Enable the queue system
- **Usage**: `/thqueueadmin enable`
- **Works in**: Game chat and server console

#### `/thqueueadmin disable`
- **Description**: Disable the queue system
- **Usage**: `/thqueueadmin disable`
- **Works in**: Game chat and server console

#### `/thqueueadmin clear`
- **Description**: Clear all players from the queue
- **Usage**: `/thqueueadmin clear`
- **Works in**: Game chat and server console

#### `/thqueueadmin list`
- **Description**: List all players currently in queue with wait times
- **Usage**: `/thqueueadmin list`
- **Works in**: Game chat and server console

#### `/thqueueadmin send <player>`
- **Description**: Send a specific player to the main server immediately
- **Usage**: `/thqueueadmin send PlayerName`
- **Example**: `/thqueueadmin send Frenk012`
- **Works in**: Game chat and server console

#### `/thqueueadmin sendnext`
- **Description**: Send the next player in queue to the main server
- **Usage**: `/thqueueadmin sendnext`
- **Works in**: Game chat and server console

#### `/thqueueadmin sendall`
- **Description**: Send all queued players to the main server
- **Usage**: `/thqueueadmin sendall`
- **Works in**: Game chat and server console

#### `/thqueueadmin autotransfer <on|off>`
- **Description**: Enable or disable automatic transfer when queue is small
- **Usage**: `/thqueueadmin autotransfer on` or `/thqueueadmin autotransfer off`
- **Works in**: Game chat and server console

#### `/thqueueadmin maxplayers <number>`
- **Description**: Set the maximum number of players allowed on the main server
- **Usage**: `/thqueueadmin maxplayers 100`
- **Range**: 1-1000
- **Works in**: Game chat and server console

#### `/thqueueadmin lobbymode <on|off>`
- **Description**: Enable or disable lobby mode (spectator spawn, name prompt skip)
- **Usage**: `/thqueueadmin lobbymode on` or `/thqueueadmin lobbymode off`
- **Effects**:
  - When ON: Players spawn in spectator mode, no name prompt
  - When OFF: Normal spawn, name prompt enabled
- **Works in**: Game chat and server console

#### `/thqueueadmin play`
- **Description**: Admin command to manually join/queue (bypasses restrictions)
- **Usage**: `/thqueueadmin play`
- **Works in**: Game chat only (requires player entity)

---

## Key Changes from Previous Version

1. **Unified Commands**: No more duplicate commands (`queue` vs `thqueue`, `queueadmin` vs `thqueueadmin`)
   - Only `thqueue` and `thqueueadmin` exist now

2. **Consistent Behavior**: All admin commands now work in both game chat and server console
   - Changed broadcast flag from `true` to `false` to ensure messages appear in chat

3. **Proper Permissions**: 
   - Non-OP players can ONLY use `/thqueue`
   - All other commands require OP level 2

4. **Server-Specific Registration**:
   - Commands only register on servers with `isLobbyServer = true`
   - Main server won't have these commands at all

## Testing Checklist

### On Lobby Server
- [ ] Set `isLobbyServer = true` in config
- [ ] Restart server with new JAR
- [ ] Verify logs show: "Registering lobby commands (isLobbyServer=true)"
- [ ] As non-OP player: `/thqueue` works, `/thqueueadmin` shows "no permission"
- [ ] As OP: All `/thqueueadmin` commands work in game chat
- [ ] From server console: All `/thqueueadmin` commands work

### On Main Server
- [ ] Set `isLobbyServer = false` in config
- [ ] Restart server with new JAR
- [ ] Verify logs show: "Not registering lobby commands on non-lobby server"
- [ ] Verify `/thqueue` and `/thqueueadmin` don't exist

## Troubleshooting

### Commands not working in game chat
- Ensure you're OP level 2 or higher: `/op YourUsername`
- Check server logs for permission errors

### Commands not registered
- Verify `isLobbyServer = true` in `config/tharidiathings-common.toml`
- Check server startup logs for "Registering lobby commands"
- Ensure the new JAR is deployed and server restarted

### "Unknown command" error
- On main server: This is expected if `isLobbyServer = false`
- On lobby server: Check config and restart

## Example Workflow

1. **Initial Setup** (lobby server):
   ```
   /thqueueadmin lobbymode on
   /thqueueadmin enable
   /thqueueadmin autotransfer on
   /thqueueadmin maxplayers 50
   ```

2. **Monitor Queue**:
   ```
   /thqueueadmin info
   /thqueueadmin list
   ```

3. **Manual Control**:
   ```
   /thqueueadmin send PlayerName
   /thqueueadmin sendnext
   /thqueueadmin sendall
   ```
