import { html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { ctx } from '../../kit/kit-runtime.js';

/** Horizontal-slider variants, lifted verbatim from the kit's Sliders section (lines 717-721). */
export type HySliderVariant = 'hairline' | 'channel' | 'minimal';

/** The kit's per-variant default value (`data-val`): hsh=30, hsf=60, hsd=45. */
const DEFAULTS: Record<HySliderVariant, number> = { hairline: 30, channel: 60, minimal: 45 };

/**
 * A horizontal slider — extracted verbatim from the Tactile Kit. Drag anywhere on
 * the track to set the value; a numeric `.cval` read-out sits beside it.
 *
 * Three faithful variants:
 *  - `hairline` — the thin rail with a round thumb (kit `.hsh`, with `makeHSlider`)
 *  - `channel`  — the inset filled track (kit `.hsf`, with `makeFSlider`; PAD=20,
 *                 read-out wraps its value in `<em>`)
 *  - `minimal`  — the rail with an end-cap and a dimple knob (kit `.hsd`, inline IIFE)
 *
 * These sliders carry no per-detent tick in the kit, so no sound is wired — only
 * the shared `ctx()` is resumed on pointerdown, exactly as the kit does.
 *
 * @element hy-slider
 * @fires hy-input  - `detail.value` as it moves.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-slider')
export class HySlider extends KitElement {
  @property({ reflect: true }) variant: HySliderVariant = 'channel';
  @property({ type: Number, reflect: true }) value = NaN;

  static styles = KitElement.kitStyles;

  willUpdate() {
    if (Number.isNaN(this.value)) this.value = DEFAULTS[this.variant];
  }

  firstUpdated() {
    const root = this.renderRoot as unknown as HTMLElement;
    const disp = root.querySelector('.cval') as HTMLElement;
    if (this.variant === 'hairline') {
      const wrap = root.querySelector('.hsh') as HTMLElement;
      this._makeHSlider(wrap, wrap.querySelector('.fill') as HTMLElement, wrap.querySelector('.thumb') as HTMLElement, disp);
    } else if (this.variant === 'minimal') {
      const wrap = root.querySelector('.hsd') as HTMLElement;
      this._makeDSlider(wrap, wrap.querySelector('.knob') as HTMLElement, disp);
    } else {
      const wrap = root.querySelector('.hsf') as HTMLElement;
      this._makeFSlider(wrap, wrap.querySelector('.fill') as HTMLElement, wrap.querySelector('.thumb') as HTMLElement, disp);
    }
  }

  private _emit(type: 'hy-input' | 'hy-change') {
    this.dispatchEvent(new CustomEvent(type, { detail: { value: this.value }, bubbles: true, composed: true }));
  }

  /** Verbatim from the kit (line 986): the hairline slider. */
  private _makeHSlider(wrap: HTMLElement, fill: HTMLElement, thumb: HTMLElement, disp: HTMLElement) {
    let val = +wrap.dataset.val! / 100, drag = false;
    const render = () => { const w = wrap.clientWidth, px = val * w; fill.style.width = px + 'px'; thumb.style.left = px + 'px'; if (disp) disp.textContent = Math.round(val * 100) + '%'; this.value = val * 100; };
    const fromE = (e: any) => { const r = wrap.getBoundingClientRect(); val = Math.max(0, Math.min(1, ((e.touches ? e.touches[0] : e).clientX - r.left) / r.width)); render(); this._emit('hy-input'); };
    wrap.addEventListener('pointerdown', e => { drag = true; fromE(e); ctx(); });
    window.addEventListener('pointermove', e => { if (drag) fromE(e); });
    window.addEventListener('pointerup', () => { if (drag) { drag = false; this._emit('hy-change'); } });
    requestAnimationFrame(render);
  }

  /** Verbatim from the kit (line 989): the channel slider (PAD=20, `<em>` read-out). */
  private _makeFSlider(wrap: HTMLElement, fill: HTMLElement, thumb: HTMLElement, disp: HTMLElement) {
    const PAD = 20;
    let val = +wrap.dataset.val! / 100, drag = false;
    const render = () => { const w = wrap.clientWidth - PAD * 2, px = PAD + val * w; fill.style.width = (val * 100) + '%'; thumb.style.left = px + 'px'; if (disp) disp.innerHTML = `<em>${Math.round(val * 100)}%</em>`; this.value = val * 100; };
    const fromE = (e: any) => { const r = wrap.getBoundingClientRect(); val = Math.max(0, Math.min(1, ((e.touches ? e.touches[0] : e).clientX - r.left - PAD) / (r.width - PAD * 2))); render(); this._emit('hy-input'); };
    wrap.addEventListener('pointerdown', e => { drag = true; fromE(e); ctx(); });
    window.addEventListener('pointermove', e => { if (drag) fromE(e); });
    window.addEventListener('pointerup', () => { if (drag) { drag = false; this._emit('hy-change'); } });
    requestAnimationFrame(render);
  }

  /** Verbatim from the kit (line 1000): the minimal / dimple slider (inline IIFE). */
  private _makeDSlider(wrap: HTMLElement, k: HTMLElement, d: HTMLElement) {
    let val = +wrap.dataset.val! / 100, drag = false;
    const render = () => { const px = 10 + val * (wrap.clientWidth - 28); k.style.left = px + 'px'; if (d) d.textContent = Math.round(val * 100) + '%'; this.value = val * 100; };
    const fromE = (e: any) => { const r = wrap.getBoundingClientRect(); val = Math.max(0, Math.min(1, ((e.touches ? e.touches[0] : e).clientX - r.left - 10) / (r.width - 28))); render(); this._emit('hy-input'); };
    wrap.addEventListener('pointerdown', e => { drag = true; fromE(e); ctx(); });
    window.addEventListener('pointermove', e => { if (drag) fromE(e); });
    window.addEventListener('pointerup', () => { if (drag) { drag = false; this._emit('hy-change'); } });
    requestAnimationFrame(render);
  }

  private _slider() {
    switch (this.variant) {
      case 'hairline':
        return html`<div class="hsh" data-val=${this.value}><div class="rail"></div><div class="fill"></div><div class="thumb"></div></div>`;
      case 'minimal':
        return html`<div class="hsd" data-val=${this.value}><div class="rail"></div><div class="end"></div><div class="knob"></div></div>`;
      case 'channel':
      default:
        return html`<div class="hsf" data-val=${this.value}><div class="track"><div class="fill"></div></div><div class="thumb"></div></div>`;
    }
  }

  render() {
    return html`
      <div class="col" style="width:100%;gap:6px">
        ${this._slider()}
        <div class="cval" part="value"></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-slider': HySlider;
  }
}
