import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A pill-shaped toggle — the primary on-glass control in Hyle.
 *
 * State is shown by the material, not a word: a pressed chip inverts to solid
 * ink, an idle chip is a quiet hairline outline.
 *
 * @element hy-chip
 * @slot - Chip label.
 * @fires hy-toggle - `detail.pressed` and `detail.value` on activation.
 * @attr pressed - Reflects the selected state (aria-pressed).
 * @attr value - Optional value reported in the toggle event.
 */
@customElement('hy-chip')
export class HyChip extends LitElement {
  @property({ type: Boolean, reflect: true }) pressed = false;
  @property() value = '';
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-block;
    }
    button {
      font-family: var(--font-family-sans);
      font-size: var(--font-size-label, 11px);
      letter-spacing: 0.06em;
      padding: 7px 11px;
      border-radius: var(--radius-full, 9999px);
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      background: transparent;
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
      cursor: pointer;
      user-select: none;
      -webkit-user-select: none;
      transition:
        color var(--duration-instant, 120ms) var(--easing-standard, ease),
        border-color var(--duration-instant, 120ms) var(--easing-standard, ease),
        background var(--duration-instant, 120ms) var(--easing-standard, ease);
    }
    :host([pressed]) button {
      color: var(--color-text-inverse, #0a0809);
      background: var(--color-background-inverse, #ece8e4);
      border-color: var(--color-background-inverse, #ece8e4);
    }
    button:focus-visible {
      outline: 2px solid var(--color-border-focus, #8e7bff);
      outline-offset: 2px;
    }
    :host([disabled]) button {
      opacity: 0.4;
      cursor: not-allowed;
    }
  `;

  private _onClick() {
    if (this.disabled) return;
    this.pressed = !this.pressed;
    this.dispatchEvent(
      new CustomEvent('hy-toggle', {
        detail: { pressed: this.pressed, value: this.value },
        bubbles: true,
        composed: true,
      })
    );
  }

  render() {
    return html`
      <button
        part="chip"
        role="button"
        aria-pressed=${this.pressed ? 'true' : 'false'}
        ?disabled=${this.disabled}
        @click=${this._onClick}
      >
        <slot></slot>
      </button>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-chip': HyChip;
  }
}
