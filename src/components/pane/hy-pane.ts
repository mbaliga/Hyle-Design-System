import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HyPaneDock = 'floating' | 'bottom';

/**
 * A frosted-glass surface — the readable layer that floats over the Field.
 *
 * Glass means legibility (ethos: "materials are assigned by meaning"). The pane
 * is calm and still; it never animates except to enter or leave.
 *
 * @element hy-pane
 * @slot - Body content.
 * @slot actions - Optional controls in the title row (icons, close button).
 * @attr heading - Uppercase tracked title shown in the head row.
 * @attr dock - `floating` (default) or `bottom` (a bottom sheet, like Form-World).
 * @attr hidden-pane - When set, the pane slides out of view.
 */
@customElement('hy-pane')
export class HyPane extends LitElement {
  @property() heading = '';
  @property({ reflect: true }) dock: HyPaneDock = 'floating';
  @property({ type: Boolean, reflect: true, attribute: 'hidden-pane' }) hiddenPane = false;

  static styles = css`
    :host {
      display: block;
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
      font-family: var(--font-family-sans);
    }
    .pane {
      background: var(--color-background-glass, rgba(10, 8, 9, 0.52));
      -webkit-backdrop-filter: blur(18px);
      backdrop-filter: blur(18px);
      border: 1px solid var(--color-border-hairline, rgba(255, 255, 255, 0.08));
      border-radius: var(--radius-lg, 12px);
      padding: 14px;
      transition:
        transform var(--duration-pane, 420ms) var(--easing-pane, cubic-bezier(0.22, 0.61, 0.36, 1)),
        opacity var(--duration-pane, 420ms);
    }
    :host([dock='bottom']) .pane {
      border-radius: 0;
      border-left: 0;
      border-right: 0;
      border-bottom: 0;
      padding-bottom: calc(14px + env(safe-area-inset-bottom, 0px));
    }
    :host([hidden-pane]) .pane {
      opacity: 0;
    }
    :host([hidden-pane][dock='bottom']) .pane {
      transform: translateY(105%);
    }
    :host([hidden-pane][dock='floating']) .pane {
      transform: translateY(8px) scale(0.99);
      pointer-events: none;
    }
    .head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
    }
    .ttl {
      font-size: var(--font-size-label, 11px);
      letter-spacing: var(--font-tracking-title, 0.34em);
      text-transform: uppercase;
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
    }
    ::slotted([slot='actions']) {
      display: flex;
      gap: 8px;
    }
  `;

  render() {
    const showHead = this.heading.length > 0;
    return html`
      <div class="pane" part="pane">
        ${showHead
          ? html`<div class="head">
              <span class="ttl">${this.heading}</span>
              <slot name="actions"></slot>
            </div>`
          : nothing}
        <slot></slot>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-pane': HyPane;
  }
}
