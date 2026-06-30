import { LitElement, css, html, svg } from 'lit';
import { customElement, property } from 'lit/decorators.js';

type Action = 'rewind' | 'prev' | 'play' | 'next' | 'stop';

const ICONS: Record<Action, ReturnType<typeof svg>> = {
  rewind: svg`<path d="M11 8.5v7l-5-3.5z"/><path d="M18 8.5v7l-5-3.5z"/>`,
  prev: svg`<path d="M17 7.5v9l-6.5-4.5z"/><line x1="7.5" y1="7.5" x2="7.5" y2="16.5"/>`,
  play: svg`<path d="M8 6.5v11l9-5.5z"/>`,
  next: svg`<path d="M7 7.5v9l6.5-4.5z"/><line x1="16.5" y1="7.5" x2="16.5" y2="16.5"/>`,
  stop: svg`<rect x="6.75" y="6.75" width="10.5" height="10.5" rx="2.5"/>`,
};
const PAUSE = svg`<rect x="7.5" y="6.5" width="3" height="11" rx="1"/><rect x="13.5" y="6.5" width="3" height="11" rx="1"/>`;

/**
 * A segmented transport bar. The play segment toggles to pause when `playing`.
 *
 * @element hy-transport
 * @fires hy-transport - `detail.action` ('rewind'|'prev'|'play'|'pause'|'next'|'stop').
 */
@customElement('hy-transport')
export class HyTransport extends LitElement {
  @property({ type: Boolean, reflect: true }) playing = false;
  /** Subset/order of segments to show. */
  @property({ type: Array }) actions: Action[] = ['rewind', 'prev', 'play', 'next', 'stop'];

  static styles = css`
    :host {
      display: inline-block;
    }
    .bar {
      display: inline-flex;
      align-items: stretch;
      border-radius: var(--radius-md, 8px);
      background: var(--control-surface, #16161a);
      box-shadow: inset 0 1px 0 var(--control-rim-soft, rgba(255, 255, 255, 0.09)), 0 2px 6px rgba(0, 0, 0, 0.5);
      overflow: hidden;
    }
    button {
      appearance: none;
      border: 0;
      background: transparent;
      color: var(--color-text-secondary, rgba(236, 232, 228, 0.42));
      width: 46px;
      height: 40px;
      display: grid;
      place-items: center;
      cursor: pointer;
      transition: color var(--duration-instant, 120ms) var(--easing-standard, ease), background var(--duration-instant, 120ms);
    }
    button:hover {
      color: var(--color-text-primary, rgba(236, 232, 228, 0.92));
    }
    button:active {
      background: var(--control-groove, #050506);
    }
    button.lit {
      color: var(--color-action-primary, #8e7bff);
    }
    button:focus-visible {
      outline: 2px solid var(--color-border-focus, #8e7bff);
      outline-offset: -2px;
    }
    .divider {
      width: 1px;
      background: var(--control-groove, #050506);
      margin: 8px 0;
    }
    svg {
      width: 24px;
      height: 24px;
      fill: currentColor;
      stroke: currentColor;
      stroke-width: 1.4;
      stroke-linejoin: round;
    }
    svg line,
    svg rect {
      fill: none;
    }
  `;

  private _fire(a: Action) {
    let action: string = a;
    if (a === 'play') {
      this.playing = !this.playing;
      action = this.playing ? 'play' : 'pause';
    }
    this.dispatchEvent(
      new CustomEvent('hy-transport', { detail: { action }, bubbles: true, composed: true })
    );
  }

  render() {
    return html`
      <div class="bar" part="bar" role="group" aria-label="Transport">
        ${this.actions.map(
          (a, i) => html`
            ${i ? html`<div class="divider"></div>` : ''}
            <button
              class=${this.playing && a === 'play' ? 'lit' : ''}
              aria-label=${this.playing && a === 'play' ? 'pause' : a}
              @click=${() => this._fire(a)}
            >
              <svg viewBox="0 0 24 24">${a === 'play' && this.playing ? PAUSE : ICONS[a]}</svg>
            </button>
          `
        )}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-transport': HyTransport;
  }
}
