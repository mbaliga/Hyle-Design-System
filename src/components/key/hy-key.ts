import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A physical surface button — the F-key / pad family. `momentary` fires while
 * held; `toggle` latches. An optional LED lights with the accent when active.
 *
 * @element hy-key
 * @slot - Icon or short label.
 * @fires hy-press  - momentary mode: `detail.down` true on press, false on release.
 * @fires hy-change - toggle mode: `detail.pressed`.
 */
@customElement('hy-key')
export class HyKey extends LitElement {
  @property({ reflect: true }) shape: 'square' | 'oval' = 'square';
  @property({ reflect: true }) mode: 'momentary' | 'toggle' = 'momentary';
  @property({ type: Boolean, reflect: true }) pressed = false;
  @property({ type: Boolean, reflect: true }) led = false;
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-block;
    }
    button {
      position: relative;
      width: 56px;
      height: 48px;
      border: 0;
      border-radius: var(--radius-md, 8px);
      cursor: pointer;
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
      background: linear-gradient(180deg, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a));
      box-shadow:
        inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)),
        inset 0 -2px 4px var(--control-groove, #050506),
        0 2px 5px rgba(0, 0, 0, 0.5);
      display: grid;
      place-items: center;
      transition: transform var(--duration-instant, 120ms) var(--easing-standard, ease),
        box-shadow var(--duration-instant, 120ms), color var(--duration-instant, 120ms);
    }
    :host([shape='oval']) button {
      border-radius: 999px;
      width: 64px;
    }
    button:active,
    :host([pressed]) button {
      transform: translateY(1px);
      box-shadow: inset 0 2px 5px var(--control-groove, #050506);
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
    }
    :host([pressed]) button {
      color: var(--color-action-primary, #8e7bff);
    }
    button:focus-visible {
      outline: 2px solid var(--color-border-focus, #8e7bff);
      outline-offset: 2px;
    }
    :host([disabled]) button {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .led {
      position: absolute;
      top: 6px;
      right: 6px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--control-groove, #050506);
      box-shadow: inset 0 0 1px rgba(0, 0, 0, 0.9);
    }
    :host([pressed]) .led,
    .led.on {
      background: var(--color-action-primary, #8e7bff);
      box-shadow: 0 0 6px var(--color-action-primary, #8e7bff);
    }
    ::slotted(svg) {
      width: 22px;
      height: 22px;
      fill: currentColor;
    }
  `;

  private _down() {
    if (this.disabled || this.mode !== 'momentary') return;
    this.pressed = true;
    this.dispatchEvent(new CustomEvent('hy-press', { detail: { down: true }, bubbles: true, composed: true }));
  }
  private _up() {
    if (this.disabled || this.mode !== 'momentary' || !this.pressed) return;
    this.pressed = false;
    this.dispatchEvent(new CustomEvent('hy-press', { detail: { down: false }, bubbles: true, composed: true }));
  }
  private _click() {
    if (this.disabled || this.mode !== 'toggle') return;
    this.pressed = !this.pressed;
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { pressed: this.pressed }, bubbles: true, composed: true })
    );
  }

  render() {
    return html`
      <button
        part="key"
        aria-pressed=${this.mode === 'toggle' ? (this.pressed ? 'true' : 'false') : nothing}
        ?disabled=${this.disabled}
        @pointerdown=${this._down}
        @pointerup=${this._up}
        @pointerleave=${this._up}
        @click=${this._click}
      >
        ${this.led ? html`<span class="led ${this.pressed ? 'on' : ''}"></span>` : nothing}
        <slot></slot>
      </button>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-key': HyKey;
  }
}
