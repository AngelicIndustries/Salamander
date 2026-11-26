# Salamander

![logo](images/logo_small.png)


Minecraft 1.7.10 support for [GeckoLib](https://github.com/bernie-g/geckolib) (3, 4, 5), model-related portions of [Citadel](https://github.com/AlexModGuy/Citadel), and OptiFine-format custom entity models (CEM).

Resource packs in OptiFine's and EMF's formats replace the models of vanilla, Et Futurum Requiem and other mods' entities, with their animations, armor models, and ETF's random and emissive textures. Fresh Animations, Just Expressions and Fresh Moves load from their original ZIPs. Other mods can extend pack models through an API like EMF's.

It also draws player skins in the modern format: 64x64 skins with slim arms, flat or 3D skin layers, see-through skin pixels (also under Angelica's shaders), and 3D hat layers on player heads. Skins come from [Wawel Auth](https://github.com/AngelicIndustries/WawelAuth) when it is installed, and from vanilla's skin download otherwise. With SimpleSkinBackport installed as well, Salamander draws the players and SimpleSkinBackport's own player drawing stays off.

Its expression editor prepares a skin's eyes and brows for Just Expressions and Fresh Moves, then exports the skin or uploads it through Wawel Auth.

<!--

[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/salamander)
-->

[![hub](images/badges/github.png)](https://github.com/AngelicIndustries/Salamander/releases)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/salamander)
[![67](images/badges/67.png)](https://67minecraft.com/mod/salamander)
[![maven](images/badges/maven.png)](https://maven.angelic.industries/#/releases/industries/angelic/salamander/Salamander)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)
![forge](images/badges/forge.png)

## Dependencies

* [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/gtnhlib) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/gtnhlib) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/GTNHLib/releases)
* [UniMixins](https://github.com/LegacyModdingMC/UniMixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)

## Documentation

* [Custom entity models (CEM)](https://github.com/AngelicIndustries/Salamander/wiki/Custom-Entity-Models): resource packs, supported targets, other mods' entities, emissive textures, configuration, and the mod API.
* [Player models](https://github.com/AngelicIndustries/Salamander/wiki/Player-Models): Just Expressions and Fresh Moves.
* [Expression editor](https://github.com/AngelicIndustries/Salamander/wiki/Expression-Editor): preparing a skin's eyes and brows for those packs.
* [Player skins](https://github.com/AngelicIndustries/Salamander/wiki/Player-Skins): skin layers, settings, and mod compatibility.
* [Random entity textures](https://github.com/AngelicIndustries/Salamander/wiki/Random-Entity-Textures): numbered variants and ETF/OptiFine rules.
* [Development](https://github.com/AngelicIndustries/Salamander/wiki/Development): building, regression tests, and debug models.

## Building

`./gradlew build`

## Credits

* The [GeckoLib](https://github.com/bernie-g/geckolib) contributors
* Skin layer implementation inspired by the [last MIT commit](https://github.com/tr7zw/3d-Skin-Layers/commit/1830e6ed7b86550afc2ed2695391a09ca70285e2) of [3D Skin Layers Mod](https://github.com/tr7zw/3d-Skin-Layers)
* Citadel compatibility code is derived from [Citadel](https://github.com/AlexModGuy/Citadel) and is licensed under LGPLv3
* [Entity Model Features](https://github.com/Traben-0/Entity_Model_Features) and [Entity Texture Features](https://github.com/Traben-0/Entity_Texture_Features), whose formats and behavior the CEM support follows
* [FreshLX / Ithan Mendoza](https://github.com/IthanMendoza/Fresh-Moves), for the Just Expressions / Fresh Moves skin layout used by the expression editor
* Debug fixtures use assets from GeckoLib Unofficial, Alex's Mobs, and Ice and Fire under their original licenses
* The [GTNewHorizons](https://github.com/GTNewHorizons) tooling

## License

`MIT`, except the Citadel-derived compatibility code and identified debug fixtures (`LGPLv3`)

## Buy me creatine

* [ko-fi.com](https://ko-fi.com/jackisasubtlejoke)
* Monero: `893tQ56jWt7czBsqAGPq8J5BDnYVCg2tvKpvwTcMY1LS79iDabopdxoUzNLEZtRTH4ewAcKLJ4DM4V41fvrJGHgeKArxwmJ`

<br>

![license](images/license_small.png)
