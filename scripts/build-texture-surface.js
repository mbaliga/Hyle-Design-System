// Assemble a chrome-less, full-bleed "texture surface" from the dynamic texture
// generator, extracted VERBATIM from public/generators/texture.html — the same
// render loop the generator ships, byte-for-byte.
//
// The generator itself is a full authoring app (sidebar + stage). For a living
// background — the shifting surface that sits beneath a frosted-glass pane — we
// lift only its pure render core (buildNoise + render, which read a plain state
// object and draw to a canvas) and drive it full-viewport with motion on. Zero
// reinterpretation of the drawing itself; only the chrome is dropped and a
// minimal resize/RAF driver is added (additive, like the colour picker bridge).
//
// Output: public/generators/texture-surface.html
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const srcPath = resolve(root, 'public/generators/texture.html');
const src = readFileSync(srcPath, 'utf8').split('\n');
const slice = (a, b) => src.slice(a - 1, b).join('\n'); // 1-indexed inclusive

// Verbatim region: `let noiseCache…`, buildNoise(), render(). Contiguous block.
const renderCore = slice(415, 514);

// Guard: fail loudly if the source ever shifts out from under these line numbers.
if (!/function render\(w,h,target,t\)/.test(renderCore) || !/function buildNoise/.test(renderCore)) {
  throw new Error(
    'build-texture-surface: expected buildNoise + render at lines 415-514 of ' +
      'public/generators/texture.html — the source moved. Re-check the slice.'
  );
}

const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Texture Surface</title>
<style>
  html,body{margin:0;height:100%;background:#0e0e12;overflow:hidden}
  canvas{display:block;width:100vw;height:100vh}
</style>
</head>
<body>
<canvas id="cv"></canvas>
<script>
/* ===== verbatim render core, extracted from texture.html (lines 415-514) ===== */
${renderCore}
/* ===== end verbatim core ===== */

/* Minimal driver (additive): a living, chrome-less surface. Motion defaults to
   'shimmer' so the grain drifts; the palette matches the generator's default. */
const cv = document.getElementById('cv');
const state = {
  fill: 'mesh', angle: 135,
  colors: ['#7b6cf6', '#3a2f8f', '#141428'],
  texType: 'grain', grain: 42, scale: 12, vig: 26,
  motion: 'shimmer', speed: 42,
  w: 0, h: 0,
};
const CAP = 1400; // render-resolution cap; CSS stretches to the viewport
function fit() {
  const vw = window.innerWidth, vh = window.innerHeight;
  const s = Math.min(1, CAP / Math.max(vw, vh));
  state.w = Math.max(2, Math.round(vw * s));
  state.h = Math.max(2, Math.round(vh * s));
}
fit();
window.addEventListener('resize', fit);
const t0 = performance.now();
(function loop() {
  const t = (performance.now() - t0) / 1000;
  render(state.w, state.h, cv, t);
  requestAnimationFrame(loop);
})();
</script>
</body>
</html>
`;

writeFileSync(resolve(root, 'public/generators/texture-surface.html'), html);
console.log('build-texture-surface: wrote public/generators/texture-surface.html');
