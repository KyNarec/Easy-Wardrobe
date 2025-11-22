# Easy Wardrobe

A Fabric mod that adds a Hypixel Skyblock-inspired Wardrobe to Minecraft. Manage up to 9 armor sets easily with a GUI!

## Features

*   **9 Armor Sets**: Store up to 9 full sets of armor.
*   **Easy Equipping**: Click the "Equip" button to instantly swap your current armor with a stored set.
*   **Ghost Items**: Equipped sets remain visible in the wardrobe as "Ghost Items" (locked and marked in red), so you can see what you have equipped.
*   **Visual Clarity**: Empty slots are filled with colored glass panes (unique color per column) for easy organization.
*   **Server-Side Compatible**: Works on dedicated servers! Players can join with a vanilla client and still use the feature (though installing it on the client is recommended for the best experience).

## Commands

*   `/wardrobe` or `/w`: Opens the Wardrobe GUI.

## Installation

1.  Install [Fabric Loader](https://fabricmc.net/).
2.  Download the `easy-wardrobe-x.x.x.jar`.
3.  Place the jar in your `mods` folder.
4.  (Optional) Install [Fabric API](https://curseforge.com/minecraft/mc-mods/fabric-api) if not already present.

## How to Build (Generate JAR)

To generate the mod JAR file yourself (which works for both Client and Server), follow these steps:

### Prerequisites
*   JDK 21 (Java Development Kit) installed.

### Steps
1.  Open a terminal in the project directory.
2.  Run the build command:
    *   **Windows**: `gradlew build`
    *   **Linux/Mac**: `./gradlew build`
3.  Once the build completes successfully, you will find the JAR file in:
    *   `build/libs/easy-wardrobe-0.0.1.jar` (or similar version)

**Note**: You might see a `-sources.jar` or `-dev.jar` as well. You want the main one (usually just `easy-wardrobe-VERSION.jar`).

## Data Storage
Wardrobe data is stored in `config/easy-wardrobe/players/<UUID>.dat` using NBT format. This ensures your items are saved securely with all their data (enchantments, durability, etc.).
