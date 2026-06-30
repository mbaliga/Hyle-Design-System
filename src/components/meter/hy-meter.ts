import { LitElement, css, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

const SEGMENTS = 16;

/**
 * A stereo level meter — segmented bars that fill with signal. A read-only state
 * display: level is shown by how much material lights, the top segments in the
 * danger hue. Set `live` to drive a gentle demo signal.
 *
 * @element hy-meter
 * @attr left  - L level, 0–100.
 * @attr right - R level, 0–100.
 * @attr live  - Self-animate a demo signal.
 */
@customElement('hy-meter')
export class HyMeter extends LitElement {
  @property({ type: Number }) left = 0;
  @property({ type: Number }) right = 0;
  @property({ type: Boolean, reflect: true }) live = false;

  @state() private _l = 0;
  @state() private _r = 0;

  private _raf = 0;

  static styles = css`
    :host {
      display: inline-block;
      font-family: var(--font-family-sans);
    }
    .meter {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 12px;
      border-radius: var(--radius-md, 8px);
      background: var(--control-surface, #16161a);
      box-shadow: inset 0 1px 0 var(--control-rim-soft, rgba(255, 255, 255, 0.09));
    }
    .hd {
      font-size: var(--font-size-micro, 9.5px);
      letter-spacing: var(--font-tracking-label, 0.2em);
      text-transform: uppercase;
      color: var(--color-text-faint, rgba(236, 232, 228, 0.18));
    }
    .row {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .ch {
      width: 10px;
      font-size: var(--font-size-micro, 9.5px);
      color: var(--color-text-faint, rgba(236, 232, 228, 0.18));
    }
    .bars {
      display: flex;
      gap: 2px;
      flex: 1;
    }
    .seg {
      flex: 1;
      height: 8px;
      border-radius: 1px;
      background: var(--control-groove, #050506);
    }
    .seg.on {
      background: var(--color-action-primary, #8e7bff);
    }
    .seg.on.hot {
      background: var(--color-feedback-warning, #e0941a);
    }
    .seg.on.peak {
      background: var(--color-feedback-danger, #e5564b);
    }
  `;

  private _bars(level: number) {
    const lit = Math.round((Math.min(100, Math.max(0, level)) / 100) * SEGMENTS);
    return Array.from({ length: SEGMENTS }, (_, i) => {
      const on = i < lit;
      const cls = on ? (i >= SEGMENTS - 2 ? 'on peak' : i >= SEGMENTS - 5 ? 'on hot' : 'on') : '';
      return html`<span class="seg ${cls}"></span>`;
    });
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
    // Smooth, bounded wander — "alive" without churn.
    const ease = (cur: number, target: number) => cur + (target - cur) * 0.12;
    this._l = ease(this._l, 35 + Math.abs(Math.sin(performance.now() / 700)) * 55);
    this._r = ease(this._r, 30 + Math.abs(Math.sin(performance.now() / 620 + 1)) * 58);
    this.requestUpdate();
    this._raf = requestAnimationFrame(this._tick);
  };

  render() {
    const l = this.live ? this._l : this.left;
    const r = this.live ? this._r : this.right;
    return html`
      <div class="meter" part="meter">
        <div class="hd">Level</div>
        <div class="row"><span class="ch">L</span><div class="bars">${this._bars(l)}</div></div>
        <div class="row"><span class="ch">R</span><div class="bars">${this._bars(r)}</div></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-meter': HyMeter;
  }
}
