import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A physical switch — the knob slides in its groove; the slot lights with the
 * accent when on. State is shown by position and light, not the words ON/OFF.
 *
 * @element hy-toggle
 * @fires hy-change - `detail.on` boolean when toggled.
 */
@customElement('hy-toggle')
export class HyToggle extends LitElement {
  @property({ type: Boolean, reflect: true }) on = false;
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-flex;
    }
    .tgl {
      position: relative;
      width: 52px;
      height: 30px;
      border-radius: 15px;
      cursor: pointer;
      background: var(--control-groove, #050506);
      box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.9), inset 0 0 0 1px var(--control-rim-soft, rgba(255, 255, 255, 0.09));
      outline: none;
      transition: background var(--duration-instant, 120ms) var(--easing-standard, ease);
    }
    .fill {
      position: absolute;
      inset: 0;
      border-radius: inherit;
      background: var(--color-action-primary, #8e7bff);
      opacity: 0;
      transition: opacity var(--duration-instant, 120ms) var(--easing-standard, ease);
    }
    :host([on]) .fill {
      opacity: 0.5;
    }
    .knob {
      position: absolute;
      top: 3px;
      left: 3px;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: linear-gradient(180deg, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a));
      box-shadow:
        inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)),
        0 2px 3px rgba(0, 0, 0, 0.6);
      transition: transform var(--duration-instant, 120ms) var(--easing-standard, cubic-bezier(0.4, 0, 0.2, 1));
    }
    :host([on]) .knob {
      transform: translateX(22px);
    }
    .tgl:focus-visible {
      box-shadow: 0 0 0 2px var(--color-border-focus, #8e7bff);
    }
    :host([disabled]) .tgl {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `;

  private _toggle() {
    if (this.disabled) return;
    this.on = !this.on;
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { on: this.on }, bubbles: true, composed: true })
    );
  }

  private _onKey(e: KeyboardEvent) {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      this._toggle();
    }
  }

  render() {
    return html`
      <div
        class="tgl"
        part="track"
        role="switch"
        aria-checked=${this.on ? 'true' : 'false'}
        tabindex=${this.disabled ? -1 : 0}
        @click=${this._toggle}
        @keydown=${this._onKey}
      >
        <div class="fill"></div>
        <div class="knob" part="knob"></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-toggle': HyToggle;
  }
}
