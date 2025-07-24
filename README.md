# UltraKOTHPlusPlus

An advanced King of the Hill (KOTH) plugin for Minecraft Spigot/Paper servers with extensive features including WorldGuard integration, PlaceholderAPI support, boss bars, rewards, and much more.

## Features

### Core Features
- **Multiple KOTH Regions**: Configure multiple KOTH locations with different settings
- **Automatic Scheduling**: Set up automatic KOTH events at regular intervals  
- **Boss Bar Integration**: Real-time progress display with customizable boss bars
- **Reward System**: Configurable rewards for KOTH winners
- **Player Statistics**: Track wins and statistics for all players
- **Sound & Particle Effects**: Immersive audio and visual feedback

### Integrations
- **WorldGuard Support**: Use WorldGuard regions for precise area control
- **PlaceholderAPI Support**: Extensive placeholders for other plugins
- **Vault Support**: Economy rewards integration

### Advanced Features
- **Tab Completion**: Full tab completion for all commands
- **Permission System**: Granular permission control
- **Configurable Capture Time**: Set custom capture durations per event
- **Debug Logging**: Comprehensive logging for troubleshooting
- **Data Persistence**: Player statistics saved automatically

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/UltraKOTHPlusPlus/config.yml`
5. Set up KOTH regions in the configuration
6. Reload the plugin or restart your server

## Configuration

### Basic Configuration

```yaml
koth:
  capture-time-seconds: 300  # 5 minutes to capture
  
  bossbar:
    enabled: true
    title: "&eKOTH being captured by &a{player}"
    color: "BLUE"
    style: "SOLID"
    countdown: true
  
  broadcast:
    start: "&6KOTH has started at &b{location}!"
    capture: "&a{player} &eis capturing the KOTH!"
    win: "&b{player} &ahas won the KOTH at &e{location}!"
  
  rewards:
    - "give {player} diamond 5"
    - "eco give {player} 1000"
  
  schedule:
    enabled: true
    interval-minutes: 60

koth-locations:
  example:
    world: "world"
    x: 100
    y: 65
    z: 100
    radius: 10
    region: "koth_example"  # Optional WorldGuard region
```

### Setting Up KOTH Regions

Add new KOTH locations in the `koth-locations` section:

```yaml
koth-locations:
  desert:
    world: "world"
    x: -200
    y: 70
    z: 300
    radius: 15
    region: "koth_desert"  # Optional WorldGuard region name
  
  nether:
    world: "world_nether"
    x: 0
    y: 64
    z: 0
    radius: 12
```

## Commands

### Player Commands
- `/koth help` - Show help message
- `/koth status` - Show current KOTH status
- `/koth list` - List all KOTH regions  
- `/koth wins [player]` - Show KOTH wins for yourself or another player

### Admin Commands
- `/koth start [region]` - Start a KOTH event (random if no region specified)
- `/koth stop` - Stop the current KOTH event
- `/koth reload` - Reload plugin configuration

## Permissions

### Player Permissions
- `ultrakoth.player` - Basic player permissions (default: true)
- `ultrakoth.player.participate` - Allow participating in KOTH events
- `ultrakoth.player.stats` - Allow viewing KOTH statistics

### Admin Permissions  
- `ultrakoth.admin` - All admin permissions (default: op)
- `ultrakoth.admin.start` - Start KOTH events
- `ultrakoth.admin.stop` - Stop KOTH events
- `ultrakoth.admin.reload` - Reload configuration
- `ultrakoth.*` - All permissions

## PlaceholderAPI Placeholders

Use these placeholders in other plugins:

### Player Placeholders
- `%ultrakoth_wins%` - Player's total KOTH wins
- `%ultrakoth_last_win%` - Timestamp of player's last win
- `%ultrakoth_is_capturing%` - True if player is currently capturing
- `%ultrakoth_rank%` - Player's rank by wins

### Global Placeholders
- `%ultrakoth_active%` - Current active KOTH name
- `%ultrakoth_is_active%` - True if any KOTH is active
- `%ultrakoth_capturing_player%` - Name of player currently capturing
- `%ultrakoth_progress%` - Current capture progress (seconds)
- `%ultrakoth_progress_percentage%` - Capture progress as percentage
- `%ultrakoth_time_left%` - Remaining capture time
- `%ultrakoth_total_time%` - Total capture time required
- `%ultrakoth_total_players%` - Total players with recorded wins
- `%ultrakoth_total_wins%` - Total wins across all players

### Top Player Placeholders
- `%ultrakoth_top_1_name%` - Name of #1 player
- `%ultrakoth_top_1_wins%` - Wins of #1 player
- `%ultrakoth_top_2_name%` - Name of #2 player
- `%ultrakoth_top_2_wins%` - Wins of #2 player
- And so on...

## How It Works

1. **KOTH Start**: A KOTH event begins at a configured location
2. **Player Entry**: Players enter the KOTH region (defined by radius or WorldGuard region)
3. **Capture Process**: Player must stay in the region for the configured capture time
4. **Progress Tracking**: Boss bar shows capture progress and current capturing player
5. **Interruption**: If player leaves region, capture resets
6. **Victory**: After full capture time, player wins and receives rewards
7. **Statistics**: Win is recorded and saved to player data

## WorldGuard Integration

If WorldGuard is installed, you can use WorldGuard regions instead of radius-based areas:

1. Create a WorldGuard region: `/rg define koth_example`
2. Set the region name in config: `region: "koth_example"`
3. The plugin will use WorldGuard's precise region boundaries

## Troubleshooting

### Common Issues

**Plugin not responding:**
- Check server logs for error messages
- Ensure all dependencies are installed
- Verify configuration syntax
- Enable debug mode in config

**KOTH not starting:**
- Check if world exists
- Verify coordinates are valid
- Ensure regions are properly configured
- Check permissions

**Boss bar not showing:**
- Verify boss bar is enabled in config
- Check if players have required permissions
- Ensure capture time is greater than 0

### Debug Mode

Enable debug logging in `config.yml`:

```yaml
settings:
  debug: true
```

This will provide detailed information about plugin operations in the server console.

## Support

For support, bug reports, or feature requests:
- Visit: https://sanscraft.top
- GitHub: Create an issue on the repository

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

- **Author**: SansNom
- **Organization**: sanscraft.top
- **Version**: 1.0.0

Built with love for the Minecraft community! 🎮