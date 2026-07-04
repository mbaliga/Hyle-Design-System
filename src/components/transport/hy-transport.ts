import { html } from 'lit';
import { customElement, queryAll, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX, ctx } from '../../kit/kit-runtime.js';

type TransportAction = 'play' | 'pause' | 'rewind' | 'previous' | 'next' | 'stop';

/**
 * A segmented transport bar — extracted verbatim from the Tactile Kit
 * (markup lines 749-761, behaviour lines 1002-1010).
 *
 * Five segments — Rewind, Previous, Play/Pause, Next, Stop — separated by
 * dividers, with a `.car-label` read-out that says "Ready" until you touch it.
 * The play segment toggles its glyph between play and pause; every press clicks
 * (audio + haptic) exactly as it does in the kit.
 *
 * @element hy-transport
 * @fires hy-change - `detail.action` ('play'|'pause'|'rewind'|'previous'|'next'|'stop').
 */
@customElement('hy-transport')
export class HyTransport extends KitElement {
  @queryAll('.seg') private _segs!: NodeListOf<HTMLElement>;
  @query('#tb2svg') private _pbsvg!: SVGSVGElement;
  @query('.car-label') private _carLabel!: HTMLElement;

  static styles = KitElement.kitStyles;

  // Verbatim from the kit (line 1002-1003).
  private _playing = false;
  private static readonly PLAY = '<path d="M8 6.5v11l9-5.5z"/>';
  private static readonly PAUSE =
    '<line x1="9.5" y1="6.5" x2="9.5" y2="17.5"/><line x1="14.5" y1="6.5" x2="14.5" y2="17.5"/>';

  firstUpdated() {
    const [tb0, tb1, tb2, tb3, tb4] = this._segs;
    const pb = tb2,
      pbsvg = this._pbsvg,
      carLabel = this._carLabel;

    const setLbl = (t: string) => {
      if (carLabel) carLabel.textContent = t;
    };
    const tapSeg = (el: HTMLElement, t: string) => {
      el.classList.add('sel');
      setLbl(t);
      SFX.click();
      HX.click();
      setTimeout(() => {
        if (el !== pb || !this._playing) el.classList.remove('sel');
      }, 220);
    };

    pb.addEventListener('click', () => {
      this._playing = !this._playing;
      pbsvg.innerHTML = this._playing ? HyTransport.PAUSE : HyTransport.PLAY;
      pb.classList.toggle('sel', this._playing);
      setLbl(this._playing ? 'Playing' : 'Paused');
      SFX.click();
      ctx();
      this._emit(this._playing ? 'play' : 'pause');
    });
    tb0.addEventListener('click', (e) => {
      tapSeg(e.currentTarget as HTMLElement, 'Rewind');
      this._emit('rewind');
    });
    tb1.addEventListener('click', (e) => {
      tapSeg(e.currentTarget as HTMLElement, 'Previous');
      this._emit('previous');
    });
    tb3.addEventListener('click', (e) => {
      tapSeg(e.currentTarget as HTMLElement, 'Next');
      this._emit('next');
    });
    tb4.addEventListener('click', (e) => {
      if (this._playing) {
        this._playing = false;
        pbsvg.innerHTML = HyTransport.PLAY;
        pb.classList.remove('sel');
      }
      (e.currentTarget as HTMLElement).classList.add('sel');
      setLbl('Stopped');
      SFX.click();
      HX.click();
      setTimeout(() => (e.currentTarget as HTMLElement).classList.remove('sel'), 220);
      this._emit('stop');
    });
  }

  private _emit(action: TransportAction) {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { action }, bubbles: true, composed: true })
    );
  }

  render() {
    return html`
      <div class="transport">
        <div class="car-bar">
          <div class="seg" aria-label="Rewind">
            <svg viewBox="0 0 24 24"><path d="M11 8.5v7l-5-3.5z" /><path d="M18 8.5v7l-5-3.5z" /></svg>
          </div>
          <div class="divider"></div>
          <div class="seg" aria-label="Previous">
            <svg viewBox="0 0 24 24"><path d="M17 7.5v9l-6.5-4.5z" /><line x1="7.5" y1="7.5" x2="7.5" y2="16.5" /></svg>
          </div>
          <div class="divider"></div>
          <div class="seg" aria-label="Play">
            <svg viewBox="0 0 24 24" id="tb2svg"><path d="M8 6.5v11l9-5.5z" /></svg>
          </div>
          <div class="divider"></div>
          <div class="seg" aria-label="Next">
            <svg viewBox="0 0 24 24"><path d="M7 7.5v9l6.5-4.5z" /><line x1="16.5" y1="7.5" x2="16.5" y2="16.5" /></svg>
          </div>
          <div class="divider"></div>
          <div class="seg" aria-label="Stop">
            <svg viewBox="0 0 24 24"><rect x="6.75" y="6.75" width="10.5" height="10.5" rx="2.5" /></svg>
          </div>
        </div>
        <div class="car-label">Ready</div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-transport': HyTransport;
  }
}
