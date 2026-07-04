import { LitElement, css, html } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';

type AnyCanvas = HTMLCanvasElement | OffscreenCanvas;
const makeCanvas = (w: number, h: number): AnyCanvas => {
  if (typeof OffscreenCanvas !== 'undefined') return new OffscreenCanvas(w, h);
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  return c;
};

const THETA_MAX = 1.22; // visible arc half-angle (~70°)
const SIN_MAX = Math.sin(THETA_MAX);

/**
 * A vertical scroll wheel — a moulded cylinder rendered on canvas, with detent
 * feedback (a haptic buzz + a soft mechanical click per ridge) and momentum.
 * Drag, mouse-wheel, or arrow keys. "Controls you want to touch."
 *
 * @element hy-scroll-wheel
 * @fires hy-input  - `detail.value` on each detent crossing while moving.
 * @fires hy-change - `detail.value` when it settles.
 * @attr value - Current detent index (integer).
 * @attr pitch - Ridge pitch as a fraction of wheel height (0.012–0.042).
 * @attr min / max - Optional bounds on value.
 * @attr haptics - Vibrate on each detent (default on).
 * @attr sound - Play a click on each detent (default on).
 */
@customElement('hy-scroll-wheel')
export class HyScrollWheel extends LitElement {
  @property({ type: Number }) value = 0;
  @property({ type: Number }) pitch = 0.024;
  @property({ type: Number }) min?: number;
  @property({ type: Number }) max?: number;
  @property({ type: Boolean, reflect: true }) disabled = false;
  @property({ type: Boolean }) haptics = true;
  @property({ type: Boolean }) sound = true;

  @query('canvas') private _cv!: HTMLCanvasElement;

  static styles = css`
    :host {
      display: inline-block;
      height: 320px;
      aspect-ratio: 132 / 482;
      user-select: none;
      -webkit-user-select: none;
    }
    .assembly {
      position: relative;
      width: 100%;
      height: 100%;
      outline: none;
      cursor: grab;
      touch-action: none;
    }
    .assembly.dragging {
      cursor: grabbing;
    }
    .assembly:focus-visible {
      outline: 2px solid var(--color-border-focus, #8e7bff);
      outline-offset: 10px;
      border-radius: 24px;
    }
    :host([disabled]) .assembly {
      cursor: not-allowed;
      opacity: 0.5;
    }
    canvas {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      display: block;
    }
  `;

  // geometry (device px)
  private _dpr = 1;
  private _W = 0;
  private _H = 0;
  private _RW = 0;
  private _RH = 0;
  private _rad = 0;
  private _gut = 0;
  private _faceX = 0;
  private _faceW = 0;
  private _cyc = 0;
  private _R = 0;
  private _Rtrue = 0;
  private _pitchPx = 0;
  private _under: AnyCanvas | null = null;
  private _over: AnyCanvas | null = null;

  // state
  private _offset = 0;
  private _velocity = 0;
  private _dragging = false;
  private _lastDetent = 0;
  private _raf = 0;
  private _lastT = 0;
  private _dirty = true;
  private _reduced = false;
  private _internal = false;
  private _ro?: ResizeObserver;
  private _ac: AudioContext | null = null;
  private _lastTick = 0;

  firstUpdated() {
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this._ro = new ResizeObserver(() => this._resize());
    this._ro.observe(this._assembly());
    this._resize();
    if (this.value) this._offsetFromValue();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
    this._raf = 0;
    this._ro?.disconnect();
    this._ac?.close().catch(() => {});
    this._ac = null;
  }

  updated(changed: Map<string, unknown>) {
    if (changed.has('value') && !this._internal && this._pitchPx > 0) this._offsetFromValue();
    if (changed.has('pitch') && this._RH > 0) {
      this._pitchPx = this.pitch * this._RH;
      this._lastDetent = Math.floor(this._offset / this._pitchPx);
      this._dirty = true;
      this._kick();
    }
    this._internal = false;
  }

  private _assembly() {
    return this.renderRoot.querySelector('.assembly') as HTMLElement;
  }
  private _offsetFromValue() {
    this._offset = this.value * this._pitchPx;
    this._lastDetent = Math.floor(this._offset / this._pitchPx);
    this._dirty = true;
    this._kick();
  }
  private _clamp() {
    if (this.min != null) this._offset = Math.max(this.min * this._pitchPx, this._offset);
    if (this.max != null) this._offset = Math.min(this.max * this._pitchPx, this._offset);
  }

  // ---- geometry ----
  private _rr(c: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
    c.beginPath();
    c.moveTo(x + r, y);
    c.arcTo(x + w, y, x + w, y + h, r);
    c.arcTo(x + w, y + h, x, y + h, r);
    c.arcTo(x, y + h, x, y, r);
    c.arcTo(x, y, x + w, y, r);
    c.closePath();
  }

