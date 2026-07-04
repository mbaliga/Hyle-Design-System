import { html } from 'lit';
import { customElement, query, queryAll } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/**
 * A stereo segmented level meter — lifted verbatim from the Tactile Kit's Meters
 * section. Read-only: the level is SHOWN by how much material lights (the top
 * segments in the hot red hue), with a monospace peak read-out in the header.
 * The signal self-animates exactly as the kit's demo does.
 *
 * Markup: tactile-kit.html line 784. Behaviour: lines 1016–1018.
 *
 * @element hy-meter
 */
@customElement('hy-meter')
export class HyMeter extends KitElement {
  @query('.pk') private _pk!: HTMLSpanElement;
  @queryAll('.mbars') private _rows!: NodeListOf<HTMLElement>;

  static styles = KitElement.kitStyles;

  private _raf = 0;
  private _reduced = false;

  firstUpdated() {
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

    // Verbatim (line 1016): N=22, fill each .mbars with N <i></i>, seed levels.
    const N = 22;
    const mkBars = (el: HTMLElement) => {
      el.innerHTML = Array(N).fill('<i></i>').join('');
      return [...el.children] as HTMLElement[];
    };
    const [rowL, rowR] = this._rows;
    const bL = mkBars(rowL),
      bR = mkBars(rowR);
    const lvl = [0.42, 0.38];

    // Verbatim (line 1017): light segments, top three "hot".
    const litBars = (bars: HTMLElement[], v: number) => {
      const lit = Math.round(v * N);
      bars.forEach((b, i) => {
        if (i < lit) {
          const hot = i >= N - 3;
          b.style.background = hot ? '#e83020' : 'var(--acc)';
          b.style.opacity = hot ? '1' : '.85';
        } else {
          b.style.background = 'var(--srf2)';
          b.style.opacity = '1';
        }
      });
    };

    // Verbatim (line 1018): random-walk both channels, update header peak.
    const animFrame = () => {
      [bL, bR].forEach((bars, c) => {
        const t = 0.18 + Math.random() * 0.65;
        lvl[c] += (t - lvl[c]) * 0.28;
        litBars(bars, lvl[c]);
      });
      const db = -20 + lvl[0] * 25;
      this._pk.textContent = (db > 0 ? '+' : '') + db.toFixed(1) + ' dB';
      this._raf = requestAnimationFrame(animFrame);
    };

    if (this._reduced) {
      // One static frame, no RAF (like other Hyle canvas components).
      litBars(bL, lvl[0]);
      litBars(bR, lvl[1]);
      const db = -20 + lvl[0] * 25;
      this._pk.textContent = (db > 0 ? '+' : '') + db.toFixed(1) + ' dB';
    } else {
      animFrame();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
  }

  render() {
    return html`
      <div class="meter">
        <div class="meter-hd"><span>LEVEL</span><span class="pk">-inf</span></div>
        <div class="mrow"><span class="mch">L</span><div class="mbars"></div></div>
        <div class="mrow"><span class="mch">R</span><div class="mbars"></div></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-meter': HyMeter;
  }
}
