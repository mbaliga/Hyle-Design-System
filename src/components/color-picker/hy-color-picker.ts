import { LitElement, css, html } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import chroma from 'chroma-js';
import { createTheme } from '../../theme/theme.js';

export type HyColorSpace = 'hsl' | 'hsv' | 'oklch';

interface SpaceDef {
  label: string;
  /** hex → { h: hue 0..360, x: 0..1, y: 0..1 } */
  toChannels(hex: string): { h: number; x: number; y: number };
  /** channels → hex */
  fromChannels(h: number, x: number, y: number): string;
  /** channels → [r,g,b] 0..255 directly (slice hot path, no hex round-trip) */
  toRgb(h: number, x: number, y: number): number[];
  xLabel: string;
  yLabel: string;
}

// Max sRGB OKLCH chroma is ~0.32; cap the slice's C axis near there so picks
// stay in gamut and the crosshair doesn't snap away from the pointer.
const OKLCH_CMAX = 0.33;

const SPACES: Record<HyColorSpace, SpaceDef> = {
  hsl: {
    label: 'HSL',
    xLabel: 'S',
    yLabel: 'L',
    toChannels(hex) {
      const [h, s, l] = chroma(hex).hsl();
      return { h: Number.isNaN(h) ? 0 : h, x: s, y: l };
    },
    fromChannels: (h, x, y) => chroma.hsl(h, x, y).hex(),
    toRgb: (h, x, y) => chroma.hsl(h, x, y).rgb(),
  },
  hsv: {
    label: 'HSV',
    xLabel: 'S',
    yLabel: 'V',
    toChannels(hex) {
      const [h, s, v] = chroma(hex).hsv();
      return { h: Number.isNaN(h) ? 0 : h, x: s, y: v };
    },
    fromChannels: (h, x, y) => chroma.hsv(h, x, y).hex(),
    toRgb: (h, x, y) => chroma.hsv(h, x, y).rgb(),
  },
  oklch: {
    label: 'OKLCH',
    xLabel: 'C',
    yLabel: 'L',
    toChannels(hex) {
      const [l, c, h] = chroma(hex).oklch();
      return { h: Number.isNaN(h) ? 0 : h, x: Math.min(1, (c || 0) / OKLCH_CMAX), y: l };
    },
    fromChannels: (h, x, y) => chroma.oklch(y, x * OKLCH_CMAX, h).hex(),
    toRgb: (h, x, y) => chroma.oklch(y, x * OKLCH_CMAX, h).rgb(),
  },
};

/**
 * A 3D colour-solid picker — a rotatable RGB cube for spatial context, a hue
 * ring, a 2D slice for the remaining channels, colour-space switching, a hex
 * readout, and an iwanthue supporting palette (colour-blind-safe optional).
 *
 * Reimagined from the Tactile Kit's colour section, in Hyle tokens, powered by
 * chroma-js. Drives the theming engine when used as an accent picker.
 *
 * @element hy-color-picker
 * @fires hy-input  - `detail.hex` while dragging.
 * @fires hy-change - `detail.hex` on release / commit.
 * @attr value - The current colour (hex).
 * @attr space - hsl | hsv | oklch.
 */
@customElement('hy-color-picker')
export class HyColorPicker extends LitElement {
  @property() value = '#8e7bff';
  @property({ reflect: true }) space: HyColorSpace = 'hsl';
  @property({ type: Number, attribute: 'palette-count' }) paletteCount = 6;
  @property({ type: Boolean, attribute: 'color-blind' }) colorBlind = false;

  @state() private _seed = 'hyle';
  @state() private _support: string[] = [];

  @query('#slice') private _sliceCv!: HTMLCanvasElement;
  @query('#model') private _modelCv!: HTMLCanvasElement;

