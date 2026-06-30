import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

const SWEEP = 240; // total degrees across the options
const START = -120;

/**
 * A stepped selector dial — a rotary switch with detents (e.g. gain O / I / II).
 * Click to advance, or use ←/→. The pointer snaps to each detent.
 *
 * @element hy-dial
 * @fires hy-change - `detail.index`, `detail.value`.
 */
@customElement('hy-dial')
export class HyDial extends LitElement {
  /** Detent labels, shown around the dial. */
  @property({ type: Array }) options: string[] = ['O', 'I', 'II'];
  @property({ type: Number }) index = 0;
  @property({ type: Number }) size = 72;
  @property({ type: Boolean, reflect: true }) disabled = false;

  static styles = css`
    :host {
      display: inline-flex;
    }
    .wrap {
      position: relative;
    }
    .dial {
      position: absolute;
      inset: 18%;
      border-radius: 50%;
      cursor: pointer;
      background: radial-gradient(circle at 50% 32%, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a) 74%);
      box-shadow: inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)), inset 0 -3px 6px var(--control-groove, #050506), 0 3px 6px rgba(0, 0, 0, 0.5);
      outline: none;
      transition: transform var(--duration-instant, 120ms) var(--easing-standard, cubic-bezier(0.4, 0, 0.2, 1));
    }
    .dial:focus-visible {
      box-shadow: 0 0 0 2px var(--color-border-focus, #8e7bff);
    }
    .tick {
      position: absolute;
      left: 50%;
      top: 8%;
      width: 3px;
      height: 22%;
      margin-left: -1.5px;
      border-radius: 2px;
      background: var(--color-action-primary, #8e7bff);
    }
    .mark {
      position: absolute;
      font: 600 9px / 1 var(--font-family-sans);
      letter-spacing: 0.1em;
      color: var(--color-text-faint, rgba(236, 232, 228, 0.18));
      transform: translate(-50%, -50%);
    }
    .mark.sel {
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
    }
    :host([disabled]) .dial {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `;

  private _angleFor(i: number) {
    const n = Math.max(1, this.options.length - 1);
    return START + (SWEEP * i) / n;
  }

  private _select(i: number) {
    const clamped = Math.min(this.options.length - 1, Math.max(0, i));
    if (clamped === this.index) return;
    this.index = clamped;
    this.dispatchEvent(
      new CustomEvent('hy-change', {
        detail: { index: this.index, value: this.options[this.index] },
        bubbles: true,
        composed: true,
      })
    );
  }

  private _advance() {
    if (this.disabled) return;
    this._select((this.index + 1) % this.options.length);
  }
  private _onKey(e: KeyboardEvent) {
    if (e.key === 'ArrowRight' || e.key === 'ArrowUp') this._select(this.index + 1);
    else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') this._select(this.index - 1);
    else return;
    e.preventDefault();
  }

  render() {
    const s = this.size;
    const r = s * 0.46;
    return html`
      <div class="wrap" part="wrap" style="width:${s}px;height:${s}px">
        ${this.options.map((opt, i) => {
          const a = (this._angleFor(i) - 90) * (Math.PI / 180);
          const x = s / 2 + r * Math.cos(a);
          const y = s / 2 + r * Math.sin(a);
          return html`<span class="mark ${i === this.index ? 'sel' : ''}" style="left:${x}px;top:${y}px"
            >${opt}</span
          >`;
        })}
        <div
          class="dial"
          part="dial"
          role="slider"
          aria-valuemin="0"
          aria-valuemax=${this.options.length - 1}
          aria-valuenow=${this.index}
          aria-valuetext=${this.options[this.index]}
          tabindex=${this.disabled ? -1 : 0}
          style="transform:rotate(${this._angleFor(this.index)}deg)"
          @click=${this._advance}
          @keydown=${this._onKey}
        >
          <span class="tick"></span>
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-dial': HyDial;
  }
}
