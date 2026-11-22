# DigDown Client

Personal Minecraft Fabric client project.

## Target
- Minecraft Java Edition 1.21.11
- Fabric
- Java 21
- Default profile: PC

## Architecture
- Core
- Module System
- Click GUI
- HUD
- Configuration
- Commands
- Utilities

## Build
Build automation will be configured with GitHub Actions.


## Hunt / Sus Chunker (prototype)

Hunt is a Misc module with a right-click child setting named Sus Chunker. When enabled, the prototype checks the player's current loaded chunk about once per second and reports chunks containing at least 8 block entities. This is a simple heuristic and is not a faithful recreation of Leakestan V7's detector. It may produce false positives in storage/redstone-heavy areas.
