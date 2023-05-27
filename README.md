# ModMixinLoader <a href="https://www.curseforge.com/minecraft/mc-mods/modmixinloader"><img src="http://cf.way2muchnoise.eu/full_866591_downloads.svg"></a>

A mod that makes it easier to mixin into other mods.

## How to Use
### Users
Download the mod and put it inside your mods folder.

### Developers
1. Setup a basic forge-mod project with mixins. Click [<ins>here</ins>](https://github.com/focamacho/ModMixinLoader/wiki/_Example:-build.gradle-with-Mixins) for a gradle buildscript example.
2. Create a new folder inside your `resources` folder called `modmixins`. **(/resources/modmixins/)**
3. Put your mixins json files inside that folder.
4. Add to your mixin json a new key-value named `mods` that accepts an array of mod-ids that it will require to work. Click [<ins>here</ins>](https://github.com/focamacho/ModMixinLoader/wiki/_Example:-mixin-with-mod-dependencies) for a mod mixin example.
5. Done!