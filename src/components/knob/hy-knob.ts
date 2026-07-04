import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

const SWEEP = 270; // degrees of travel
const START = -135; // degrees from 12 o'clock

/**
 * A rotary knob — drag vertically (or use ↑/↓) to set a value. The pointer and
 * an accent arc show the value; nothing is written in words.
 *
 * @element hy-knob
 * @fires hy-input - `detail.value` as the knob turns.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-knob')
export class HyKnob extends LitElement {
  @property({ type: Number }) min = 0;
  @property({ type: Number }) max = 100;
  @property({ type: Number }) value = 50;
  @property({ type: Number }) size = 72;
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-flex;
      touch-action: none;
    }
    .dial {
      position: relative;
      border-radius: 50%;
      cursor: ns-resize;
      background:
        repeating-radial-gradient(
            circle at 50% 50%,
            rgba(255, 255, 255, 0.05) 0 0.5px,
            transparent 0.5px 2.5px,
            rgba(0, 0, 0, 0.08) 2.5px 3px,
            transparent 3px 5.5px
          ),
        radial-gradient(circle at 50% 32%, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a) 72%);
      box-shadow:
        inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)),
        inset 0 -3px 6px var(--control-groove, #050506),
        0 3px 6px rgba(0, 0, 0, 0.5);
      outline: none;
    }
    .dial:focus-visible {
      box-shadow:
        inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)),
        0 0 0 2px var(--color-border-focus, #8e7bff);
    }
    :host([disabled]) .dial {
      cursor: not-allowed;
      opacity: 0.5;
    }
    svg {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      overflow: visible;
    }
    .track {
      fill: none;
      stroke: var(--control-groove, #050506);
      stroke-linecap: round;
    }
    .fill {
      fill: none;
      stroke: var(--color-action-primary, #8e7bff);
      stroke-linecap: round;
    }
    .pointer {
      position: absolute;
      left: 50%;
      top: 50%;
      width: 2px;
      height: 38%;
      margin-left: -1px;
      border-radius: 2px;
      background: var(--control-indicator, #6b6760);
      transform-origin: 50% 100%;
    }
  `;

  private _dragging = false;
  private _startY = 0;
  private _startVal = 0;

  private get _ratio() {
    const r = (this.value - this.min) / (this.max - this.min);
    return Math.min(1, Math.max(0, isFinite(r) ? r : 0));
  }

  private _arc(ratio: number) {
    // describe an SVG arc on a 100x100 viewBox, radius 42, centre 50,50
    const a0 = (START - 90) * (Math.PI / 180);
    const a1 = (START + SWEEP * ratio - 90) * (Math.PI / 180);
    const r = 42;
    const x0 = 50 + r * Math.cos(a0);
    const y0 = 50 + r * Math.sin(a0);
    const x1 = 50 + r * Math.cos(a1);
    const y1 = 50 + r * Math.sin(a1);
    const large = SWEEP * ratio > 180 ? 1 : 0;
    return `M ${x0} ${y0} A ${r} ${r} 0 ${large} 1 ${x1} ${y1}`;
  }

  private _set(v: number, change = false) {
    const clamped = Math.min(this.max, Math.max(this.min, v));
    if (clamped === this.value && !change) return;
    this.value = clamped;
    this.dispatchEvent(
      new CustomEvent(change ? 'hy-change' : 'hy-input', {
        detail: { value: this.value },
        bubbles: true,
        composed: true,
      })
    );
  }

  private _onDown(e: PointerEvent) {
    if (this.disabled) return;
    this._dragging = true;
    this._startY = e.clientY;
    this._startVal = this.value;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  }

  private _onMove(e: PointerEvent) {
    if (!this._dragging) return;
    const dy = this._startY - e.clientY;
    const range = this.max - this.min;
    this._set(this._startVal + (dy / 160) * range);
  }

  private _onUp(e: PointerEvent) {
    if (!this._dragging) return;
    this._dragging = false;
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    this._set(this.value, true);
  }

  private _onKey(e: KeyboardEvent) {
    const step = (this.max - this.min) / 100;
    if (e.key === 'ArrowUp' || e.key === 'ArrowRight') this._set(this.value + step * 5, true);
    else if (e.key === 'ArrowDown' || e.key === 'ArrowLeft') this._set(this.value - step * 5, true);
    else return;
    e.preventDefault();
  }

  render() {
    const ratio = this._ratio;
    const angle = START + SWEEP * ratio;
    return html`
      <div
        class="dial"
        part="dial"
        style="width:${this.size}px;height:${this.size}px"
        role="slider"
        tabindex=${this.disabled ? -1 : 0}
        aria-valuemin=${this.min}
        aria-valuemax=${this.max}
        aria-valuenow=${Math.round(this.value)}
        @pointerdown=${this._onDown}
        @pointermove=${this._onMove}
        @pointerup=${this._onUp}
        @pointercancel=${this._onUp}
        @keydown=${this._onKey}
      >
        <svg viewBox="0 0 100 100">
          <path class="track" d=${this._arc(1)} stroke-width="2.5"></path>
          <path class="fill" d=${this._arc(ratio)} stroke-width="2.5"></path>
        </svg>
        <span class="pointer" style="transform:translateY(-100%) rotate(${angle}deg)"></span>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-knob': HyKnob;
  }
}
