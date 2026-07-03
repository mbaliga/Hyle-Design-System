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
  xLabel: string;
  yLabel: string;
}

const OKLCH_CMAX = 0.4;

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
        from 90deg,
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
      box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.5), 0 2px 6px rgba(0, 0, 0, 0.5);
      transform: translate(-50%, -50%);
      pointer-events: none;
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
      box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.6);
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
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    // 6^3 RGB cube point cloud, centred on the origin.
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

  firstUpdated() {
    this._paintSlice(true);
    this._paintPalette();
    this._loop();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
  }

  updated(changed: Map<string, unknown>) {
    if (changed.has('value') || changed.has('space')) this._paintSlice();
    // iwanthue clustering is costly — never run it per drag frame. Recompute on
    // count/mode changes, or on a value change only when not actively dragging.
    if (changed.has('paletteCount') || changed.has('colorBlind')) this._paintPalette();
    else if (changed.has('value') && !this._drag) this._paintPalette();
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
    const size = 72;
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
        const [r, g, b] = chroma(def.fromChannels(h, x, y)).rgb();
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
    if (!this._drag && !this._reduced) this._yaw += 0.006;
    this._paintModel();
    this._raf = requestAnimationFrame(this._loop);
  };

  private _paintModel() {
    const cv = this._modelCv;
    if (!cv) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = (cv.width = Math.max(1, cv.clientWidth * dpr));
    const h = (cv.height = Math.max(1, cv.clientHeight * dpr));
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
    else this._lastModel = { x: e.clientX, y: e.clientY };
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
    this._commit(def.fromChannels(nh, nx, ny), 'hy-change');
    this._paintPalette();
  }

  private _onHex(e: Event) {
    const v = (e.target as HTMLInputElement).value.trim();
    if (chroma.valid(v)) this._commit(chroma(v).hex(), 'hy-change');
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
        <div class="spaces" role="tablist">
          ${(Object.keys(SPACES) as HyColorSpace[]).map(
            (s) => html`<button
              class="seg"
              role="tab"
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
            aria-label=${`${SPACES[this.space].xLabel} / ${SPACES[this.space].yLabel}`}
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
            title="Drag to rotate the RGB cube"
            @pointerdown=${(e: PointerEvent) => this._startDrag('model', e)}
            @pointermove=${this._moveDrag}
            @pointerup=${this._endDrag}
            @pointercancel=${this._endDrag}
          ></canvas>
        </div>

        <div class="readout">
          <div class="swatch" style="background:${this.value}"></div>
          <input class="hex" .value=${this.value} @change=${this._onHex} spellcheck="false" />
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