  private _resize() {
    const el = this._assembly();
    if (!el) return;
    this._dpr = Math.min(window.devicePixelRatio || 1, 3);
    const r = el.getBoundingClientRect();
    this._W = Math.round(r.width * this._dpr);
    this._H = Math.round(r.height * this._dpr);
    if (!this._W || !this._H) return;
    this._cv.width = this._W;
    this._cv.height = this._H;
    this._RW = this._W;
    this._RH = this._H;
    this._rad = 0.115 * this._RW;
    this._gut = 0.072 * this._RW;
    this._faceX = this._gut;
    this._faceW = this._RW - 2 * this._gut;
    this._cyc = this._RH / 2;
    this._R = this._RH / 2 - 0.006 * this._RH;
    this._Rtrue = this._R / SIN_MAX;
    this._pitchPx = this.pitch * this._RH;
    this._buildStatic();
    this._dirty = true;
    this._kick();
  }

  private _buildStatic() {
    const { _RW: RW, _RH: RH, _rad: rad, _gut: gut, _faceX: faceX, _faceW: faceW } = this;
    this._under = makeCanvas(this._W, this._H);
    const c = this._under.getContext('2d') as OffscreenCanvasRenderingContext2D;
    const floor = c.createLinearGradient(0, 0, 0, RH);
    floor.addColorStop(0.0, '#6a6c66');
    floor.addColorStop(0.1, '#8f918a');
    floor.addColorStop(0.5, '#a7a8a1');
    floor.addColorStop(0.94, '#94958f');
    floor.addColorStop(1.0, '#787a74');
    this._rr(c, 0, 0, RW, RH, rad);
    c.fillStyle = floor;
    c.fill();
    c.save();
    this._rr(c, 0, 0, RW, RH, rad);
    c.clip();
    const lw = c.createLinearGradient(0, 0, gut * 1.6, 0);
    lw.addColorStop(0, 'rgba(214,215,209,0.85)');
    lw.addColorStop(1, 'rgba(214,215,209,0)');
    c.fillStyle = lw;
    c.fillRect(0, 0, gut * 1.6, RH);
    const rw2 = c.createLinearGradient(RW - gut * 1.6, 0, RW, 0);
    rw2.addColorStop(0, 'rgba(66,68,63,0)');
    rw2.addColorStop(1, 'rgba(66,68,63,0.55)');
    c.fillStyle = rw2;
    c.fillRect(RW - gut * 1.6, 0, gut * 1.6, RH);
    c.restore();

    this._over = makeCanvas(this._W, this._H);
    const o = this._over.getContext('2d') as OffscreenCanvasRenderingContext2D;
    o.save();
    this._rr(o, 0, 0, RW, RH, rad);
    o.clip();
    const aoL = o.createLinearGradient(faceX, 0, faceX + faceW * 0.09, 0);
    aoL.addColorStop(0, 'rgba(52,54,49,0.20)');
    aoL.addColorStop(1, 'rgba(52,54,49,0)');
    o.fillStyle = aoL;
    o.fillRect(faceX, 0, faceW * 0.09, RH);
    const aoR = o.createLinearGradient(faceX + faceW * 0.9, 0, faceX + faceW, 0);
    aoR.addColorStop(0, 'rgba(52,54,49,0)');
    aoR.addColorStop(1, 'rgba(52,54,49,0.30)');
    o.fillStyle = aoR;
    o.fillRect(faceX + faceW * 0.9, 0, faceW * 0.1, RH);
    const ts = o.createLinearGradient(0, 0, 0, RH * 0.105);
    ts.addColorStop(0.0, 'rgba(38,40,35,0.18)');
    ts.addColorStop(0.28, 'rgba(38,40,35,0.68)');
    ts.addColorStop(0.52, 'rgba(38,40,35,0.26)');
    ts.addColorStop(1.0, 'rgba(38,40,35,0)');
    o.fillStyle = ts;
    o.fillRect(0, 0, RW, RH * 0.105);
    const ch = o.createLinearGradient(0, RH * 0.004, 0, RH * 0.022);
    ch.addColorStop(0, 'rgba(230,231,225,0.60)');
    ch.addColorStop(1, 'rgba(230,231,225,0)');
    o.fillStyle = ch;
    o.fillRect(faceX * 0.4, RH * 0.004, RW - faceX * 0.8, RH * 0.02);
    const bs = o.createLinearGradient(0, RH, 0, RH * 0.95);
    bs.addColorStop(0.0, 'rgba(30,32,28,0.62)');
    bs.addColorStop(0.38, 'rgba(30,32,28,0.24)');
    bs.addColorStop(1.0, 'rgba(30,32,28,0)');
    o.fillStyle = bs;
    o.fillRect(0, RH * 0.95, RW, RH * 0.05);
    o.lineWidth = Math.max(1, RW * 0.012);
    o.strokeStyle = 'rgba(40,42,38,0.28)';
    this._rr(o, o.lineWidth / 2, o.lineWidth / 2, RW - o.lineWidth, RH - o.lineWidth, rad - o.lineWidth / 2);
    o.stroke();
    o.restore();
  }

