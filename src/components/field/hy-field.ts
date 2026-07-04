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
    /* The engine is a living backdrop only — panes above it receive all input. */
    .bg {
      pointer-events: none;
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

  /** Procedural field — a lightweight echo of the Form-World "Bowl": concentric
   *  ridges receding into a warm glowing throat, present even at rest and alive
   *  with activity. The zero-config fallback when the engine isn't wired in. */
  private _draw(t: number) {
    const c = this._canvas;
    if (!c) return;
    const ctx = c.getContext('2d');
    if (!ctx) return;
    const w = c.width;
    const h = c.height;
    const a = this._act;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const cx = w * 0.5;
    const cy = h * 0.52;
    const diag = Math.hypot(w, h);

    // warm-dark ground with depth toward the throat
    const ground = ctx.createRadialGradient(cx, cy, 0, cx, cy, diag * 0.62);
    ground.addColorStop(0, '#191210');
    ground.addColorStop(0.5, '#0b0908');
    ground.addColorStop(1, '#000000');
    ctx.fillStyle = ground;
    ctx.fillRect(0, 0, w, h);

    // concentric ridges receding toward the centre; brighter near the throat
    const spacing = (22 + a * 18) * dpr;
    const amp = (1.5 + a * 8) * dpr;
    const speed = 0.0009 * (1 - 0.4 * a); // monumental = slower
    const rings = Math.ceil(diag / spacing);
    ctx.lineWidth = 1.4 * dpr;
    for (let i = 1; i < rings; i++) {
      const wobble = Math.sin(t * speed + i * 0.5) * amp * (i / rings);
      const r = i * spacing + wobble;
      const near = Math.max(0, 1 - r / (diag * 0.55)); // brighter toward centre
      const alpha = (0.06 + near * 0.16 + a * 0.05).toFixed(3);
      ctx.strokeStyle = `rgba(200,150,120,${alpha})`;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    }

    // the glowing throat — a warm ember at rest, brightening with activity
    const throatR = Math.max(w, h) * (0.05 + a * 0.06);
    const throat = ctx.createRadialGradient(cx, cy, 0, cx, cy, throatR * 3.4);
    const tb = (0.35 + a * 0.5).toFixed(3);
    throat.addColorStop(0, `rgba(255,224,190,${tb})`);
    throat.addColorStop(0.4, `rgba(224,148,79,${(0.18 + a * 0.28).toFixed(3)})`);
    throat.addColorStop(1, 'rgba(224,148,79,0)');
    ctx.fillStyle = throat;
    ctx.fillRect(0, 0, w, h);

    // thinking-glow — a violet cast that only rises with real activity
    if (a > 0.06) {
      const tg = ctx.createRadialGradient(cx, h * 0.3, 0, cx, h * 0.3, diag * 0.5);
      tg.addColorStop(0, `rgba(142,123,255,${(a * 0.14).toFixed(3)})`);
      tg.addColorStop(1, 'rgba(142,123,255,0)');
      ctx.fillStyle = tg;
      ctx.fillRect(0, 0, w, h);
    }

    // faint grain for atmosphere
    ctx.fillStyle = 'rgba(255,255,255,0.015)';
    for (let k = 0; k < (w * h) / 9000; k++) {
      ctx.fillRect((Math.sin(k * 12.9 + t * 0.0002) * 0.5 + 0.5) * w, (Math.cos(k * 78.2) * 0.5 + 0.5) * h, dpr, dpr);
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
