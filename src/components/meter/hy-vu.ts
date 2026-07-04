import { html } from 'lit';
import { customElement, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/**
 * An analogue VU meter — lifted verbatim from the Tactile Kit's Meters section.
 * A needle swings across an etched scale (red past 0 dB); read-only, the level is
 * the needle's angle. The scale is drawn by the kit's exact builder and the
 * needle self-animates exactly as the kit's demo does.
 *
 * Markup: tactile-kit.html line 785. Scale: line 1014. Needle: line 1018.
 *
 * @element hy-vu
 */
@customElement('hy-vu')
export class HyVu extends KitElement {
  @query('.vu-sc') private _g!: SVGGElement;
  @query('.vu-needle') private _needle!: SVGLineElement;

  static styles = KitElement.kitStyles;

  private _raf = 0;
  private _reduced = false;

  firstUpdated() {
    this._reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

    // Verbatim scale-builder IIFE (line 1014).
    ((g: SVGGElement) => {
      const cx = 180,
        cy = 88,
        R = 74;
      const dba = (db: number) => -62 + ((db + 30) / 33) * 94;
      const pt = (a: number, r: number) => ({
        x: cx + r * Math.sin((a * Math.PI) / 180),
        y: cy - r * Math.cos((a * Math.PI) / 180),
      });
      const marks: [number, string][] = [
        [-30, '-30'],
        [-20, '-20'],
        [-10, '-10'],
        [-5, '-5'],
        [0, '0'],
        [3, '+3'],
      ];
      let h = '';
      const p1 = pt(dba(-30), R),
        p2 = pt(dba(3), R);
      h += `<path fill="none" stroke="currentColor" stroke-width=".8" opacity=".1" d="M ${p1.x.toFixed(1)} ${p1.y.toFixed(1)} A ${R} ${R} 0 0 1 ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}"/>`;
      marks.forEach(([db, lbl]) => {
        const a = dba(db),
          red = db >= 0,
          i = pt(a, R - 10),
          o = pt(a, R),
          tp = pt(a, R + 11);
        h += `<line x1="${i.x.toFixed(1)}" y1="${i.y.toFixed(1)}" x2="${o.x.toFixed(1)}" y2="${o.y.toFixed(1)}" stroke="${red ? 'var(--acc)' : 'currentColor'}" stroke-width="${red ? 1.4 : 1}" opacity="${red ? 0.75 : 0.4}"/>`;
        h += `<text x="${tp.x.toFixed(1)}" y="${tp.y.toFixed(1)}" text-anchor="middle" dominant-baseline="middle" fill="${red ? 'var(--acc)' : 'currentColor'}" font-family="ui-monospace,monospace" font-size="8.5" opacity="${red ? 0.75 : 0.38}">${lbl}</text>`;
      });
      g.innerHTML = h;
    })(this._g);

    // Verbatim needle animation (line 1018): random-walk drives the rotation.
    const lvl = [0.42];
    const animFrame = () => {
      const t = 0.18 + Math.random() * 0.65;
      lvl[0] += (t - lvl[0]) * 0.28;
      this._needle.style.transform = `rotate(${18 - lvl[0] * 80}deg)`;
      this._raf = requestAnimationFrame(animFrame);
    };

    if (this._reduced) {
      this._needle.style.transform = `rotate(${18 - lvl[0] * 80}deg)`;
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
      <div class="vu-wrap">
        <div class="vu-face">
          <svg viewBox="0 0 360 98" preserveAspectRatio="xMidYMid meet">
            <g class="vu-sc"></g>
            <line class="vu-needle" x1="180" y1="88" x2="180" y2="12" />
            <circle class="vu-pivot" cx="180" cy="88" r="6" />
          </svg>
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-vu': HyVu;
  }
}
