import { LitElement, css, html } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';

/**
 * A waveform display. Renders a symmetric bar waveform on a canvas; `live`
 * scrolls a gently evolving signal. Read-only — the signal is the state.
 *
 * @element hy-waveform
 * @attr live - Animate a scrolling signal.
 * @attr bars - Number of bars.
 */
@customElement('hy-waveform')
export class HyWaveform extends LitElement {
  @property({ type: Boolean, reflect: true }) live = false;
  @property({ type: Number }) bars = 48;
  /** Tint the waveform with the accent instead of monochrome screen ink. */
  @property({ type: Boolean }) accent = false;

  @query('canvas') private _canvas!: HTMLCanvasElement;
  private _raf = 0;
  private _amps: number[] = [];
  private _reduced = false;

  static styles = css`
    :host {
      display: block;
    }
    .frame {
      width: 100%;
      height: 72px;
      border-radius: var(--radius-md, 8px);
      background: var(--control-groove, #050506);
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.9);
      overflow: hidden;
    }
    canvas {
      display: block;
      width: 100%;
      height: 100%;
    }
  `;

  firstUpdated() {
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this._amps = Array.from({ length: this.bars }, (_, i) => 0.2 + 0.6 * Math.abs(Math.sin(i * 0.5)));
    this._draw();
    if (this.live && !this._reduced) this._loop();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
  }

  updated(changed: Map<string, unknown>) {
    if (changed.has('live')) {
      cancelAnimationFrame(this._raf);
      if (this.live && !this._reduced) this._loop();
      else this._draw();
    }
  }

  private _ink() {
    // Screen ink is monochrome by default (the kit's screens are clean, not
    // accent-flooded); opt into the accent tint with the `accent` attribute.
    if (this.accent) {
      return getComputedStyle(this).getPropertyValue('--color-action-primary').trim() || '#8e7bff';
    }
    return getComputedStyle(this).getPropertyValue('--control-screen-ink').trim() || '#dddbd6';
  }

  private _draw() {
    const c = this._canvas;
    if (!c) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = (c.width = Math.max(1, c.clientWidth * dpr));
    const h = (c.height = Math.max(1, c.clientHeight * dpr));
    const ctx = c.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = this._ink();
    const n = this._amps.length;
    const gap = w / n;
    const bw = Math.max(1, gap * 0.5);
    for (let i = 0; i < n; i++) {
      const a = this._amps[i] * (h * 0.42);
      ctx.globalAlpha = 0.55 + this._amps[i] * 0.45;
      ctx.fillRect(i * gap + (gap - bw) / 2, h / 2 - a, bw, a * 2);
    }
    ctx.globalAlpha = 1;
  }

  private _loop = () => {
    // shift left, append a smooth new sample
    const last = this._amps[this._amps.length - 1] ?? 0.4;
    const next = Math.min(1, Math.max(0.08, last + (Math.sin(performance.now() / 240) * 0.5 + (Math.random() - 0.5) * 0.4)));
    this._amps.push(next);
    if (this._amps.length > this.bars) this._amps.shift();
    this._draw();
    this._raf = requestAnimationFrame(this._loop);
  };

  render() {
    return html`<div class="frame" part="frame"><canvas></canvas></div>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-waveform': HyWaveform;
  }
}
