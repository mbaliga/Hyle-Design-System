# Hyle Worlds — Brutalist live wallpaper

`:wallpaper` runs the design system's procedural worlds — the **Form-World** SDF
raymarcher — as an Android live wallpaper: monumental forms in coloured haze,
calm by default, themed from the shared Hyle tokens.

## How it works

The Form-World engine is a GLSL **ES 1.00** fragment shader, which is exactly
what Android OpenGL **ES 2.0** runs — so the shader is reused verbatim:

```
field/form-world.html  ──(extracted)──►  res/raw/world_frag.glsl + world_vert.glsl
                                          (the same raymarcher the web Field renders)
```

- `HyleWorldsWallpaperService` — a `WallpaperService` whose `Engine` owns a
  render thread with its own EGL/GLES2 context (`EglCore`). Rendering **pauses
  whenever the wallpaper isn't visible** (calm + battery), and home-screen paging
  becomes a gentle parallax yaw.
- `WorldRenderer` — compiles the shader and feeds it the same uniforms the web
  `draw()` loop does. Sprites are disabled, so the marcher never touches the
  dynamic uniform-array path — keeping it portable and light.
- `WorldConfig` / `WallpaperSettingsActivity` — scene (Bowl · Helix · Towers ·
  Arch · Ruins · Planet), palette, motion (Passive/Active), recursion and speed,
  persisted in `SharedPreferences`. Palettes are stone / smog / light triads,
  including a token-derived **Hyle** preset (`HyleTokens` material + violet
  accent) and the README's Blade Runner / Gris recipes.

The settings screen is Compose, themed from `dev.aarso.hyle.tokens.HyleTokens`.

## Build & install

```bash
./gradlew :wallpaper:assembleDebug
./gradlew :wallpaper:installDebug    # to a connected device / emulator
```
Then open **Hyle Worlds** (it sets itself up via the live-wallpaper picker), or
launch the settings app and tap **Set as live wallpaper**. Requires GLES2
(declared `required` in the manifest), JDK 17, Android SDK 36.

> The shader lives in `field/` as the source of truth; if it changes there,
> re-extract the two `res/raw/*.glsl` files.
