import { html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX } from '../../kit/kit-runtime.js';

/** Button variants, lifted verbatim from the kit's Buttons section (lines 765-771). */
export type HyCraterVariant = 'crater' | 'power';

/** Crater glyphs, lifted verbatim from the kit (lines 766-768). */
export type HyCraterIcon = 'record' | 'mix' | 'save';

/**
 * A physical button — extracted verbatim from the Tactile Kit's Buttons section.
 *
 * Two faithful variants:
 *  - `crater` — the sunken `.crater-cell` key with a record / mix / save glyph
 *    (kit lines 765-768). Record latches an `on` state.
 *  - `power`  — the raised `.mound` power key; click toggles the mound's `off`
 *    class (kit line 999).
 *
 * @element hy-crater
 * @slot - Custom crater glyph (overrides `icon`).
 * @fires hy-change - `detail.value` (the `on` state) on click.
 */
@customElement('hy-crater')
export class HyCrater extends KitElement {
  @property() variant: HyCraterVariant = 'crater';
  @property() icon: HyCraterIcon = 'record';
  @property({ type: Boolean, reflect: true }) on = false;

  static styles = KitElement.kitStyles;

  /** `.crater-cell` (lines 765-768): click sound + haptic; record latches `on`. */
  private _clickCrater = () => {
    if (this.icon === 'record') this.on = !this.on;
    SFX.click();
    HX.click();
    this._emit();
  };

  /** `.mound` (kit line 999): toggle the mound's `off` class, thunk + toggle haptic. */
  private _clickPower = () => {
    this.on = !this.on;
    SFX.thunk();
    HX.toggle();
    this._emit();
  };

  private _emit() {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { value: this.on }, bubbles: true, composed: true })
    );
  }

  /** The crater glyph, copied character-for-character from the kit. */
  private _glyph() {
    switch (this.icon) {
      case 'mix':
        return html`<svg viewBox="0 0 24 24">
          <path d="M4 8h7" />
          <path d="M16 8h4" />
          <circle cx="13.5" cy="8" r="2.3" />
          <path d="M4 16h4" />
          <path d="M13 16h7" />
          <circle cx="10.5" cy="16" r="2.3" />
        </svg>`;
      case 'save':
        return html`<svg viewBox="0 0 24 24">
          <path d="M5 5h11l3 3v11H5z" />
          <path d="M8 5v5h7V5" />
          <rect x="8" y="13" width="8" height="5" />
        </svg>`;
      case 'record':
      default:
        return html`<span class="g-rec"></span>`;
    }
  }

  render() {
    if (this.variant === 'power') {
      // The mound's `off` class is the "powered down" state; `on` = powered.
      return html`<div class="mound${this.on ? '' : ' off'}">
        <div class="hill"></div>
        <button class="pbtn" aria-label="Power" @click=${this._clickPower}>
          <svg viewBox="0 0 24 24">
            <path d="M12 3v9" />
            <path d="M7.5 6.5a7 7 0 1 0 9 0" />
          </svg>
        </button>
      </div>`;
    }
    return html`<button class="crater-cell${this.on ? ' on' : ''}" @click=${this._clickCrater}>
      <span class="crater"><slot>${this._glyph()}</slot></span>
    </button>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-crater': HyCrater;
  }
}