  private _raf = 0;
  private _yaw = 0.6;
  private _pitch = -0.5;
  private _lastSliceKey = '';
  private _cube: { p: [number, number, number]; c: [number, number, number] }[] = [];
  private _reduced = false;
  private _onscreen = true;
  private _io?: IntersectionObserver;
  private _mq?: MediaQueryList;
  private _drag: 'ring' | 'slice' | 'model' | null = null;

  static styles = css`
    :host {
      display: inline-block;
      font-family: var(--font-family-sans);
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
    }
    .app {
      width: 320px;
      max-width: 100%;
      background: var(--control-surface, #16161a);
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      border-radius: var(--radius-lg, 12px);
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .spaces {
      display: flex;
      gap: 3px;
      background: var(--control-groove, #050506);
      border-radius: var(--radius-md, 8px);
      padding: 3px;
    }
    .seg {
      flex: 1;
      height: 30px;
      border: 0;
      background: transparent;
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
      font: 600 12px var(--font-family-sans);
      border-radius: 6px;
      cursor: pointer;
    }
    .seg[aria-pressed='true'] {
      background: color-mix(in srgb, var(--color-action-primary, #8e7bff) 22%, transparent);
      color: var(--color-text-primary, #ece8e4);
      box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-action-primary, #8e7bff) 50%, transparent);
    }
    .stage {
      position: relative;
      width: 100%;
      aspect-ratio: 1;
      touch-action: none;
    }
    #ring {
      position: absolute;
      inset: 8%;
      border-radius: 50%;
      cursor: pointer;
      background: conic-gradient(
        from 0deg,
        hsl(0 90% 55%),
        hsl(60 90% 55%),
        hsl(120 90% 55%),
        hsl(180 90% 55%),
        hsl(240 90% 55%),
        hsl(300 90% 55%),
        hsl(360 90% 55%)
      );
      -webkit-mask: radial-gradient(circle, transparent 0 58%, #000 59%);
      mask: radial-gradient(circle, transparent 0 58%, #000 59%);
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
    }
    #ringthumb {
      position: absolute;
      width: 26px;
      height: 26px;
      border-radius: 50%;
      border: 3px solid #fff;
      box-shadow: 0 0 0 1.5px rgba(0, 0, 0, 0.85), 0 2px 6px rgba(0, 0, 0, 0.5);
      transform: translate(-50%, -50%);
      pointer-events: none;
    }
    #ring:focus-visible {
      outline: none;
      box-shadow: 0 0 0 3px var(--color-border-focus, #8e7bff), 0 4px 14px rgba(0, 0, 0, 0.4);
    }
    #slice:focus-visible,
    .seg:focus-visible {
      outline: 2px solid var(--color-border-focus, #8e7bff);
      outline-offset: 2px;
    }
    #slice {
      position: absolute;
      top: 27%;
      left: 27%;
      width: 46%;
      height: 46%;
      border-radius: 10px;
      cursor: crosshair;
      image-rendering: auto;
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
      touch-action: none;
    }
    #cross {
      position: absolute;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      border: 2px solid #fff;
      box-shadow: 0 0 0 1.5px rgba(0, 0, 0, 0.75), inset 0 0 0 1px rgba(0, 0, 0, 0.4);
      transform: translate(-50%, -50%);
      pointer-events: none;
    }
    #model {
      position: absolute;
      top: 76%;
      left: 76%;
      width: 22%;
      height: 22%;
      cursor: grab;
      border-radius: 8px;
      background: radial-gradient(circle at 50% 40%, #1a1a20, #0a0809 75%);
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
      touch-action: none;
    }
    #model:active {
      cursor: grabbing;
    }
    .readout {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .swatch {
      width: 34px;
      height: 34px;
      border-radius: 8px;
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      flex: none;
    }
    .hex {
      flex: 1;
      height: 34px;
      background: var(--control-groove, #050506);
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      border-radius: 8px;
      color: var(--color-text-primary, #ece8e4);
      font: 500 13px var(--font-family-mono, monospace);
      padding: 0 10px;
      text-transform: uppercase;
    }
    .hex:focus {
      outline: none;
      border-color: var(--color-border-focus, #8e7bff);
    }
    .btn {
      height: 34px;
      padding: 0 12px;
      border-radius: 8px;
      border: 1px solid var(--color-border-strong, rgba(255, 255, 255, 0.14));
      background: transparent;
      color: var(--color-text-primary, #ece8e4);
      font: 600 12px var(--font-family-sans);
      cursor: pointer;
    }
    .btn:hover {
      background: var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }
    .palhead {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: var(--font-size-micro, 9.5px);
      letter-spacing: 0.2em;
      text-transform: uppercase;
      color: var(--color-text-faint, rgba(236, 232, 228, 0.18));
    }
    .cvd {
      display: flex;
      align-items: center;
      gap: 6px;
      font: 500 11px var(--font-family-sans);
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
      letter-spacing: 0;
      text-transform: none;
      cursor: pointer;
    }
    .cvd input {
      accent-color: var(--color-action-primary, #8e7bff);
    }
    .row {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .strip {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(0, 1fr));
      grid-auto-flow: column;
      gap: 4px;
    }
    .strip > div {
      height: 26px;
      border-radius: 5px;
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }
    input[type='range'] {
      flex: 1;
      accent-color: var(--color-action-primary, #8e7bff);
    }
  `;

