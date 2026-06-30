import { LitElement, css, html, svg } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

const MAX_ANGLE = 52; // degrees either side of centre

/**
 * An analogue VU meter — a needle that swings with the signal, ballistic and
 * unhurried. Read-only: the level is the needle's angle, nothing is spelled out.
 *
 * @element hy-vu
 * @attr value - 0–100.
 * @attr live  - Self-animate a demo signal with realistic needle inertia.
 */
@customElement('hy-vu')
export class HyVu extends LitElement {
  @property({ type: Number }) value = 0;
  @property({ type: Boolean, reflect: true }) live = false;

  @state() private _shown = 0;
  private _raf = 0;

  static styles = css`
    :host {
      display: inline-block;
    }
    .face {
      width: 240px;
      max-width: 100%;
      padding: 14px 16px 10px;
      border-radius: var(--radius-md, 8px);
      background: linear-gradient(180deg, #1b1a16, #100f0c);
      box-shadow: inset 0 1px 0 var(--control-rim-soft, rgba(255, 255, 255, 0.09));
    }
    svg {
      display: block;
      width: 100%;
      height: auto;
    }
    .arc {
      fill: none;
      stroke: var(--control-indicator, #6b6760);
      stroke-width: 1.5;
    }
    .arc.hot {
      stroke: var(--color-feedback-danger, #e5564b);
      stroke-width: 2.5;
    }
    .tick {
      stroke: var(--control-indicator, #6b6760);
      stroke-width: 1.5;
    }
    .needle {
      stroke: var(--color-text-primary, rgba(236, 232, 228, 0.92));
      stroke-width: 2;
      stroke-linecap: round;
    }
    .pivot {
      fill: var(--control-edge, #3a3a44);
    }
    .lbl {
      font-family: var(--font-family-sans);
      font-size: 9px;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      fill: var(--color-text-faint, rgba(236, 232, 228, 0.18));
    }
  `;

  private _pt(angleDeg: number, radius: number, cx = 120, cy = 104) {
    const a = (angleDeg * Math.PI) / 180;
    return { x: cx + radius * Math.sin(a), y: cy - radius * Math.cos(a) };
  }

  private _arcPath(a0: number, a1: number, r: number) {
    const p0 = this._pt(a0, r);
    const p1 = this._pt(a1, r);
    return `M ${p0.x} ${p0.y} A ${r} ${r} 0 0 1 ${p1.x} ${p1.y}`;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.live) this._tick();
  }
  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
  }
  updated(changed: Map<string, unknown>) {
    if (changed.has('live')) {
      cancelAnimationFrame(this._raf);
      if (this.live) this._tick();
    }
  }

  private _tick = () => {
    const target = 30 + Math.abs(Math.sin(performance.now() / 650)) * 62;
    this._shown += (target - this._shown) * 0.08; // needle inertia
    this.requestUpdate();
    this._raf = requestAnimationFrame(this._tick);
  };

  render() {
    const v = this.live ? this._shown : this.value;
    const angle = -MAX_ANGLE + (Math.min(100, Math.max(0, v)) / 100) * 2 * MAX_ANGLE;
    const tip = this._pt(angle, 86);
    const hotStart = MAX_ANGLE * 0.45;
    return html`
      <div class="face" part="face">
        <svg viewBox="0 0 240 116" role="img" aria-label="VU meter">
          ${svg`
            <path class="arc" d=${this._arcPath(-MAX_ANGLE, hotStart, 92)} />
            <path class="arc hot" d=${this._arcPath(hotStart, MAX_ANGLE, 92)} />
            ${[-MAX_ANGLE, -26, 0, 26, MAX_ANGLE].map((a) => {
              const o = this._pt(a, 92);
              const i = this._pt(a, 82);
              return svg`<line class="tick" x1=${o.x} y1=${o.y} x2=${i.x} y2=${i.y} />`;
            })}
            <line class="needle" x1="120" y1="104" x2=${tip.x} y2=${tip.y} />
            <circle class="pivot" cx="120" cy="104" r="5" />
            <text class="lbl" x="120" y="70" text-anchor="middle">VU</text>
          `}
        </svg>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-vu': HyVu;
  }
}
