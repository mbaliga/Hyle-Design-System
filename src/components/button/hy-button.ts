import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HyButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type HyButtonSize = 'sm' | 'md' | 'lg';

/**
 * A token-driven button.
 *
 * @element hy-button
 * @slot - Button label content.
 * @fires hy-click - Dispatched on activation (unless disabled).
 */
@customElement('hy-button')
export class HyButton extends LitElement {
  /** Visual style of the button. */
  @property({ reflect: true }) variant: HyButtonVariant = 'primary';

  /** Control height / padding scale. */
  @property({ reflect: true }) size: HyButtonSize = 'md';

  /** Disables interaction. */
  @property({ type: Boolean, reflect: true }) disabled = false;

  /** Stretch to fill the container width. */
  @property({ type: Boolean, reflect: true, attribute: 'full-width' }) fullWidth = false;

  static styles = css`
    :host {
      display: inline-block;
    }
    :host([full-width]) {
      display: block;
    }
    button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-2, 8px);
      width: 100%;
      border: var(--size-border-thin, 1px) solid transparent;
      border-radius: var(--radius-md, 8px);
      font-family: var(--font-family-sans);
      font-weight: var(--font-weight-semibold, 600);
      line-height: 1;
      cursor: pointer;
      transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
    }
    button:focus-visible {
      outline: var(--size-border-thick, 2px) solid var(--color-border-focus, #8e7bff);
      outline-offset: 2px;
    }
    button:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }

    /* Sizes */
    :host([size='sm']) button {
      height: var(--size-control-sm, 32px);
      padding: 0 var(--spacing-3, 12px);
      font-size: var(--font-size-sm, 14px);
    }
    :host([size='md']) button {
      height: var(--size-control-md, 40px);
      padding: 0 var(--spacing-4, 16px);
      font-size: var(--font-size-md, 16px);
    }
    :host([size='lg']) button {
      height: var(--size-control-lg, 48px);
      padding: 0 var(--spacing-6, 24px);
      font-size: var(--font-size-lg, 18px);
    }

    /* Variants */
    :host([variant='primary']) button {
      background: var(--color-action-primary, #3a51e8);
      color: var(--color-action-on-primary, #fff);
    }
    :host([variant='primary']) button:hover:not(:disabled) {
      background: var(--color-action-primary-hover, #2c3ccb);
    }
    :host([variant='primary']) button:active:not(:disabled) {
      background: var(--color-action-primary-active, #2531a3);
    }

    :host([variant='secondary']) button {
      background: transparent;
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
      border-color: var(--color-border-strong, rgba(255, 255, 255, 0.14));
    }
    :host([variant='secondary']) button:hover:not(:disabled) {
      background: var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }

    :host([variant='ghost']) button {
      background: transparent;
      color: var(--color-text-accent, #8e7bff);
    }
    :host([variant='ghost']) button:hover:not(:disabled) {
      background: var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }

    :host([variant='danger']) button {
      background: var(--color-feedback-danger, #e5564b);
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
    }
    :host([variant='danger']) button:hover:not(:disabled) {
      filter: brightness(0.93);
    }
  `;

  private _onClick(event: Event) {
    if (this.disabled) {
      event.stopImmediatePropagation();
      event.preventDefault();
      return;
    }
    this.dispatchEvent(new CustomEvent('hy-click', { bubbles: true, composed: true }));
  }

  render() {
    return html`
      <button ?disabled=${this.disabled} part="button" @click=${this._onClick}>
        <slot></slot>
      </button>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-button': HyButton;
  }
}
