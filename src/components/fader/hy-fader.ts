import { html, type PropertyValues } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { ctx } from '../../kit/kit-runtime.js';

/**
 * A vertical channel fader — extracted verbatim from the Tactile Kit's Faders
 * section (line 727). Drag the cap along its slot to set a level; the numeric
 * read-out below mirrors the kit's `.cval`.
 *
 * The kit's `makeVFader` (line 992) has no per-detent sound — only a single
 * `ctx()` on grab — so this control stays silent while moving, faithfully.
 *
 * @element hy-fader
 * @fires hy-input  - `detail.value` as the cap moves.
 * @fires hy-change - `detail.value` on release.
 */
@customElement('hy-fader')
export class HyFader extends KitElement {
  @property({ type: Number, reflect: true }) value = 72;

  @query('.vf') private _el!: HTMLElement;
  @query('.vf-cap') private _cap!: HTMLElement;

  // The rendered read-out (the kit shows a numeric `.cval` beneath each fader).
  @state() private _display = 0;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    this._makeVFader(this._el, this._cap, (v: number) => {
      this._display = Math.round(v);
    });
  }

  /** Verbatim from the kit (line 992): drag the cap along its slot. */
  private _makeVFader(wrap: HTMLElement, cap: HTMLElement, onChg: (v: number) => void) {
    const CAP_H = 22;
    let val = +(wrap.dataset.val as string) / 100,
      drag = false;
    const render = () => {
      const slot = wrap.querySelector('.vf-slot') as HTMLElement,
        h = slot.clientHeight || 180,
        travel = h - CAP_H;
      cap.style.top = (1 - val) * travel + 'px';
      this.value = val * 100;
      onChg(val * 100);
    };
    const fromE = (e: any) => {
      const slot = wrap.querySelector('.vf-slot') as HTMLElement,
        r = slot.getBoundingClientRect();
      val = Math.max(0, Math.min(1, 1 - ((e.touches ? e.touches[0] : e).clientY - r.top) / (r.height - CAP_H)));
      render();
    };
    wrap.addEventListener('pointerdown', (e: any) => {
      drag = true;
      fromE(e);
      ctx();
      this._emit('hy-input');
    });
    window.addEventListener('pointermove', (e: any) => {
      if (drag) {
        fromE(e);
        this._emit('hy-input');
      }
    });
    window.addEventListener('pointerup', () => {
      if (drag) {
        drag = false;
        this._emit('hy-change');
      }
    });
    requestAnimationFrame(render);
  }

  private _emit(type: 'hy-input' | 'hy-change') {
    this.dispatchEvent(new CustomEvent(type, { detail: { value: this.value }, bubbles: true, composed: true }));
  }

  render() {
    return html`
      <div class="col">
        <div class="vf" data-val=${this.value}>
          <div class="vf-body">
            <div class="vf-slot"><div class="vf-cap"></div></div>
            <div class="vf-scale">
              <span>10</span><span>8</span><span>6</span><span>4</span><span>2</span><span>0</span>
            </div>
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
    'hy-fader': HyFader;
  }
}
