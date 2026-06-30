import { LitElement, css, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

export type HyCardElevation = 'flat' | 'sm' | 'md' | 'lg';

/**
 * A surface container for grouping related content.
 *
 * @element hy-card
 * @slot header - Optional header region.
 * @slot - Default body content.
 * @slot footer - Optional footer region.
 */
@customElement('hy-card')
export class HyCard extends LitElement {
  /** Shadow depth. `flat` uses a border instead of a shadow. */
  @property({ reflect: true }) elevation: HyCardElevation = 'sm';

  @state() private _hasHeader = false;
  @state() private _hasFooter = false;

  static styles = css`
    :host {
      display: block;
    }
    .card {
      background: var(--color-background-surface, #fff);
      border-radius: var(--radius-lg, 12px);
      overflow: hidden;
    }
    :host([elevation='flat']) .card {
      border: var(--size-border-thin, 1px) solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }
    :host([elevation='sm']) .card {
      box-shadow: var(--shadow-sm, 0 1px 2px rgba(16, 24, 40, 0.06));
    }
    :host([elevation='md']) .card {
      box-shadow: var(--shadow-md, 0 4px 8px -2px rgba(16, 24, 40, 0.1));
    }
    :host([elevation='lg']) .card {
      box-shadow: var(--shadow-lg, 0 12px 24px -6px rgba(16, 24, 40, 0.14));
    }
    .header,
    .body,
    .footer {
      padding: var(--spacing-5, 20px);
    }
    .header {
      border-bottom: var(--size-border-thin, 1px) solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      font-weight: var(--font-weight-semibold, 600);
      color: var(--color-text-primary, #161a23);
    }
    .footer {
      border-top: var(--size-border-thin, 1px) solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
    }
    .body {
      color: var(--color-text-secondary, #5f6b80);
      font-family: var(--font-family-sans);
      line-height: var(--font-line-height-normal, 1.5);
    }
    .hidden {
      display: none;
    }
    ::slotted([slot='header']) {
      margin: 0;
    }
  `;

  private _onHeaderSlotChange(event: Event) {
    this._hasHeader = (event.target as HTMLSlotElement).assignedNodes().length > 0;
  }

  private _onFooterSlotChange(event: Event) {
    this._hasFooter = (event.target as HTMLSlotElement).assignedNodes().length > 0;
  }

  render() {
    return html`
      <div class="card" part="card">
        <div class="header ${this._hasHeader ? '' : 'hidden'}">
          <slot name="header" @slotchange=${this._onHeaderSlotChange}></slot>
        </div>
        <div class="body"><slot></slot></div>
        <div class="footer ${this._hasFooter ? '' : 'hidden'}">
          <slot name="footer" @slotchange=${this._onFooterSlotChange}></slot>
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-card': HyCard;
  }
}
