import { html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX } from '../../kit/kit-runtime.js';

/** Surface-button variants, lifted verbatim from the kit's Surface Buttons section (lines 792-802). */
export type HyKeyVariant = 'square' | 'oval' | 'dot' | 'press';

/**
 * A surface button — the F-key family, extracted verbatim from the Tactile Kit.
 *
 * Four faithful variants:
 *  - `square` — the transport `.fkey.fsq` key; momentary flash on click (kit line 838)
 *  - `oval`   — the `.fkey.foval` bank key; latches on/off (kit line 839)
 *  - `dot`    — the `.fkey.fsq.fdot` key with an `.fled`; latches on (kit line 840)
 *  - `press`  — the `.fkey.fpress` key; latches on/off (kit line 841)
 *
 * @element hy-key
 * @slot - Icon or short label (square keys).
 * @fires hy-change - `detail.value` (the `on` state) on click.
 */
@customElement('hy-key')
export class HyKey extends KitElement {
  @property() variant: HyKeyVariant = 'square';
  @property({ type: Boolean, reflect: true }) on = false;

  static styles = KitElement.kitStyles;

  /** `.fsq` (kit line 838): flash `on` for 150ms, click sound + haptic. */
  private _clickSquare = () => {
    this.on = true;
    SFX.click();
    HX.click();
    this._emit();
    setTimeout(() => {
      this.on = false;
    }, 150);
  };

  /** `.foval` (line 839) / `.fpress` (line 841): toggle `on`, click sound + haptic. */
  private _clickToggle = () => {
    this.on = !this.on;
    SFX.click();
    HX.click();
    this._emit();
  };

  /** `.fdot` (line 840): radio-exclusive in a pair; standalone here → set `on`. */
  private _clickDot = () => {
    this.on = true;
    SFX.click();
    HX.click();
    this._emit();
  };

  private _emit() {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { value: this.on }, bubbles: true, composed: true })
    );
  }

  render() {
    const onCls = this.on ? ' on' : '';
    switch (this.variant) {
      case 'oval':
        return html`<button class="fkey foval${onCls}" @click=${this._clickToggle}></button>`;
      case 'dot':
        return html`<button class="fkey fsq fdot${onCls}" @click=${this._clickDot}>
          <span class="fled"></span>
        </button>`;
      case 'press':
        return html`<button class="fkey fpress${onCls}" @click=${this._clickToggle}></button>`;
      case 'square':
      default:
        return html`<button class="fkey fsq${onCls}" @click=${this._clickSquare}>
          <slot>${nothing}</slot>
        </button>`;
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-key': HyKey;
  }
}
