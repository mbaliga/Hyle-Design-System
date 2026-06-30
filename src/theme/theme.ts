import iwanthue from 'iwanthue';
import chroma from 'chroma-js';

/**
 * Hyle theming — the accent (primary) colour is ALWAYS user-selectable, and the
 * accompanying colours are generated with iwanthue so they are maximally,
 * perceptually distinct (from each other *and* from the accent), optionally
 * colour-blind-safe.
 *
 * The trick: Hyle's semantic tokens reference the *primitive*
 * `--color-palette-accent-*`, so overriding those three custom properties
 * re-themes action / focus / text-accent everywhere at once.
 */

export interface ThemeOptions {
  /** Number of distinct supporting colours to generate. */
  count?: number;
  /** Bias toward colour-blind-safe spacing. */
  colorBlind?: boolean;
  /** Deterministic palette for a given seed. */
  seed?: string;
}

export interface Theme {
  accent: string;
  accentBright: string;
  accentDeep: string;
  onAccent: string;
  /** Supporting categorical colours, distinct from the accent and each other. */
  support: string[];
}

const FIELD_NEAR = '#0a0809';
const INK = '#ece8e4';

/** Circular hue distance in degrees (0..180). */
function hueGap(a: number, b: number): number {
  if (Number.isNaN(a) || Number.isNaN(b)) return 180;
  let d = Math.abs(a - b) % 360;
  if (d > 180) d = 360 - d;
  return d;
}

/** Compute a full theme from a single user-chosen accent. */
export function createTheme(accent: string, opts: ThemeOptions = {}): Theme {
  const { count = 6, colorBlind = false, seed = 'hyle' } = opts;
  const c = chroma.valid(accent) ? chroma(accent) : chroma('#8e7bff');
  const accentHue = c.get('hcl.h');

  const support = iwanthue(count, {
    clustering: 'force-vector',
    quality: 60,
    seed,
    distance: colorBlind ? 'compromise' : 'euclidean',
    colorSpace: colorBlind ? 'colorblind' : 'default',
    // Keep supporting hues clear of the accent so it always stands out.
    colorFilter: (rgb) => hueGap(chroma(rgb[0], rgb[1], rgb[2]).get('hcl.h'), accentHue) > 26,
  });

  return {
    accent: c.hex(),
    accentBright: c.brighten(0.65).hex(),
    accentDeep: c.darken(0.85).hex(),
    onAccent: chroma.contrast(c, FIELD_NEAR) >= chroma.contrast(c, INK) ? FIELD_NEAR : INK,
    support,
  };
}

/** Apply a theme to a target element's CSS custom properties (default: :root). */
export function applyTheme(
  accent: string,
  opts: ThemeOptions = {},
  target: HTMLElement = document.documentElement
): Theme {
  const t = createTheme(accent, opts);
  const s = target.style;
  // Override the primitives — every semantic accent token references these.
  s.setProperty('--color-palette-accent-violet', t.accent);
  s.setProperty('--color-palette-accent-violet-bright', t.accentBright);
  s.setProperty('--color-palette-accent-violet-deep', t.accentDeep);
  s.setProperty('--color-action-on-primary', t.onAccent);
  // Supporting palette for categorical / multi-accent use.
  t.support.forEach((col, i) => s.setProperty(`--hy-support-${i + 1}`, col));
  s.setProperty('--hy-support-count', String(t.support.length));
  return t;
}

/** A single vivid, on-brand random accent (handy for "surprise me"). */
export function randomAccent(seed: string | number = 'accent'): string {
  return iwanthue(1, { colorSpace: 'intense', seed })[0];
}
