import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ICONS } from './icons.js';

/**
 * A sharp-edged icon. Inherits `currentColor`, so it follows the surrounding
 * text colour (and the accent when placed on accent surfaces).
 *
 * @element hy-icon
 * @attr name - One of the registered icon names (see ICON_NAMES).
 * @attr size - Pixel size (square). Defaults to 24.
 * @attr label - Accessible label; when omitted the icon is decorative.
 */
@customElement('hy-icon')
export class HyIcon extends LitElement {
  @property() name = '';
  @property({ type: Number }) size = 24;
  @property() label = '';

  static styles = css`
    :host {
      display: inline-flex;
      color: inherit;
      vertical-align: middle;
    }
    svg {
      display: block;
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: butt;
      stroke-linejoin: miter;
    }
  `;

  render() {
    const glyph = ICONS[this.name];
    const labelled = this.label.length > 0;
    return html`
      <svg
        viewBox="0 0 24 24"
        width=${this.size}
        height=${this.size}
        role=${labelled ? 'img' : 'presentation'}
        aria-label=${labelled ? this.label : nothing}
        aria-hidden=${labelled ? nothing : 'true'}
      >
        ${glyph ?? nothing}
      </svg>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-icon': HyIcon;
  }
}