  // ---- wheel face shading ----
  private _smooth(a: number, b: number, x: number) {
    const t = Math.max(0, Math.min(1, (x - a) / (b - a)));
    return t * t * (3 - 2 * t);
  }
  private _shade(theta: number, s: number) {
    let L = 0.598 + 0.395 * Math.cos(theta - 0.08);
    const u = Math.abs(theta) / THETA_MAX;
    const roll = theta > 0 ? 0.34 * this._smooth(0.72, 1.0, u) : 0.22 * this._smooth(0.78, 1.0, u);
    L *= 1 - roll;
    const fs = (((s / this._pitchPx) % 1) + 1) % 1;
    const amp = 0.017 * (0.45 + 0.55 * u);
    L += amp * Math.sin(fs * Math.PI * 2);
    const g = fs - 0.5;
    L -= amp * 0.95 * Math.exp(-(g * g) / (2 * 0.06 * 0.06));
    return Math.max(0, Math.min(1, L));
  }

  private _drawWheel(ctx: CanvasRenderingContext2D) {
    ctx.save();
    this._rr(ctx, 1, 1, this._RW - 2, this._RH - 2, this._rad - 1);
    ctx.clip();
    const y0 = Math.max(0, Math.floor(this._cyc - this._R));
    const y1 = Math.min(this._RH, Math.ceil(this._cyc + this._R));
    for (let y = y0; y < y1; y++) {
      const t = Math.max(-0.9995, Math.min(0.9995, ((y + 0.5 - this._cyc) / this._R) * SIN_MAX));
      const theta = Math.asin(t);
      const s = theta * this._Rtrue + this._offset;
      const L = this._shade(theta, s);
      const v = L * 255;
      const dB = Math.max(0, Math.min(8, (8 * (242 - v)) / 102));
      ctx.fillStyle = `rgb(${Math.round(v)},${Math.round(Math.min(255, v + 1.2))},${Math.round(v - dB)})`;
      ctx.fillRect(this._faceX, y, this._faceW, 1);
    }
    ctx.restore();
  }

  private _render() {
    const ctx = this._cv.getContext('2d');
    if (!ctx || !this._under || !this._over) return;
    const { _RW: RW, _RH: RH, _rad: rad } = this;
    ctx.clearRect(0, 0, this._W, this._H);
    ctx.save();
    ctx.shadowColor = 'rgba(60,62,56,0.10)';
    ctx.shadowBlur = RW * 0.06;
    ctx.shadowOffsetY = RW * 0.012;
    this._rr(ctx, 0, 0, RW, RH, rad);
    ctx.fillStyle = '#8a8b85';
    ctx.fill();
    ctx.restore();
    ctx.drawImage(this._under as CanvasImageSource, 0, 0);
    this._drawWheel(ctx);
    ctx.drawImage(this._over as CanvasImageSource, 0, 0);
    ctx.save();
    ctx.globalAlpha = 0.7;
    ctx.fillStyle = '#fbfbf8';
    ctx.fillRect(rad * 0.6, RH - 1.5 * this._dpr, RW - rad * 1.2, 1.5 * this._dpr);
    ctx.restore();
  }

  // ---- feedback ----
  private _ensureAudio() {
    if (this._ac || !this.sound) return;
    try {
      const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this._ac = new Ctor();
    } catch {
      this._ac = null;
    }
  }
  private _tick() {
    const now = performance.now();
    if (now - this._lastTick < 24) return;
    this._lastTick = now;
    if (this.haptics) {
      try {
        navigator.vibrate?.(2);
      } catch {
        /* not supported */
      }
    }
    const ac = this._ac;
    if (!this.sound || !ac || ac.state !== 'running') return;
    const t = ac.currentTime;
    const len = Math.floor(ac.sampleRate * 0.012);
    const buf = ac.createBuffer(1, len, ac.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 2.5);
    const src = ac.createBufferSource();
    src.buffer = buf;
    const bp = ac.createBiquadFilter();
    bp.type = 'bandpass';
    bp.frequency.value = 2600;
    bp.Q.value = 1.4;
    const g = ac.createGain();
    g.gain.value = 0.05;
    src.connect(bp).connect(g).connect(ac.destination);
    src.start(t);
  }

