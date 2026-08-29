# LynHack

LynHack is a modern, modular utility client mod for **Minecraft 1.21.11** built on the **Fabric** toolchain (Java 21).

## Tech Stack
- **Minecraft Version:** 1.21.11
- **Mod Loader:** Fabric Loader (>=0.16.0)
- **Toolchain:** Gradle 8.14, Fabric Loom 1.9.2, Java 21
- **Mappings:** Yarn (1.21.11+build.6)
- **Fabric API:** 0.141.6+1.21.11
- **Bytecode Transformation:** SpongePowered Mixin / MixinExtras

## Building from Source
Run the Gradle build task:
```bash
./gradlew build
```
The compiled mod `.jar` will be available under `build/libs/`.
