import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HyInputSize = 'sm' | 'md' | 'lg';

/**
 * A labelled text field.
 *
 * @element hy-input
 * @fires hy-input - Dispatched as the user types; `detail.value` holds the value.
 * @fires hy-change - Dispatched on commit (blur / Enter); `detail.value` holds the value.
 */
@customElement('hy-input')
export class HyInput extends LitElement {
  /** Visible field label. */
  @property() label = '';

  /** Placeholder text. */
  @property() placeholder = '';

  /** Current value. */
  @property() value = '';

  /** Native input type. */
  @property() type = 'text';

  /** Control size scale. */
  @property({ reflect: true }) size: HyInputSize = 'md';

  /** Helper text shown beneath the field. */
  @property({ attribute: 'helper-text' }) helperText = '';

  /** Error message; when set, the field renders in an invalid state. */
  @property() error = '';

  /** Disables the field. */
  @property({ type: Boolean, reflect: true }) disabled = false;

  /** Marks the field as required. */
  @property({ type: Boolean, reflect: true }) required = false;

  static styles = css`
    :host {
      display: block;
      font-family: var(--font-family-sans);
    }
    .label {
      display: block;
      margin-bottom: var(--spacing-1, 4px);
      font-size: var(--font-size-sm, 14px);
      font-weight: var(--font-weight-medium, 500);
      color: var(--color-text-primary, #161a23);
    }
    .required {
      color: var(--color-feedback-danger, #dc3a3a);
      margin-left: 2px;
    }
    input {
      width: 100%;
      box-sizing: border-box;
      border: var(--size-border-thin, 1px) solid var(--color-border-strong, #b9c0cc);
      border-radius: var(--radius-md, 8px);
      background: var(--color-background-surface, #fff);
      color: var(--color-text-primary, #161a23);
      font-family: inherit;
      transition: border-color 120ms ease, box-shadow 120ms ease;
    }
    input::placeholder {
      color: var(--color-text-faint, rgba(236, 232, 228, 0.18));
    }
    input:focus {
      outline: none;
      border-color: var(--color-border-focus, #3a51e8);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-border-focus, #3a51e8) 25%, transparent);
    }
    input:disabled {
      background: var(--color-border-hairline, rgba(255, 255, 255, 0.06));
      cursor: not-allowed;
      opacity: 0.7;
    }
    :host([size='sm']) input {
      height: var(--size-control-sm, 32px);
      padding: 0 var(--spacing-3, 12px);
      font-size: var(--font-size-sm, 14px);
    }
    :host([size='md']) input {
      height: var(--size-control-md, 40px);
      padding: 0 var(--spacing-3, 12px);
      font-size: var(--font-size-md, 16px);
    }
    :host([size='lg']) input {
      height: var(--size-control-lg, 48px);
      padding: 0 var(--spacing-4, 16px);
      font-size: var(--font-size-md, 16px);
    }
    :host([data-invalid]) input,
    .invalid input {
      border-color: var(--color-feedback-danger, #dc3a3a);
    }
    .message {
      margin-top: var(--spacing-1, 4px);
      font-size: var(--font-size-xs, 12px);
      color: var(--color-text-secondary, #5f6b80);
    }
    .message.error {
      color: var(--color-feedback-danger, #dc3a3a);
    }
  `;

  private _onInput(event: Event) {
    this.value = (event.target as HTMLInputElement).value;
    this.dispatchEvent(
      new CustomEvent('hy-input', { detail: { value: this.value }, bubbles: true, composed: true })
    );
  }

  private _onChange() {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { value: this.value }, bubbles: true, composed: true })
    );
  }

  render() {
    const invalid = this.error.length > 0;
    return html`
      <div class=${invalid ? 'invalid' : ''}>
        ${this.label
          ? html`<label class="label"
              >${this.label}${this.required
                ? html`<span class="required">*</span>`
                : nothing}</label
            >`
          : nothing}
        <input
          part="input"
          .type=${this.type}
          .value=${this.value}
          placeholder=${this.placeholder}
          ?disabled=${this.disabled}
          ?required=${this.required}
          aria-invalid=${invalid ? 'true' : 'false'}
          @input=${this._onInput}
          @change=${this._onChange}
        />
        ${invalid
          ? html`<div class="message error">${this.error}</div>`
          : this.helperText
            ? html`<div class="message">${this.helperText}</div>`
            : nothing}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-input': HyInput;
  }
}