  private _checkDetent(kind: 'hy-input' | 'hy-change') {
    this._clamp();
    if (this._pitchPx <= 0) return;
    const d = Math.floor(this._offset / this._pitchPx);
    if (d !== this._lastDetent) {
      this._lastDetent = d;
      this._tick();
      this._internal = true;
      this.value = d;
      this.dispatchEvent(new CustomEvent('hy-input', { detail: { value: d }, bubbles: true, composed: true }));
    }
    this._assembly()?.setAttribute('aria-valuenow', String(Math.round(this._offset / this._pitchPx)));
    if (kind === 'hy-change') {
      this.dispatchEvent(
        new CustomEvent('hy-change', { detail: { value: this._lastDetent }, bubbles: true, composed: true })
      );
    }
  }

  // ---- motion ----
  private _kick() {
    if (!this._raf) {
      this._lastT = performance.now();
      this._raf = requestAnimationFrame(this._loop);
    }
  }
  private _loop = (now: number) => {
    this._raf = 0;
    const dt = Math.min(0.05, (now - this._lastT) / 1000);
    this._lastT = now;
    if (!this._dragging && Math.abs(this._velocity) > 2) {
      this._offset += this._velocity * dt;
      this._velocity *= Math.exp(-dt / 0.3);
      this._clamp();
      this._checkDetent('hy-input');
      this._dirty = true;
    } else if (!this._dragging && this._velocity !== 0) {
      this._velocity = 0;
      this._checkDetent('hy-change'); // settled
    }
    if (this._dirty) {
      this._render();
      this._dirty = false;
    }
    if (this._dragging || Math.abs(this._velocity) > 2) this._kick();
  };

  // ---- input ----
  private _lastY = 0;
  private _lastMoveT = 0;
  private _vSample = 0;

  private _onDown = (e: PointerEvent) => {
    if (this.disabled) return;
    this._ensureAudio();
    if (this._ac?.state === 'suspended') this._ac.resume();
    this._dragging = true;
    this._velocity = 0;
    this._lastY = e.clientY;
    this._lastMoveT = performance.now();
    this._vSample = 0;
    this._assembly().classList.add('dragging');
    this._assembly().setPointerCapture(e.pointerId);
    this._kick();
  };
  private _onMove = (e: PointerEvent) => {
    if (!this._dragging) return;
    const now = performance.now();
    const dy = (e.clientY - this._lastY) * this._dpr;
    this._lastY = e.clientY;
    this._offset -= dy;
    const dt = Math.max(1, now - this._lastMoveT) / 1000;
    this._vSample = 0.7 * this._vSample + 0.3 * (-dy / dt);
    this._lastMoveT = now;
    this._checkDetent('hy-input');
    this._dirty = true;
    this._kick();
  };
  private _onUp = (e: PointerEvent) => {
    if (!this._dragging) return;
    this._dragging = false;
    this._assembly().classList.remove('dragging');
    try {
      this._assembly().releasePointerCapture(e.pointerId);
    } catch {
      /* already released */
    }
    this._velocity = this._reduced ? 0 : this._vSample;
    if (this._velocity === 0) this._checkDetent('hy-change');
    this._kick();
  };
  private _onWheel = (e: WheelEvent) => {
    if (this.disabled) return;
    e.preventDefault();
    this._offset += e.deltaY * 0.6 * this._dpr;
    this._checkDetent('hy-change');
    this._dirty = true;
    this._kick();
  };
  private _onKey = (e: KeyboardEvent) => {
    if (this.disabled) return;
    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
      e.preventDefault();
      this._ensureAudio();
      if (this._ac?.state === 'suspended') this._ac.resume();
      this._offset += (e.key === 'ArrowDown' ? 1 : -1) * this._pitchPx;
      this._checkDetent('hy-change');
      this._dirty = true;
      this._kick();
    }
  };

  render() {
    return html`
      <div
        class="assembly"
        role="slider"
        aria-label="Scroll wheel"
        aria-orientation="vertical"
        aria-valuenow=${this.value}
        aria-valuemin=${this.min ?? -1000000}
        aria-valuemax=${this.max ?? 1000000}
        tabindex=${this.disabled ? -1 : 0}
        @pointerdown=${this._onDown}
        @pointermove=${this._onMove}
        @pointerup=${this._onUp}
        @pointercancel=${this._onUp}
        @wheel=${this._onWheel}
        @keydown=${this._onKey}
      >
        <canvas></canvas>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-scroll-wheel': HyScrollWheel;
  }
}
