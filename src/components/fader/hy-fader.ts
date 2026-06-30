import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A vertical channel fader — drag the cap (or ↑/↓) to set a level. An accent
 * fill rises from the floor of the slot.
 *
 * @element hy-fader
 * @fires hy-input - `detail.value` as it moves.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-fader')
export class HyFader extends LitElement {
  @property({ type: Number }) min = 0;
  @property({ type: Number }) max = 100;
  @property({ type: Number }) value = 60;
  @property({ type: Number }) height = 168;
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-flex;
      touch-action: none;
    }
    .body {
      position: relative;
      width: 36px;
      display: flex;
      justify-content: center;
      cursor: ns-resize;
      outline: none;
    }
    :host([disabled]) .body {
      cursor: not-allowed;
      opacity: 0.5;
    }
    .slot {
      position: absolute;
      top: 8px;
      bottom: 8px;
      width: 6px;
      border-radius: 3px;
      background: var(--control-groove, #050506);
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.9);
      overflow: hidden;
    }
    .fill {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      background: var(--color-action-primary, #8e7bff);
      opacity: 0.85;
    }
    .cap {
      position: absolute;
      width: 30px;
      height: 16px;
      margin-top: -8px;
      border-radius: 4px;
      background: linear-gradient(
        180deg,
        var(--control-surface-high, #2c2c34),
        var(--control-surface, #16161a)
      );
      box-shadow:
        inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)),
        0 2px 4px rgba(0, 0, 0, 0.6);
    }
    .cap::after {
      content: '';
      position: absolute;
      left: 4px;
      right: 4px;
      top: 50%;
      height: 1px;
      background: var(--control-groove, #050506);
    }
    .body:focus-visible .cap {
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

  private _fromClientY(clientY: number, el: HTMLElement) {
    const rect = el.getBoundingClientRect();
    const pad = 8 + 8; // top+bottom padding of the slot ends
    const usable = rect.height - pad;
    const y = clientY - (rect.top + 8);
    const ratio = 1 - Math.min(1, Math.max(0, y / usable));
    this._set(this.min + ratio * (this.max - this.min));
  }

  private _onDown(e: PointerEvent) {
    if (this.disabled) return;
    this._dragging = true;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    this._fromClientY(e.clientY, e.currentTarget as HTMLElement);
  }

  private _onMove(e: PointerEvent) {
    if (!this._dragging) return;
    this._fromClientY(e.clientY, e.currentTarget as HTMLElement);
  }

  private _onUp(e: PointerEvent) {
    if (!this._dragging) return;
    this._dragging = false;
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    this._set(this.value, true);
  }

  private _onKey(e: KeyboardEvent) {
    const step = (this.max - this.min) / 20;
    if (e.key === 'ArrowUp') this._set(this.value + step, true);
    else if (e.key === 'ArrowDown') this._set(this.value - step, true);
    else return;
    e.preventDefault();
  }

  render() {
    const ratio = this._ratio;
    const capTop = 8 + (1 - ratio) * (this.height - 16); // centre of the cap
    return html`
      <div
        class="body"
        part="body"
        style="height:${this.height}px"
        role="slider"
        tabindex=${this.disabled ? -1 : 0}
        aria-valuemin=${this.min}
        aria-valuemax=${this.max}
        aria-valuenow=${Math.round(this.value)}
        aria-orientation="vertical"
        @pointerdown=${this._onDown}
        @pointermove=${this._onMove}
        @pointerup=${this._onUp}
        @pointercancel=${this._onUp}
        @keydown=${this._onKey}
      >
        <div class="slot"><div class="fill" style="height:${ratio * 100}%"></div></div>
        <div class="cap" part="cap" style="top:${capTop}px"></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-fader': HyFader;
  }
}
