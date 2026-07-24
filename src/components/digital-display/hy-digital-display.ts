import { html } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/** Digital-display variants, lifted verbatim from the kit (lines 1158-1159). */
export type HyDigitalDisplayVariant = 'halo' | 'led';

/**
 * A digital matrix display — extracted verbatim from the Tactile Kit's
 * "Digital Displays" section. Two read-only motifs rendered on a dot grid:
 *
 *  - `halo` — a halftone metaball field (kit `halftone`, line 1094), soft
 *    accent blobs drifting behind a circular vignette mask.
 *  - `led`  — a Tidbyt-style LED dot-matrix ticker (kit `ledTicker`, line 1117)
 *    scrolling a monospace marquee.
 *
 * @element hy-digital-display
 * @attr variant - `halo` (default) | `led`.
 */
@customElement('hy-digital-display')
export class HyDigitalDisplay extends KitElement {
  @property() variant: HyDigitalDisplayVariant = 'halo';

  @query('canvas') private _cv!: HTMLCanvasElement;
  private _raf = 0;
  private _ro?: ResizeObserver;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    if (this.variant === 'led') this._ledTicker(this._cv);
    else this._halftone(this._cv);
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

  /** Verbatim from the kit (line 1094): halftone metaball field. */
  private _halftone(cv: HTMLCanvasElement) {
    if (!cv) return;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    let W = 0,
      H = 0,
      dpr = 1,
      cols = 0,
      rows = 0;
    const GRID = 9;
    const resize = () => {
      const r = cv.getBoundingClientRect();
      dpr = Math.min(devicePixelRatio || 1, 2);
      W = r.width;
      H = r.height;
      cv.width = (W * dpr) | 0;
      cv.height = (H * dpr) | 0;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      cols = Math.ceil(W / GRID);
      rows = Math.ceil(H / GRID);
    };
    const blobs = [...Array(5)].map(() => ({
      r: 0.09 + Math.random() * 0.05,
      px: Math.random() * 6.28,
      py: Math.random() * 6.28,
      sx: 0.00012 + Math.random() * 0.0002,
      sy: 0.00012 + Math.random() * 0.0002,
      ax: 0.1 + Math.random() * 0.12,
      ay: 0.1 + Math.random() * 0.12,
      cx: 0.5,
      cy: 0.5,
    }));
    const frame = (t: number) => {
      const acc = this._accRGB([140, 123, 255]);
      ctx.clearRect(0, 0, W, H);
      for (const b of blobs) {
        b.cx = 0.5 + Math.sin(t * b.sx + b.px) * b.ax;
        b.cy = 0.5 + Math.cos(t * b.sy + b.py) * b.ay;
      }
      for (let j = 0; j < rows; j++) {
        for (let i = 0; i < cols; i++) {
          const ux = (i + 0.5) / cols,
            uy = (j + 0.5) / rows;
          let f = 0;
          for (const b of blobs) {
            const dx = ux - b.cx,
              dy = uy - b.cy;
            f += Math.exp(-(dx * dx + dy * dy) / (2 * b.r * b.r));
          }
          const cdx = ux - 0.5,
            cdy = uy - 0.5,
            cd = Math.sqrt(cdx * cdx + cdy * cdy),
            mask = Math.max(0, 1 - Math.pow(Math.min(1, cd / 0.58), 2.4));
          const v = Math.min(1, Math.max(0, f * 0.95 - 0.4)) * mask;
          if (v <= 0.03) continue;
          const mix = Math.pow(v, 1.5);
          const cr = Math.round(acc[0] + (255 - acc[0]) * mix),
            cg = Math.round(acc[1] + (255 - acc[1]) * mix),
            cb = Math.round(acc[2] + (255 - acc[2]) * mix);
          ctx.fillStyle = 'rgba(' + cr + ',' + cg + ',' + cb + ',' + (0.4 + 0.6 * v) + ')';
          ctx.beginPath();
          ctx.arc((i + 0.5) * GRID, (j + 0.5) * GRID, v * GRID * 0.62, 0, 6.2832);
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

  /** Verbatim from the kit (line 1117): Tidbyt-style LED dot-matrix ticker. */
  private _ledTicker(cv: HTMLCanvasElement) {
    if (!cv) return;
    const ctx = cv.getContext('2d');
    if (!ctx) return;
    let W = 0,
      H = 0,
      dpr = 1,
      cols = 0,
      rows = 0;
    const CELL = 6;
    const off = document.createElement('canvas');
    const octx = off.getContext('2d');
    if (!octx) return;
    const TXT = 'HYLE   169.99  +0.88%      TACTILE KIT      ';
    let textW = 0;
    const build = () => {
      if (rows < 1) return;
      off.height = rows;
      const fs = Math.round(rows * 0.96);
      octx.font = '700 ' + fs + 'px ui-monospace,Menlo,monospace';
      textW = Math.ceil(octx.measureText(TXT).width) + cols + 4;
      off.width = textW;
      octx.font = '700 ' + fs + 'px ui-monospace,Menlo,monospace';
      octx.textBaseline = 'middle';
      octx.fillStyle = '#fff';
      octx.clearRect(0, 0, off.width, off.height);
      octx.fillText(TXT, cols, rows / 2);
    };
    const resize = () => {
      const r = cv.getBoundingClientRect();
      dpr = Math.min(devicePixelRatio || 1, 2);
      W = r.width;
      H = r.height;
      cv.width = (W * dpr) | 0;
      cv.height = (H * dpr) | 0;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      cols = Math.ceil(W / CELL);
      rows = Math.ceil(H / CELL);
      build();
    };
    let scroll = 0;
    const frame = () => {
      const acc = this._accRGB([140, 123, 255]);
      ctx.clearRect(0, 0, W, H);
      scroll += 0.4;
      if (scroll >= textW - cols) scroll = 0;
      const sx = Math.floor(scroll);
      let data;
      try {
        data = octx.getImageData(sx, 0, cols, rows).data;
      } catch (e) {
        this._raf = requestAnimationFrame(frame);
        return;
      }
      const A = 'rgba(' + acc[0] + ',' + acc[1] + ',' + acc[2] + ',';
      for (let j = 0; j < rows; j++) {
        for (let i = 0; i < cols; i++) {
          const lit = data[(j * cols + i) * 4 + 3] > 90;
          const cx = (i + 0.5) * CELL,
            cy = (j + 0.5) * CELL;
          ctx.beginPath();
          ctx.arc(cx, cy, lit ? CELL * 0.36 : CELL * 0.3, 0, 6.2832);
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
    return html`<div class="dmat ${this.variant === 'led' ? 'dmat-led' : ''}" part="matrix">
      <canvas></canvas>
    </div>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-digital-display': HyDigitalDisplay;
  }
}
