import { css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/**
 * A speaker grille — a perforated panel surface, lifted verbatim from the kit's
 * Surfaces section (line 808: `<div class="grille" style="width:100%;height:64px"></div>`).
 * The kit's `.grille` class carries the whole perforated look; the element fills
 * the host width and takes its height from the `height` property.
 *
 * @element hy-grille
 */
@customElement('hy-grille')
export class HyGrille extends KitElement {
  @property() height = '64px';

  static styles = [
    KitElement.kitStyles,
    css`
      :host {
        display: block;
      }
    `,
  ];

  render() {
    return html`<div class="grille" part="grille" style="width:100%;height:${this.height}"></div>`;
  }
}

/**
 * A panel jack / socket — lifted verbatim from the kit's Surfaces section
 * (line 811: `<div class="jack"></div>`). The kit's `.jack` class carries the
 * whole machined-socket look, including its size.
 *
 * @element hy-jack
 */
@customElement('hy-jack')
export class HyJack extends KitElement {
  static styles = [
    KitElement.kitStyles,
    css`
      :host {
        display: inline-block;
      }
    `,
  ];

  render() {
    return html`<div class="jack" part="jack"></div>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-grille': HyGrille;
    'hy-jack': HyJack;
  }
}
