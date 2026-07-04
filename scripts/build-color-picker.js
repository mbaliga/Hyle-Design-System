// Assemble the self-contained 3D colour picker, extracted VERBATIM from
// kit/tactile-kit.html — the same THREE.js picker the kit ships, byte-for-byte.
//
// It is emitted as a standalone document so the picker's own code (which uses
// `document.getElementById`) runs unchanged; hy-color-picker mounts it in an
// iframe and bridges the chosen colour out via postMessage. This is the most
// faithful possible reproduction: zero reinterpretation of the picker itself.
//
// Output: public/kit/color-picker.html
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const src = readFileSync(resolve(root, 'kit/tactile-kit.html'), 'utf8').split('\n');
const slice = (a, b) => src.slice(a - 1, b).join('\n'); // 1-indexed inclusive

// Verbatim regions (see build notes): picker CSS, markup, THREE.js lib, picker IIFE.
const pickerCss = slice(450, 557);
const markup = slice(644, 677);
const threeLib = slice(1169, 1169);
let pickerJs = slice(1172, 1895); // the IIFE, without its wrapping <script> tags
pickerJs = pickerJs.replace(/^<script>/, '');

// Violet-default kit tokens the picker reads (:root --acc-rgb → the ring/model
// accent), matching the extracted KIT_CSS. Everything else the picker needs it
// defines itself in its #hyle-picker block.
const rootTokens = `
  :root{
    --bg:#000;--srf:#16161a;--srf2:#212128;--srf3:#2c2c34;
    --groove:#050506;--edge:#3a3a44;
    --rim:rgba(255,255,255,.16);--rim2:rgba(255,255,255,.09);--lip:rgba(255,255,255,.12);
    --drk:rgba(0,0,0,.88);--drk2:rgba(0,0,0,.55);
    --ink:rgba(255,255,255,.58);--ink2:rgba(255,255,255,.3);--ink3:rgba(255,255,255,.15);
    --acc:#8e7bff;--acc-hi:#b0a4ff;--acc-lo:#5e4fdd;--acc-rgb:142,123,255;
    --ease:cubic-bezier(.4,0,.2,1);--t:.2s;
  }
  html,body{margin:0;background:transparent}
  body{display:flex;align-items:center;justify-content:center;min-height:100vh;padding:8px}
  #hyle-picker{background:transparent!important;box-shadow:none!important;padding:0!important}
`;

// A minimal bridge: emit the current hex to the host whenever it changes, and
// accept an initial value via ?value=. Additive only — the picker is untouched.
const bridge = `
<script>
(function(){
  var last='',lastH=0;
  function cur(){var h=document.getElementById('hex');return h?h.value.trim():'';}
  function post(t,d){try{parent.postMessage(Object.assign({type:t},d),'*');}catch(e){}}
  function tick(){
    var v=cur();if(v&&v!==last){last=v;post('hy-color',{value:v});}
    var h=Math.ceil((document.getElementById('app')||document.body).getBoundingClientRect().height);
    if(h&&h!==lastH){lastH=h;post('hy-height',{value:h});}
    requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
  try{
    var p=new URLSearchParams(location.search),iv=p.get('value');
    if(iv){var hex=document.getElementById('hex');if(hex){hex.value=iv;hex.dispatchEvent(new Event('input',{bubbles:true}));}}
  }catch(e){}
})();
<\/script>`;

const out = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>Hyle — Colour Picker</title>
<style>
${rootTokens}
/* ===== picker skin (verbatim from tactile-kit.html lines 450-557) ===== */
${pickerCss}
</style>
</head>
<body>
${markup}
<script>
${threeLib}
<\/script>
<script>
${pickerJs}
<\/script>
${bridge}
</body>
</html>
`;

const dest = resolve(root, 'public/kit/color-picker.html');
mkdirSync(dirname(dest), { recursive: true });
writeFileSync(dest, out, 'utf8');
console.log('wrote', dest, '(' + out.length + ' bytes)');
