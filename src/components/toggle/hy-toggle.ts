import { html, type PropertyValues } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX } from '../../kit/kit-runtime.js';

/** Toggle variants, lifted verbatim from the kit's Toggles section (lines 736-744). */
export type HyToggleVariant = 'standard' | 'flip' | 'gain' | 'smooth' | 'big' | 'square';

/**
 * A tactile switch — extracted verbatim from the Tactile Kit. State is SHOWN by
 * the knob's position and the slot's light, never SAID. Each flip clicks or
 * thunks (audio + haptic) exactly as it does in the kit.
 *
 * Six faithful variants:
 *  - `standard` — the sliding knob in a lit slot (kit `.tgl`, line 736/737;
 *    click thunks, JS line 995)
 *  - `flip`     — the bare flip switch (kit `.tgl-f`, line 738; click clicks,
 *    JS line 996)
 *  - `gain`     — the 3-position gain button O/I/II (kit `.gain`, line 739;
 *    click clicks, JS line 997)
 *  - `smooth`   — the glowing smooth toggle (kit `.stog`, line 740; click thunks,
 *    JS line 998)
 *  - `big`      — the big knob toggle (kit `.btog`, line 743; click thunks,
 *    JS line 842)
 *  - `square`   — the square glow toggle (kit `.sqtog`, line 744; click thunks,
 *    JS line 842)
 *
 * @element hy-toggle
 * @fires hy-change - `detail.value` boolean when toggled (`pos` 0-2 for `gain`).
 */
@customElement('hy-toggle')
export class HyToggle extends KitElement {
  @property() variant: HyToggleVariant = 'standard';
  @property({ type: Boolean, reflect: true }) on = false;
  @property({ type: Number, reflect: true }) pos = 0;

  @query('.gbtn') private _gbtn?: HTMLElement;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    if (this.variant === 'gain') this._setPos(this.pos);
  }

  /** Verbatim from the kit (line 997): move the gain button to a detent. */
  private _setPos(p: number) {
    const xs = [0, 26, 52];
    this.pos = (p + 3) % 3;
    if (this._gbtn) this._gbtn.style.transform = `translateX(${xs[this.pos]}px)`;
  }

  private _click = () => {
    if (this.variant === 'gain') {
      // Kit line 997: cycle 3 positions, click sound + haptic.
      this._setPos(this.pos + 1);
      SFX.click();
      HX.click();
      this._emit(this.pos);
      return;
    }
    this.on = !this.on;
    if (this.variant === 'flip') {
      // Kit line 996: click sound + haptic.
      SFX.click();
      HX.click();
    } else {
      // Kit lines 995 / 998 / 842: thunk sound + toggle haptic.
      SFX.thunk();
      HX.toggle();
    }
    this._emit(this.on);
  };

  private _emit(value: boolean | number) {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { value }, bubbles: true, composed: true })
    );
  }

  render() {
    const on = this.on ? ' on' : '';
    switch (this.variant) {
      case 'flip':
        // Kit line 738.
        return html`<div class="tgl-f${on}" @click=${this._click}><div class="knob"></div></div>`;
      case 'gain':
        // Kit line 739.
        return html`<div class="gain" data-pos=${this.pos} @click=${this._click}>
          <div class="gslot"></div>
          <div class="gbtn"><div class="gpin"></div></div>
          <div class="gmarks"><span>O</span><span>I</span><span>II</span></div>
        </div>`;
      case 'smooth':
        // Kit line 740.
        return html`<div class="stog${on}" @click=${this._click}>
          <div class="glow"></div>
          <div class="sk"></div>
        </div>`;
      case 'big':
        // Kit line 743.
        return html`<div class="btog${on}" @click=${this._click}>
          <div class="bgroove"></div>
          <div class="bfill"></div>
          <div class="bknob"></div>
        </div>`;
      case 'square':
        // Kit line 744.
        return html`<div class="sqtog${on}" @click=${this._click}>
          <div class="sqglow"></div>
          <div class="sqknob"></div>
        </div>`;
      case 'standard':
      default:
        // Kit line 736/737.
        return html`<div class="tgl${on}" @click=${this._click}>
          <div class="track"></div>
          <div class="fill"></div>
          <div class="knob"></div>
          <span class="on-lbl">ON</span>
          <span class="off-lbl">OFF</span>
        </div>`;
    }
  }

  protected updated(changed: PropertyValues) {
    // Keep the gain button positioned when `pos` is set programmatically.
    if (this.variant === 'gain' && changed.has('pos')) this._setPos(this.pos);
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-toggle': HyToggle;
  }
}
