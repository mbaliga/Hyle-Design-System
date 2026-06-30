import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A speaker grille — a perforated panel surface. Decorative; reads as a real
 * acoustic surface (the "as found" honesty of brutalism).
 *
 * @element hy-grille
 */
@customElement('hy-grille')
export class HyGrille extends LitElement {
  @property({ type: Number }) height = 64;

  static styles = css`
    :host {
      display: block;
    }
    .grille {
      width: 100%;
      border-radius: var(--radius-md, 8px);
      background-color: var(--control-surface, #16161a);
      background-image: radial-gradient(var(--control-groove, #050506) 1.1px, transparent 1.4px);
      background-size: 7px 7px;
      box-shadow: inset 0 1px 0 var(--control-rim-soft, rgba(255, 255, 255, 0.09)), inset 0 0 18px rgba(0, 0, 0, 0.5);
    }
  `;

  render() {
    return html`<div class="grille" part="grille" style="height:${this.height}px"></div>`;
  }
}

/**
 * A panel jack / socket. Concentric machined ring around a dark bore.
 *
 * @element hy-jack
 */
@customElement('hy-jack')
export class HyJack extends LitElement {
  @property({ type: Number }) size = 36;

  static styles = css`
    :host {
      display: inline-block;
    }
    .jack {
      border-radius: 50%;
      background: radial-gradient(
        circle at 50% 42%,
        #000 0 22%,
        var(--control-groove, #050506) 23% 40%,
        var(--control-surface-high, #2c2c34) 41% 70%,
        var(--control-surface, #16161a) 71% 100%
      );
      box-shadow: inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)), 0 1px 3px rgba(0, 0, 0, 0.6);
    }
  `;

  render() {
    return html`<div class="jack" part="jack" style="width:${this.size}px;height:${this.size}px"></div>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-grille': HyGrille;
    'hy-jack': HyJack;
  }
}
