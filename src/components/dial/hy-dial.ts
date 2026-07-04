import { html, type PropertyValues } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX, ctx } from '../../kit/kit-runtime.js';

/**
 * A continuous rotary dial — extracted verbatim from the Tactile Kit's
 * "Dial & Joystick" section (line 777). Drag vertically (or scroll) to turn;
 * the value is shown by the tick's rotation and the numeric read-out beneath.
 * Each detent clicks (audio + haptic) exactly as it does in the kit.
 *
 * The kit wires it with `makeKnob('dl1', …)` (line 984) — identical drag/wheel
 * logic to the knob, except `makeKnob` rotates the `.dl-rot.rot` element.
 *
 * @element hy-dial
 * @fires hy-input  - `detail.value` as the dial turns.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-dial')
export class HyDial extends KitElement {
  @property({ type: Number, reflect: true }) min = 0;
  @property({ type: Number, reflect: true }) max = 100;
  @property({ type: Number, reflect: true }) value = 60;

  @query('.dial2') private _el!: HTMLElement;

  // The rendered read-out (the kit shows a numeric `.cval` beneath the dial).
  @state() private _display = 60;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    this._makeKnob(this._el, (v: number) => {
      this._display = Math.round(v);
    });
  }

  /** Verbatim from the kit (line 979): drag / wheel to turn, click per detent. */
  private _makeKnob(el: HTMLElement, onChg: (v: number, t: number) => void) {
    const rotEl = (el.querySelector('.rot') || el.querySelector('.kface')) as HTMLElement;
    const min = this.min,
      max = this.max;
    let val = this.value,
      prevS = -999;
    const render = () => {
      const t = (val - min) / (max - min);
      rotEl.style.transform = `rotate(${-135 + 270 * t}deg)`;
      this.value = val;
      onChg(val, t);
    };
    let drag = false,
      sy = 0,
      sv = 0;
    el.addEventListener('pointerdown', (e: any) => {
      drag = true;
      sy = (e.touches ? e.touches[0] : e).clientY;
      sv = val;
      el.setPointerCapture?.(e.pointerId);
      e.preventDefault();
      ctx();
    });
    window.addEventListener('pointermove', (e: any) => {
      if (!drag) return;
      const y = (e.touches ? e.touches[0] : e).clientY;
      val = Math.max(min, Math.min(max, sv + (sy - y) / 165 * (max - min)));
      const s = Math.floor((val - min) / (max - min) * 20);
      if (s !== prevS) {
        SFX.tick();
        HX.tick();
        prevS = s;
      }
      render();
      this._emit('hy-input');
    });
    window.addEventListener('pointerup', () => {
      if (drag) {
        drag = false;
        this._emit('hy-change');
      }
    });
    el.addEventListener(
      'wheel',
      (e: WheelEvent) => {
        e.preventDefault();
        const ps = Math.floor((val - min) / (max - min) * 20);
        val = Math.max(min, Math.min(max, val - Math.sign(e.deltaY) * (max - min) / 40));
        if (Math.floor((val - min) / (max - min) * 20) !== ps) {
          SFX.tick();
          HX.tick();
        }
        render();
        this._emit('hy-input');
        this._emit('hy-change');
      },
      { passive: false }
    );
    render();
  }

  private _emit(type: 'hy-input' | 'hy-change') {
    this.dispatchEvent(new CustomEvent(type, { detail: { value: this.value }, bubbles: true, composed: true }));
  }

  render() {
    return html`
      <div class="col">
        <div class="dial2" data-min=${this.min} data-max=${this.max} data-val=${this.value}>
          <div class="dl-mound"></div>
          <div class="dl-knob">
            <div class="dl-cap"></div>
            <div class="dl-rot rot"><div class="dl-tick"></div></div>
          </div>
        </div>
        <div class="cval" part="value">${this._display}</div>
      </div>
    `;
  }

  // Keep the numeric read-out in sync when value is set programmatically.
  protected updated(changed: PropertyValues) {
    if (changed.has('value') && !Number.isNaN(this.value)) this._display = Math.round(this.value);
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-dial': HyDial;
  }
}
