import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HySliderVariant = 'channel' | 'hairline' | 'minimal';

/**
 * A horizontal slider. `channel` is a filled track, `hairline` is a thin rail
 * (good for pan, with a centre origin), `minimal` is a rail with a dimple knob.
 *
 * @element hy-slider
 * @fires hy-input  - `detail.value` as it moves.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-slider')
export class HySlider extends LitElement {
  @property({ type: Number }) min = 0;
  @property({ type: Number }) max = 100;
  @property({ type: Number }) value = 50;
  @property({ reflect: true }) variant: HySliderVariant = 'channel';
  /** `start` fills from the left; `center` fills from the middle (pan). */
  @property() origin: 'start' | 'center' = 'start';
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: block;
      touch-action: none;
    }
    .track {
      position: relative;
      height: 28px;
      display: flex;
      align-items: center;
      cursor: pointer;
      outline: none;
    }
    :host([disabled]) .track {
      cursor: not-allowed;
      opacity: 0.5;
    }
    .rail {
      position: absolute;
      left: 0;
      right: 0;
      height: 4px;
      border-radius: 2px;
      background: var(--control-groove, #050506);
      box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.9);
    }
    :host([variant='hairline']) .rail {
      height: 2px;
    }
    .fill {
      position: absolute;
      height: inherit;
      border-radius: 2px;
      background: var(--color-action-primary, #8e7bff);
      opacity: 0.85;
    }
    .thumb {
      position: absolute;
      width: 16px;
      height: 16px;
      margin-left: -8px;
      border-radius: 50%;
      background: linear-gradient(180deg, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a));
      box-shadow: inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)), 0 2px 4px rgba(0, 0, 0, 0.6);
    }
    :host([variant='minimal']) .thumb::after {
      content: '';
      position: absolute;
      inset: 5px;
      border-radius: 50%;
      background: var(--control-groove, #050506);
    }
    .track:focus-visible .thumb {
      box-shadow: 0 0 0 2px var(--color-border-focus, #8e7bff);
    }
  `;

  private _dragging = false;

  private get _ratio() {
    const r = (this.value - this.min) / (this.max - this.min);
    return Math.min(1, Math.max(0, isFinite(r) ? r : 0));
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

  private _fromClientX(clientX: number, el: HTMLElement) {
    const rect = el.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    this._set(this.min + ratio * (this.max - this.min));
  }

  private _onDown(e: PointerEvent) {
    if (this.disabled) return;
    this._dragging = true;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    this._fromClientX(e.clientX, e.currentTarget as HTMLElement);
  }
  private _onMove(e: PointerEvent) {
    if (this._dragging) this._fromClientX(e.clientX, e.currentTarget as HTMLElement);
  }
  private _onUp(e: PointerEvent) {
    if (!this._dragging) return;
    this._dragging = false;
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    this._set(this.value, true);
  }
  private _onKey(e: KeyboardEvent) {
    const step = (this.max - this.min) / 100;
    if (e.key === 'ArrowRight' || e.key === 'ArrowUp') this._set(this.value + step * 2, true);
    else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') this._set(this.value - step * 2, true);
    else return;
    e.preventDefault();
  }

  render() {
    const ratio = this._ratio;
    const pct = ratio * 100;
    // fill geometry depends on origin
    const fill =
      this.origin === 'center'
        ? pct >= 50
          ? `left:50%; width:${pct - 50}%`
          : `left:${pct}%; width:${50 - pct}%`
        : `left:0; width:${pct}%`;
    return html`
      <div
        class="track"
        part="track"
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
        <div class="rail"></div>
        ${this.variant === 'hairline' ? '' : html`<div class="fill" style=${fill}></div>`}
        <div class="thumb" part="thumb" style="left:${pct}%"></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-slider': HySlider;
  }
}
