import { svg, type SVGTemplateResult } from 'lit';

/**
 * Hyle icons — sharp-edged and blocky (miter joins, flat caps, no rounding),
 * on a 24-unit grid. Filled glyphs use fill="currentColor"; line glyphs inherit
 * the <svg>'s stroke. Brutalist and built for legibility at small sizes.
 */
export const ICONS: Record<string, SVGTemplateResult> = {
  play: svg`<path fill="currentColor" stroke="none" d="M6 4 L20 12 L6 20 Z"/>`,
  pause: svg`<path fill="currentColor" stroke="none" d="M6 4h4v16H6z M14 4h4v16h-4z"/>`,
  stop: svg`<path fill="currentColor" stroke="none" d="M5 5h14v14H5z"/>`,
  next: svg`<path fill="currentColor" stroke="none" d="M5 4 L15 12 L5 20 Z M17 4h3v16h-3z"/>`,
  prev: svg`<path fill="currentColor" stroke="none" d="M19 4 L9 12 L19 20 Z M4 4h3v16H4z"/>`,
  record: svg`<path fill="currentColor" stroke="none" d="M12 5 L19 12 L12 19 L5 12 Z"/>`,
  power: svg`<path d="M12 3 V12"/><path d="M6.5 6.5 A8 8 0 1 0 17.5 6.5"/>`,
  check: svg`<path d="M4 12 L10 18 L20 6"/>`,
  close: svg`<path d="M6 6 L18 18 M18 6 L6 18"/>`,
  plus: svg`<path d="M12 4 V20 M4 12 H20"/>`,
  minus: svg`<path d="M4 12 H20"/>`,
  chevronDown: svg`<path d="M4 8 L12 16 L20 8"/>`,
  chevronRight: svg`<path d="M8 4 L16 12 L8 20"/>`,
  arrowRight: svg`<path d="M3 12 H20 M14 6 L20 12 L14 18"/>`,
  menu: svg`<path d="M4 6 H20 M4 12 H20 M4 18 H20"/>`,
  grid: svg`<path d="M4 4h7v7H4z M13 4h7v7h-7z M4 13h7v7H4z M13 13h7v7h-7z"/>`,
  layers: svg`<path d="M12 3 L21 8 L12 13 L3 8 Z"/><path d="M3 13 L12 18 L21 13"/>`,
  equalizer: svg`<path fill="currentColor" stroke="none" d="M4 13h3v7H4z M10 7h3v13h-3z M16 3h3v17h-3z"/>`,
  bolt: svg`<path fill="currentColor" stroke="none" d="M13 2 L4 14 H11 L10 22 L20 9 H13 Z"/>`,
  folder: svg`<path d="M3 6h6l2 2h10v12H3z"/>`,
  lock: svg`<path d="M6 11h12v9H6z"/><path d="M9 11 V8 a3 3 0 0 1 6 0 v3"/>`,
  search: svg`<path d="M4 4h10v10H4z"/><path d="M14 14 L20 20"/>`,
  settings: svg`<path d="M4 7h9 M17 7h3 M4 12h3 M11 12h9 M4 17h7 M15 17h5"/><path fill="currentColor" stroke="none" d="M13 5h2v4h-2z M5 10h2v4H5z M11 15h2v4h-2z"/>`,
  copy: svg`<path d="M9 9h11v11H9z"/><path d="M4 4h11v2 M4 4v11h2"/>`,
  sun: svg`<path fill="currentColor" stroke="none" d="M9 9h6v6H9z"/><path d="M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M19 5l-2 2 M7 17l-2 2"/>`,
  moon: svg`<path fill="currentColor" stroke="none" d="M20 14 A9 9 0 1 1 10 4 A7 7 0 0 0 20 14 Z"/>`,
};

export type HyIconName = keyof typeof ICONS;
export const ICON_NAMES = Object.keys(ICONS) as HyIconName[];
