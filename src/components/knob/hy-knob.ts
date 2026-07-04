import { html, type PropertyValues } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX, ctx } from '../../kit/kit-runtime.js';

/** Knob variants, lifted verbatim from the kit's Knobs section (lines 708-711). */
export type HyKnobVariant = 'precision' | 'standard' | 'minimal' | 'dial';

/**
 * A rotary knob — extracted verbatim from the Tactile Kit. Drag vertically (or
 * scroll) to turn; value is shown only by the pointer's rotation, never in words.
 * Each detent clicks (audio + haptic) exactly as it does in the kit.
 *
 * Four faithful variants:
 *  - `precision` — the etched 140px scale ring (kit `.kscale`, with `buildRing`)
 *  - `standard`  — the 104px collar knob with an accent dot (kit `.kplain`)
 *  - `minimal`   — the bare 78px knob with an accent line (kit `.ksmall`)
 *  - `dial`      — the outlined sphere with a concave dimple (kit `.kdial`)
 *
 * @element hy-knob
 * @fires hy-input  - `detail.value` as the knob turns.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-knob')
export class HyKnob extends KitElement {
  @property() variant: HyKnobVariant = 'precision';
  @property({ type: Number }) min = 0;
  @property({ type: Number }) max = 100;
  @property({ type: Number }) value = 40;

  @query('.knob-root') private _el!: HTMLElement;
  @query('.kscale svg') private _svg?: SVGSVGElement;

  // The rendered read-out (the kit shows a numeric `.cval` beside each knob).
  @state() private _display = 0;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    if (this.variant === 'precision' && this._svg) this._buildRing(this._svg, 70, 70, 68, 50);
    this._makeKnob(this._el, (v: number) => {
      this._display = Math.round(v);
    });
  }

  /** Verbatim from the kit (line 976): draw the etched scale ring into the svg. */
  private _buildRing(svg: SVGSVGElement, cx: number, cy: number, Ro: number, Ri: number) {
    const N = 27;
    let h = '';
    for (let i = 0; i <= N; i++) {
      const a = (-135 + (270 / N) * i) * Math.PI / 180;
      const maj = i % Math.round(N / 4) === 0;
      const r1 = maj ? Ri + 4 : Ri + 7;
      const x1 = cx + r1 * Math.sin(a),
        y1 = cy - r1 * Math.cos(a);
      const x2 = cx + (Ro - 2) * Math.sin(a),
        y2 = cy - (Ro - 2) * Math.cos(a);
      h += `<line x1="${x1.toFixed(1)}" y1="${y1.toFixed(1)}" x2="${x2.toFixed(1)}" y2="${y2.toFixed(1)}" stroke="currentColor" stroke-width="${maj ? 1.5 : 1}" opacity="${maj ? 0.6 : 0.3}"/>`;
      if (maj && i > 0 && i < N) {
        const v = Math.round((i / N) * 100);
        const tr = r1 - 7,
          tx = cx + tr * Math.sin(a),
          ty = cy - tr * Math.cos(a);
        h += `<text x="${tx.toFixed(1)}" y="${ty.toFixed(1)}" text-anchor="middle" dominant-baseline="middle" fill="currentColor" font-family="ui-monospace,monospace" font-size="7.5" opacity=".45">${v}</text>`;
      }
    }
    h += `<circle cx="${cx}" cy="${cy}" r="${Ro - 1.5}" fill="none" stroke="currentColor" stroke-width=".8" opacity=".18"/>`;
    svg.innerHTML = h;
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

  private _face() {
    switch (this.variant) {
      case 'standard':
        return html`<div class="kface"><div class="kdot"></div></div>`;
      case 'minimal':
        return html`<div class="kface"><div class="kind"></div></div>`;
      case 'dial':
        return html`<div class="dsphere"></div><div class="kface"><div class="ddimple"></div></div>`;
      case 'precision':
      default:
        return html`<svg viewBox="0 0 140 140"></svg><div class="kface"><div class="kind"></div></div>`;
    }
  }

  private _cls() {
    return { precision: 'kscale', standard: 'kplain', minimal: 'ksmall', dial: 'kdial' }[this.variant];
  }

  render() {
    return html`
      <div class="col">
        <div
          class="knob-root ${this._cls()}"
          data-min=${this.min}
          data-max=${this.max}
          data-val=${this.value}
        >
          ${this._face()}
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
    'hy-knob': HyKnob;
  }
}
