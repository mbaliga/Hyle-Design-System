// Checks directive I-3 against the design tokens: every meaning-colour role also has a
// shape and a word, and the red token is used for nothing but the error role.
// Run: node scripts/check-meaning-colours.js   (exit 1 on any failure). Needs no install.
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const roles = JSON.parse(readFileSync(join(root, 'scripts/meaning-roles.json'), 'utf8'));

// Flatten tokens/*.json into { 'a.b.c': token }.
const tokens = {};
const walk = (node, path, file) => {
  if (node && typeof node === 'object') {
    if ('$value' in node) {
      tokens[path.join('.')] = { ...node, file };
      return;
    }
    for (const [k, v] of Object.entries(node)) if (!k.startsWith('$')) walk(v, [...path, k], file);
  }
};
for (const f of readdirSync(join(root, 'tokens')).filter((n) => n.endsWith('.json'))) {
  walk(JSON.parse(readFileSync(join(root, 'tokens', f), 'utf8')), [], f);
}

const failures = [];
const fail = (m) => failures.push(m);
const ref = (v) => (typeof v === 'string' && /^\{(.+)\}$/.exec(v)?.[1]) || null;
const resolve = (name, seen = new Set()) => {
  const t = tokens[name];
  if (!t) return null;
  const r = ref(t.$value);
  if (!r) return t.$value;
  if (seen.has(r)) return null;
  seen.add(r);
  return resolve(r, seen);
};
const parse = (c) => {
  let m = /^#([0-9a-f]{6})([0-9a-f]{2})?$/i.exec(c);
  if (m) return [0, 2, 4].map((i) => parseInt(m[1].slice(i, i + 2), 16));
  m = /^rgba?\(([^)]+)\)/.exec(c);
  return m ? m[1].split(',').slice(0, 3).map((x) => parseFloat(x)) : null;
};
const hsv = ([r, g, b]) => {
  r /= 255; g /= 255; b /= 255;
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn;
  let h = 0;
  if (d) h = mx === r ? ((g - b) / d + 6) % 6 : mx === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return { h: h * 60, s: mx ? d / mx : 0, v: mx };
};
const isRed = (c) => {
  const rgb = parse(c);
  if (!rgb) return false;
  const { h, s, v } = hsv(rgb);
  return (h >= 345 || h <= 15) && s >= 0.5 && v >= 0.4;
};

// 1. Roles: tokens exist, are colours, and each role has a non-empty shape and word.
const shapes = new Map();
const words = new Map();
const roleOf = new Map(); // token -> role
for (const [role, def] of Object.entries(roles.roles)) {
  if (!def.shape || !String(def.shape).trim()) fail(`role "${role}" has no shape`);
  if (!def.word || !String(def.word).trim()) fail(`role "${role}" has no word`);
  if (shapes.has(def.shape)) fail(`roles "${shapes.get(def.shape)}" and "${role}" share the shape "${def.shape}"; shape cannot tell them apart`);
  if (words.has(def.word)) fail(`roles "${words.get(def.word)}" and "${role}" share the word "${def.word}"`);
  shapes.set(def.shape, role);
  words.set(def.word, role);
  if (!def.tokens?.length) fail(`role "${role}" lists no tokens`);
  for (const name of def.tokens ?? []) {
    const t = tokens[name];
    if (!t) fail(`role "${role}": token ${name} does not exist`);
    else if (t.$type !== 'color') fail(`role "${role}": token ${name} is not a colour`);
    if (roleOf.has(name) && roleOf.get(name) !== role) fail(`token ${name} is claimed by roles "${roleOf.get(name)}" and "${role}"`);
    roleOf.set(name, role);
  }
}

// 2. Every role-bearing semantic token is covered. Palette colours that the docs say
//    MUST pair with a non-colour channel (provenance, signal) and the feedback and
//    provenance aliases must belong to a role.
const mustHaveRole = Object.keys(tokens).filter(
  (n) => /^color\.(palette\.(provenance|signal)|feedback|provenance)\./.test(n),
);
for (const n of mustHaveRole) if (!roleOf.has(n)) fail(`meaning-colour token ${n} has no role, shape or word in scripts/meaning-roles.json`);

// 3. Red: the red token is the error role's and nothing else's.
const redName = roles.red.token;
const redValue = resolve(redName)?.toLowerCase();
if (!redValue) fail(`red token ${redName} not found`);
if (roleOf.get(redName) !== roles.red.role) fail(`red token ${redName} is not in role "${roles.red.role}"`);
const exceptions = roles.red.nonMeaningRedExceptions ?? {};
for (const [name, t] of Object.entries(tokens)) {
  if (t.$type !== 'color' || name === redName) continue;
  const r = ref(t.$value);
  if (r) {
    // Aliases of red are fine only inside the error role.
    if (resolve(name)?.toLowerCase() === redValue && roleOf.get(name) !== roles.red.role) {
      fail(`${name} aliases the red token but is not in role "${roles.red.role}"`);
    }
    continue;
  }
  if (t.$value.toLowerCase() === redValue) fail(`${name} duplicates the red value ${redValue}`);
  else if (isRed(t.$value) && !(name in exceptions)) fail(`${name} (${t.$value}) is red-hued and not the red token; add it to a documented exception or change it`);
}
for (const name of Object.keys(exceptions)) {
  if (roleOf.has(name)) fail(`red exception ${name} is also a meaning-colour role token`);
  if (!tokens[name]) fail(`red exception ${name} does not exist`);
}
// Other roles must not use a red-hued colour.
for (const [name, role] of roleOf) {
  const v = resolve(name);
  if (role !== roles.red.role && v && isRed(v)) fail(`token ${name} in role "${role}" is red-hued`);
}

// Report.
const nRoles = Object.keys(roles.roles).length;
console.log(`Checked ${nRoles} meaning-colour roles, ${roleOf.size} tokens, red token ${redName} (${redValue}).`);
for (const [role, d] of Object.entries(roles.roles)) console.log(`  ${role.padEnd(10)} shape=${d.shape.padEnd(9)} word=${d.word}`);
for (const [n, why] of Object.entries(exceptions)) console.log(`  red exception (not a meaning colour): ${n}: ${why}`);
if (failures.length) {
  console.error(`\nFAIL (${failures.length}):`);
  for (const f of failures) console.error(`  - ${f}`);
  process.exit(1);
}
console.log('OK');
