import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * A device screen — the lit glass panel that carries displays (waveform, timer,
 * status). Warm-dark backlight with monospace ink. A `recording` flag breathes.
 *
 * @element hy-screen
 * @slot - Screen content.
 * @attr clock - Optional clock string in the status bar.
 * @attr status - Optional right-aligned status word.
 * @attr recording - Show a breathing REC dot.
 */
@customElement('hy-screen')
export class HyScreen extends LitElement {
  @property() clock = '';
  @property() status = '';
  @property({ type: Boolean, reflect: true }) recording = false;

  static styles = css`
    :host {
      display: block;
    }
    .screen {
      border-radius: var(--radius-md, 8px);
      padding: 12px 14px;
      background: linear-gradient(180deg, #161310, #100e0b);
      color: #dddbd6;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05), inset 0 0 24px rgba(0, 0, 0, 0.6);
      font-family: var(--font-family-mono, ui-monospace, monospace);
    }
    .bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 11px;
      letter-spacing: 0.12em;
      color: #8d8a84;
      margin-bottom: 10px;
    }
    .rec {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--color-feedback-danger, #e5564b);
    }
    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
      box-shadow: 0 0 6px currentColor;
      animation: breathe var(--duration-pane, 2400ms) ease-in-out infinite;
    }
    @keyframes breathe {
      0%, 100% { opacity: 0.42; }
      50% { opacity: 0.9; }
    }
    @media (prefers-reduced-motion: reduce) {
      .dot { animation: none; }
    }
  `;

  render() {
    const showBar = this.clock || this.status || this.recording;
    return html`
      <div class="screen" part="screen">
        ${showBar
          ? html`<div class="bar">
              <span>${this.clock}</span>
              ${this.recording
                ? html`<span class="rec"><span class="dot"></span>REC</span>`
                : html`<span>${this.status}</span>`}
            </div>`
          : nothing}
        <slot></slot>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-screen': HyScreen;
  }
}