  connectedCallback() {
    super.connectedCallback();
    // 6^3 RGB cube point cloud — build once (guard against reconnect duplication).
    if (this._cube.length === 0) {
      const N = 6;
      for (let r = 0; r < N; r++)
        for (let g = 0; g < N; g++)
          for (let b = 0; b < N; b++) {
            const rr = Math.round((r / (N - 1)) * 255);
            const gg = Math.round((g / (N - 1)) * 255);
            const bb = Math.round((b / (N - 1)) * 255);
            this._cube.push({ p: [rr / 255 - 0.5, gg / 255 - 0.5, bb / 255 - 0.5], c: [rr, gg, bb] });
          }
    }
    this._mq = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    this._reduced = this._mq?.matches ?? false;
    this._mq?.addEventListener?.('change', this._onReducedChange);
    if (this.hasUpdated) this._observe(); // re-arm after a disconnect/reconnect
  }

  firstUpdated() {
    this._paintSlice(true);
    this._paintPalette();
    this._observe();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
    this._raf = 0;
    this._io?.disconnect();
    this._mq?.removeEventListener?.('change', this._onReducedChange);
  }

  updated(changed: Map<string, unknown>) {
    if (changed.has('value') || changed.has('space')) this._paintSlice();
    // iwanthue clustering is costly — never run it per drag frame. Recompute on
    // count/mode changes, or on a value change only when not actively dragging.
    if (changed.has('paletteCount') || changed.has('colorBlind')) this._paintPalette();
    else if (changed.has('value') && !this._drag) this._paintPalette();
    // Move the cube's current-colour marker even when the loop is idle.
    if (changed.has('value') && this._onscreen && !this._raf) this._paintModel();
  }

  private _onReducedChange = (e: MediaQueryListEvent) => {
    this._reduced = e.matches;
    this._kick();
  };

  private _observe() {
    this._io?.disconnect();
    this._io = new IntersectionObserver(
      (entries) => {
        this._onscreen = entries[0]?.isIntersecting ?? true;
        this._kick();
      },
      { threshold: 0.01 }
    );
    this._io.observe(this);
    this._kick();
  }

  /** Animate only when it earns it: onscreen, and either dragging or motion allowed. */
  private _kick() {
    const animate = this._onscreen && (this._drag === 'model' || !this._reduced);
    if (animate) {
      if (!this._raf) this._loop();
    } else {
      cancelAnimationFrame(this._raf);
      this._raf = 0;
      if (this._onscreen) this._paintModel(); // one static frame
    }
  }

  // --- geometry ---
  private get _channels() {
    const c = chroma.valid(this.value) ? this.value : '#8e7bff';
    return SPACES[this.space].toChannels(c);
  }

