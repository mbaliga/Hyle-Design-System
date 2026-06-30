import { LitElement, css, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HyPulseState = 'still' | 'watched' | 'active';

/**
 * "Heartbeat, not weather" — the ambient-emission primitive. Wraps content
 * (typically a provenance light) and breathes its glow on a slow, regular,
 * low-amplitude cycle that *means* alive / connected / watched — never churn.
 *
 * The web counterpart of the Kotlin `Pulse` contract:
 *   watched ≈ Pulse.WATCHED (2.4 s, 42–78% alpha) · still ≈ Pulse.STILL.
 *
 * @element hy-pulse
 * @slot - The element that should breathe (e.g. a coloured dot).
 * @attr state - still | watched | active.
 */
@customElement('hy-pulse')
export class HyPulse extends LitElement {
  @property({ reflect: true }) state: HyPulseState = 'watched';

  static styles = css`
    :host {
      display: inline-flex;
    }
    .breath {
      display: inline-flex;
      animation: hy-breathe var(--_period, 2400ms) ease-in-out infinite;
    }
    :host([state='still']) .breath {
      animation: none;
      opacity: 1;
    }
    :host([state='watched']) .breath {
      --_period: var(--duration-pane, 2400ms);
      --_min: 0.42;
      --_max: 0.78;
    }
    :host([state='active']) .breath {
      --_period: 1000ms;
      --_min: 0.55;
      --_max: 1;
    }
    @keyframes hy-breathe {
      0%,
      100% {
        opacity: var(--_min, 0.42);
      }
      50% {
        opacity: var(--_max, 0.78);
      }
    }
    @media (prefers-reduced-motion: reduce) {
      .breath {
        animation: none;
        opacity: var(--_max, 1);
      }
    }
  `;

  render() {
    return html`<span class="breath" part="breath"><slot></slot></span>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-pulse': HyPulse;
  }
}
