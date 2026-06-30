import { LitElement, css, html } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';

export type HyFieldState = 'idle' | 'thinking' | 'deep';

const STATE_ACTIVITY: Record<HyFieldState, number> = { idle: 0.05, thinking: 0.5, deep: 1 };

/**
 * The Field — Hyle's living material layer behind the pane. Motion is bound to
 * system state: matte and near-still at idle, gaining slow, monumental motion
 * as computation deepens. Light gathers only where thinking happens.
 *
 * By default it renders a lightweight, fully-owned procedural animation driven
 * by `activity`/`state`. Set `src` to host the heavier Form-World WebGL engine
 * instead (a passive hero surface).
 *
 * @element hy-field
 * @slot - Content layered over the field (typically one or more <hy-pane>).
 * @attr state - idle | thinking | deep (maps to an activity level).
 * @attr activity - explicit 0..1 activity, overrides `state`.
 * @attr src - URL of the Form-World engine HTML (renders the engine instead).
 */
@customElement('hy-field')
export class HyField extends LitElement {
  /** URL to the Form-World engine. Empty → the procedural state field. */
  @property() src = '';
  /** Named activity level. */
  @property({ reflect: true }) state: HyFieldState = 'idle';
  /** Explicit activity 0..1; when ≥ 0 it overrides `state`. */
  @property({ type: Number }) activity = -1;
  @property() label = 'Hyle field';

  @query('canvas') private _canvas?: HTMLCanvasElement;

  private _raf = 0;
  private _act = 0.05; // eased current activity
  private _ro?: ResizeObserver;
  private _reduced = false;

  static styles = css`
    :host {
      display: block;
      position: relative;
      width: 100%;
      min-height: 320px;
      overflow: hidden;
      background: var(--color-background-field, #000);
      border-radius: var(--radius-lg, 12px);
      isolation: isolate;
    }
    .bg,
    canvas {
      position: absolute;
      inset: 0;
      z-index: 0;
      width: 100%;
      height: 100%;
      display: block;
      border: 0;
    }
    .overlay {
      position: relative;
      z-index: 1;
      width: 100%;
      height: 100%;
      min-height: inherit;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      pointer-events: none;
    }
    ::slotted(*) {
      pointer-events: auto;
    }
  `;

  private get _target() {
    if (this.activity >= 0) return Math.min(1, this.activity);
    return STATE_ACTIVITY[this.state] ?? 0.05;
  }

  firstUpdated() {
    if (this.src) return;
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this._act = this._target;
    this._ro = new ResizeObserver(() => this._resize());
    this._ro.observe(this);
    this._resize();
    if (this._reduced) this._draw(0);
    else this._loop();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
    this._ro?.disconnect();
  }

  updated(changed: Map<string, unknown>) {
    if ((changed.has('state') || changed.has('activity')) && this._reduced && this._canvas) {
      this._act = this._target;
      this._draw(performance.now());
    }
  }

  private _resize() {
    const c = this._canvas;
    if (!c) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    c.width = Math.max(1, c.clientWidth * dpr);
    c.height = Math.max(1, c.clientHeight * dpr);
  }

  private _loop = () => {
    this._act += (this._target - this._act) * 0.04; // ease toward state
    this._draw(performance.now());
    this._raf = requestAnimationFrame(this._loop);
  };

  /** Procedural field: a near-black ground, a violet glow where thinking gathers,
   *  and concentric cymatic ridges whose motion scales with activity. */
  private _draw(t: number) {
    const c = this._canvas;
    if (!c) return;
    const ctx = c.getContext('2d');
    if (!ctx) return;
    const w = c.width;
    const h = c.height;
    const a = this._act;
    const cx = w * 0.5;
    const cy = h * 0.62;

    // ground
    const g = ctx.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, '#0a0809');
    g.addColorStop(1, '#000000');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);

    // glow — light only where thinking happens
    const glowR = Math.max(w, h) * (0.35 + a * 0.4);
    const glow = ctx.createRadialGradient(cx, h * 0.22, 0, cx, h * 0.22, glowR);
    glow.addColorStop(0, `rgba(142,123,255,${(0.05 + a * 0.22).toFixed(3)})`);
    glow.addColorStop(1, 'rgba(142,123,255,0)');
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, w, h);

    // concentric cymatic ridges — mass grows, motion slows, with depth
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const spacing = (26 + a * 26) * dpr;
    const amp = a * 9 * dpr;
    const speed = 0.0011 * (1 - 0.45 * a); // monumental = slower
    const rings = Math.ceil(Math.hypot(w, h) / spacing);
    ctx.lineWidth = 1 * dpr;
    for (let i = 1; i < rings; i++) {
      const wobble = Math.sin(t * speed + i * 0.55) * amp;
      const r = i * spacing + wobble;
      const fade = Math.max(0, 1 - r / (Math.hypot(w, h) * 0.7));
      ctx.strokeStyle = `rgba(236,232,228,${(0.05 * fade + a * 0.04 * fade).toFixed(3)})`;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    }
  }

  render() {
    if (this.src) {
      return html`
        <iframe
          class="bg"
          part="engine"
          src=${this.src}
          title=${this.label}
          loading="lazy"
          aria-hidden="true"
          scrolling="no"
        ></iframe>
        <div class="overlay"><slot></slot></div>
      `;
    }
    return html`
      <canvas part="canvas" role="img" aria-label=${this.label}></canvas>
      <div class="overlay"><slot></slot></div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-field': HyField;
  }
}