  private _commit(hex: string, kind: 'hy-input' | 'hy-change') {
    this.value = hex;
    this.dispatchEvent(new CustomEvent(kind, { detail: { hex }, bubbles: true, composed: true }));
  }

  // --- slice canvas (depends only on hue + space) ---
  private _paintSlice(force = false) {
    const cv = this._sliceCv;
    if (!cv) return;
    const { h } = this._channels;
    const key = `${this.space}:${Math.round(h)}`;
    if (!force && key === this._lastSliceKey) return;
    this._lastSliceKey = key;
    const size = 96;
    cv.width = size;
    cv.height = size;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    const img = ctx.createImageData(size, size);
    const def = SPACES[this.space];
    for (let yy = 0; yy < size; yy++) {
      for (let xx = 0; xx < size; xx++) {
        const x = xx / (size - 1);
        const y = 1 - yy / (size - 1);
        const [r, g, b] = def.toRgb(h, x, y); // direct rgb, no hex serialize/reparse
        const i = (yy * size + xx) * 4;
        img.data[i] = r;
        img.data[i + 1] = g;
        img.data[i + 2] = b;
        img.data[i + 3] = 255;
      }
    }
    ctx.putImageData(img, 0, 0);
  }

  // --- 3D RGB cube ---
  private _loop = () => {
    if (!this._onscreen || (this._reduced && this._drag !== 'model')) {
      this._raf = 0;
      if (this._onscreen) this._paintModel();
      return;
    }
    if (this._drag !== 'model') this._yaw += 0.006;
    this._paintModel();
    this._raf = requestAnimationFrame(this._loop);
  };

