import { html } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/** Mini-display kinds, lifted verbatim from the kit's Mini Displays (line 1160). */
export type HyMiniDisplayKind = 'eq' | 'wave' | 'pulse';

/**
 * A tiny LED dot-matrix display — extracted verbatim from the Tactile Kit's
 * "Mini Displays" section (kit `miniDot`, line 1139). A grid of accent dots
 * animates one of three read-only motifs; the signal IS the state.
 *
 *  - `eq`    — a bouncing spectrum-analyser bar field
 *  - `wave`  — a travelling sine wave
 *  - `pulse` — an expanding ring from the centre
 *
 * @element hy-mini-display
 * @attr kind - `eq` (default) | `wave` | `pulse`.
 */
@customElement('hy-mini-display')
export class HyMiniDisplay extends KitElement {
  @property() kind: HyMiniDisplayKind = 'eq';

  @query('canvas') private _cv!: HTMLCanvasElement;
  private _raf = 0;
  private _ro?: ResizeObserver;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    this._miniDot(this._cv, this.kind);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
    this._ro?.disconnect();
  }

  /** The host's `--acc-rgb` as an [r,g,b] triple (kit `accRGB`, line 1091). */
  private _accRGB(def: number[]) {
    const v = getComputedStyle(this).getPropertyValue('--acc-rgb').trim();
    if (v) {
      const p = v.split(',').map((n) => +n);
      if (p.length === 3 && p.every((x) => !isNaN(x))) return p;
    }
    return def;
  }

  /** Verbatim from the kit (line 1139): the dot-matrix motif renderer. */
  private _miniDot(cv: HTMLCanvasElement, kind: HyMiniDisplayKind) {
    if (!cv) return;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    let W = 0,
      H = 0,
      dpr = 1,
      cols = 0,
      rows = 0;
    const CELL = 5;
    const resize = () => {
      const r = cv.getBoundingClientRect();
      dpr = Math.min(devicePixelRatio || 1, 2);
      W = r.width;
      H = r.height;
      cv.width = (W * dpr) | 0;
      cv.height = (H * dpr) | 0;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      cols = Math.floor(W / CELL);
      rows = Math.floor(H / CELL);
    };
    const frame = (t: number) => {
      const acc = this._accRGB([140, 123, 255]);
      ctx.clearRect(0, 0, W, H);
      const A = 'rgba(' + acc[0] + ',' + acc[1] + ',' + acc[2] + ',';
      const ox = (W - cols * CELL) / 2,
        oy = (H - rows * CELL) / 2;
      for (let j = 0; j < rows; j++) {
        for (let i = 0; i < cols; i++) {
          let lit = false;
          if (kind === 'eq') {
            const h = (0.5 + 0.5 * Math.sin(t * 0.004 + i * 0.7)) * (rows - 1);
            lit = rows - 1 - j <= h;
          } else if (kind === 'wave') {
            const yy = (rows - 1) / 2 + Math.sin(t * 0.005 + i * 0.5) * ((rows - 1) * 0.34);
            lit = Math.abs(j - yy) < 0.75;
          } else {
            const d = Math.hypot(i - (cols - 1) / 2, j - (rows - 1) / 2);
            const ring = (t * 0.013) % (Math.max(cols, rows) / 2 + 2.5);
            lit = Math.abs(d - ring) < 0.9;
          }
          const cx = ox + (i + 0.5) * CELL,
            cy = oy + (j + 0.5) * CELL;
          ctx.beginPath();
          ctx.arc(cx, cy, lit ? CELL * 0.36 : CELL * 0.27, 0, 6.2832);
          ctx.fillStyle = lit ? A + '0.95)' : 'rgba(255,255,255,0.05)';
          ctx.fill();
        }
      }
      this._raf = requestAnimationFrame(frame);
    };
    this._ro = new ResizeObserver(resize);
    this._ro.observe(cv);
    resize();
    this._raf = requestAnimationFrame(frame);
  }

  render() {
    return html`<div class="mini" part="mini"><canvas></canvas></div>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-mini-display': HyMiniDisplay;
  }
}