  private _paintModel() {
    const cv = this._modelCv;
    if (!cv) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const tw = Math.max(1, Math.round(cv.clientWidth * dpr));
    const th = Math.max(1, Math.round(cv.clientHeight * dpr));
    if (cv.width !== tw || cv.height !== th) {
      cv.width = tw;
      cv.height = th;
    }
    const w = cv.width;
    const h = cv.height;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, w, h);
    const cy = Math.cos(this._yaw);
    const sy = Math.sin(this._yaw);
    const cp = Math.cos(this._pitch);
    const sp = Math.sin(this._pitch);
    const project = (p: [number, number, number]) => {
      const x1 = p[0] * cy - p[2] * sy;
      const z1 = p[0] * sy + p[2] * cy;
      const y2 = p[1] * cp - z1 * sp;
      const z2 = p[1] * sp + z1 * cp;
      return { x: x1, y: y2, z: z2 };
    };
    const R = Math.min(w, h) * 0.62;
    const pts = this._cube
      .map((pt) => ({ ...project(pt.p), c: pt.c }))
      .sort((a, b) => a.z - b.z);
    for (const pr of pts) {
      const depth = pr.z + 0.7;
      const size = Math.max(1.2 * dpr, 3.2 * dpr * depth);
      ctx.globalAlpha = 0.35 + 0.6 * Math.max(0, Math.min(1, depth));
      ctx.fillStyle = `rgb(${pr.c[0]},${pr.c[1]},${pr.c[2]})`;
      ctx.fillRect(w / 2 + pr.x * R - size / 2, h / 2 - pr.y * R - size / 2, size, size);
    }
    // current-colour marker
    if (chroma.valid(this.value)) {
      const [r, g, b] = chroma(this.value).rgb();
      const m = project([r / 255 - 0.5, g / 255 - 0.5, b / 255 - 0.5]);
      const mx = w / 2 + m.x * R;
      const my = h / 2 - m.y * R;
      ctx.globalAlpha = 1;
      ctx.beginPath();
      ctx.arc(mx, my, 4 * dpr, 0, Math.PI * 2);
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 1.6 * dpr;
      ctx.stroke();
    }
    ctx.globalAlpha = 1;
  }

  // --- iwanthue supporting palette ---
  private _paintPalette() {
    const base = chroma.valid(this.value) ? this.value : '#8e7bff';
    this._support = createTheme(base, {
      count: this.paletteCount,
      colorBlind: this.colorBlind,
      seed: this._seed,
    }).support;
  }

  // --- pointer handling ---
  private _onRing(e: PointerEvent) {
    const stage = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const dx = e.clientX - (stage.left + stage.width / 2);
    const dy = e.clientY - (stage.top + stage.height / 2);
    let deg = (Math.atan2(dy, dx) * 180) / Math.PI + 90;
    deg = ((deg % 360) + 360) % 360;
    const { x, y } = this._channels;
    this._commit(SPACES[this.space].fromChannels(deg, x, y), this._drag ? 'hy-input' : 'hy-change');
  }

  private _onSlice(e: PointerEvent) {
    const r = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const x = Math.min(1, Math.max(0, (e.clientX - r.left) / r.width));
    const y = 1 - Math.min(1, Math.max(0, (e.clientY - r.top) / r.height));
    const { h } = this._channels;
    this._commit(SPACES[this.space].fromChannels(h, x, y), this._drag ? 'hy-input' : 'hy-change');
  }

  private _startDrag(kind: 'ring' | 'slice' | 'model', e: PointerEvent) {
    this._drag = kind;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    if (kind === 'ring') this._onRing(e);
    else if (kind === 'slice') this._onSlice(e);
    else {
      this._lastModel = { x: e.clientX, y: e.clientY };
      this._kick(); // rotate even under reduced-motion while actively dragging
    }
  }
  private _lastModel = { x: 0, y: 0 };
  private _moveDrag(e: PointerEvent) {
    if (this._drag === 'ring') this._onRing(e);
    else if (this._drag === 'slice') this._onSlice(e);
    else if (this._drag === 'model') {
      this._yaw += (e.clientX - this._lastModel.x) * 0.01;
      this._pitch = Math.max(-1.3, Math.min(1.3, this._pitch + (e.clientY - this._lastModel.y) * 0.01));
      this._lastModel = { x: e.clientX, y: e.clientY };
    }
  }
  private _endDrag(e: PointerEvent) {
    const was = this._drag;
    this._drag = null;
    try {
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {
      /* capture may already be gone */
    }
    if (was === 'ring' || was === 'slice') {
      this._commit(this.value, 'hy-change');
      this._paintPalette(); // deferred from during-drag
    } else if (was === 'model') {
      this._kick(); // resume auto-rotate, or settle to a static frame under reduced-motion
    }
  }

  private _nudge(kind: 'ring' | 'slice', e: KeyboardEvent) {
    const { h, x, y } = this._channels;
    const def = SPACES[this.space];
    let nh = h;
    let nx = x;
    let ny = y;
    const step = e.shiftKey ? 0.1 : 0.02;
    if (kind === 'ring') {
      if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') nh = (h - (e.shiftKey ? 10 : 2) + 360) % 360;
      else if (e.key === 'ArrowRight' || e.key === 'ArrowUp') nh = (h + (e.shiftKey ? 10 : 2)) % 360;
      else return;
    } else {
      if (e.key === 'ArrowLeft') nx = Math.max(0, x - step);
      else if (e.key === 'ArrowRight') nx = Math.min(1, x + step);
      else if (e.key === 'ArrowDown') ny = Math.max(0, y - step);
      else if (e.key === 'ArrowUp') ny = Math.min(1, y + step);
      else return;
    }
    e.preventDefault();
    // updated() recomputes the palette once (not dragging) — no explicit call here.
    this._commit(def.fromChannels(nh, nx, ny), 'hy-change');
  }

  private _onHex(e: Event) {
    const el = e.target as HTMLInputElement;
    const v = el.value.trim();
    if (chroma.valid(v)) this._commit(chroma(v).hex(), 'hy-change');
    el.value = this.value; // re-sync the field to the canonical value (invalid/equal entries)
  }

  private _copy() {
    navigator.clipboard?.writeText(this.value).catch(() => {});
  }

  private _roll() {
    this._seed = `hyle-${Math.floor(performance.now())}`;
    this._paintPalette();
  }

  render() {
    const { h, x, y } = this._channels;
    // thumb sits on the ring band centre-line (~34% radius of the stage)
    const rad = (a: number) => ((a - 90) * Math.PI) / 180;
    const thumbR = 34;
    const tx = 50 + thumbR * Math.cos(rad(h));
    const ty = 50 + thumbR * Math.sin(rad(h));
    return html`
      <div class="app" part="app">
        <div class="spaces" role="group" aria-label="Colour space">
          ${(Object.keys(SPACES) as HyColorSpace[]).map(
            (s) => html`<button
              class="seg"
              aria-pressed=${this.space === s ? 'true' : 'false'}
              @click=${() => (this.space = s)}
            >
              ${SPACES[s].label}
            </button>`
          )}
        </div>

        <div class="stage">
          <div
            id="ring"
            role="slider"
            aria-label="Hue"
            aria-valuemin="0"
            aria-valuemax="360"
            aria-valuenow=${Math.round(h)}
            tabindex="0"
            @pointerdown=${(e: PointerEvent) => this._startDrag('ring', e)}
            @pointermove=${this._moveDrag}
            @pointerup=${this._endDrag}
            @pointercancel=${this._endDrag}
            @keydown=${(e: KeyboardEvent) => this._nudge('ring', e)}
          ></div>
          <div id="ringthumb" style="left:${tx}%; top:${ty}%; background:${this.value}"></div>
          <canvas
            id="slice"
            role="application"
            aria-label=${`Colour field: ${SPACES[this.space].xLabel} ${Math.round(x * 100)}%, ${SPACES[this.space].yLabel} ${Math.round(y * 100)}%. Arrow keys to adjust.`}
            tabindex="0"
            @pointerdown=${(e: PointerEvent) => this._startDrag('slice', e)}
            @pointermove=${this._moveDrag}
            @pointerup=${this._endDrag}
            @pointercancel=${this._endDrag}
            @keydown=${(e: KeyboardEvent) => this._nudge('slice', e)}
          ></canvas>
          <div id="cross" style="left:calc(27% + ${x * 46}%); top:calc(27% + ${(1 - y) * 46}%);"></div>
          <canvas
            id="model"
            aria-hidden="true"
            title="Drag to rotate the RGB cube"
            @pointerdown=${(e: PointerEvent) => this._startDrag('model', e)}
            @pointermove=${this._moveDrag}
            @pointerup=${this._endDrag}
            @pointercancel=${this._endDrag}
          ></canvas>
        </div>

        <div class="readout">
          <div class="swatch" style="background:${this.value}"></div>
          <input
            class="hex"
            aria-label="Hex colour value"
            .value=${this.value}
            @change=${this._onHex}
            spellcheck="false"
          />
          <button class="btn" @click=${this._copy}>Copy</button>
        </div>

        <div class="palhead">
          <span>Palette · iwanthue</span>
          <label class="cvd"
            ><input
              type="checkbox"
              .checked=${this.colorBlind}
              @change=${(e: Event) => (this.colorBlind = (e.target as HTMLInputElement).checked)}
            />
            colour-blind-safe</label
          >
        </div>
        <div class="row">
          <input
            type="range"
            aria-label="Number of palette colours"
            min="3"
            max="10"
            .value=${String(this.paletteCount)}
            @input=${(e: Event) => (this.paletteCount = Number((e.target as HTMLInputElement).value))}
          />
          <button class="btn" @click=${this._roll}>Roll</button>
        </div>
        <div class="strip">
          ${this._support.map((c) => html`<div title=${c} style="background:${c}"></div>`)}
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-color-picker': HyColorPicker;
  }
}
