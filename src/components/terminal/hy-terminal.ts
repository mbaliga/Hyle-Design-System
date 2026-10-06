import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type HyTerminalState = 'idle' | 'working' | 'failed';

/**
 * A terminal well — the surface a machine reads its own work back out of.
 *
 * This is a finish, not a costume. The house already has glass (`hy-pane`) for
 * the layer you read through, and grille and jack for the panel you touch;
 * terminal is the layer a machine writes to while you watch. It is cut INTO the
 * surface rather than laid on it, which is why its ground sits below
 * `field.raised` and it carries an inner shadow rather than a drop shadow.
 *
 * Two decisions are worth stating because they are the ones a terminal
 * treatment usually gets wrong:
 *
 * 1. **No phosphor green, no scanlines by default.** The ink is the house ink.
 *    A terminal that cosplays a VT100 is saying "this is technical" in language,
 *    which is exactly the move the house law forbids. What makes this read as a
 *    terminal is the fixed advance, the well, and the cursor, all of which are
 *    functional. `scanlines` exists for the one case where a CRT is literally
 *    the subject, and it is off by default.
 *
 * 2. **State is shown by the cursor, never written out.** `working` breathes,
 *    `idle` holds steady, `failed` stops and goes to the danger hue. There is no
 *    status word anywhere in this component, and no spinner. The cursor is also
 *    the non-colour channel that `failed` is paired with (it stops moving), so
 *    the state survives greyscale and colour-vision deficiency per WCAG 1.4.1.
 *
 * The prompt glyph takes the native-provenance hue on purpose: a prompt means
 * this machine, the one in front of you, is the thing about to act. When the
 * work is happening elsewhere, set `provenance="cloud"` and it takes the cold
 * cyan instead, so where the compute is happening is legible from the prompt
 * alone.
 *
 * @element hy-terminal
 * @slot - Terminal lines. Plain text, or `<span slot="...">` rows.
 * @attr prompt - The prompt glyph. Defaults to `>`.
 * @attr state - `idle` (default), `working`, or `failed`.
 * @attr provenance - `native` (default) or `cloud`.
 * @attr scanlines - Opt in to a CRT line texture. Off by default.
 * @attr no-cursor - Hide the cursor, for a transcript that is finished.
 */
@customElement('hy-terminal')
export class HyTerminal extends LitElement {
  @property() prompt = '>';
  @property({ reflect: true }) state: HyTerminalState = 'idle';
  @property({ reflect: true }) provenance: 'native' | 'cloud' = 'native';
  @property({ type: Boolean, reflect: true }) scanlines = false;
  @property({ type: Boolean, reflect: true, attribute: 'no-cursor' }) noCursor = false;

  static styles = css`
    :host {
      --_ground: var(--color-terminal-ground, #08080a);
      --_ink: var(--color-terminal-ink, rgba(236, 232, 228, 0.92));
      --_dim: var(--color-terminal-dim, rgba(236, 232, 228, 0.42));
      --_faint: var(--color-terminal-faint, rgba(236, 232, 228, 0.18));
      --_hairline: var(--color-terminal-hairline, rgba(255, 255, 255, 0.08));
      --_selection: var(--color-terminal-selection, rgba(199, 239, 158, 0.18));
      --_signal: var(--color-terminal-prompt, #c7ef9e);

      display: block;
      position: relative;
      overflow: hidden;
      background: var(--_ground);
      color: var(--_ink);
      /* Real fixed advance. Columns and right-aligned figures are the whole
         point of this surface, so the fallback chain ends in monospace and
         never in the house sans. */
      font-family: var(--font-family-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
      font-size: var(--font-size-xs, 12px);
      line-height: var(--font-line-height-normal, 1.5);
      font-variant-numeric: tabular-nums;
      padding: 0.9rem 1rem;
      /* Cut in, not laid on: an inner shadow and a hairline, no drop shadow.
         Square by design, because a terminal has no corner radius. */
      box-shadow:
        inset 0 1px 0 rgba(0, 0, 0, 0.6),
        inset 0 0 0 1px var(--_hairline);
    }

    :host([provenance='cloud']) {
      --_signal: var(--color-provenance-cloud, #35e0ff);
    }
    :host([state='failed']) {
      --_signal: var(--color-feedback-danger, #e5564b);
    }

    ::slotted(*) {
      margin: 0;
    }
    ::selection {
      background: var(--_selection);
    }

    .row {
      display: flex;
      align-items: baseline;
      gap: 0.55ch;
    }

    .prompt {
      color: var(--_signal);
      user-select: none;
      flex: none;
    }

    .body {
      min-width: 0;
    }

    /* The cursor carries the state. A block that holds still is idle, one that
       breathes is working, one that has stopped and gone red has failed. No
       word anywhere. */
    .cursor {
      display: inline-block;
      width: 1ch;
      height: 1em;
      margin-left: 0.15ch;
      vertical-align: text-bottom;
      background: var(--_signal);
      flex: none;
    }
    :host([state='idle']) .cursor {
      animation: hy-terminal-blink 1.15s steps(1, end) infinite;
    }
    :host([state='working']) .cursor {
      animation: hy-terminal-breathe 1.05s ease-in-out infinite;
    }
    :host([state='failed']) .cursor {
      animation: none;
      opacity: 1;
    }

    @keyframes hy-terminal-blink {
      0%,
      55% {
        opacity: 1;
      }
      56%,
      100% {
        opacity: 0;
      }
    }
    @keyframes hy-terminal-breathe {
      0%,
      100% {
        opacity: 0.28;
      }
      50% {
        opacity: 1;
      }
    }

    /* A blinking block is the single worst offender on this surface for anyone
       who asked the platform to stop moving things. Hold it solid instead: the
       cursor is still present, still the state channel, just not animated. */
    @media (prefers-reduced-motion: reduce) {
      :host .cursor {
        animation: none !important;
        opacity: 1;
      }
    }

    /* Off by default. Only for when a CRT is literally the subject. */
    .scan {
      position: absolute;
      inset: 0;
      pointer-events: none;
      background: repeating-linear-gradient(
        to bottom,
        rgba(0, 0, 0, 0.22) 0px,
        rgba(0, 0, 0, 0.22) 1px,
        transparent 1px,
        transparent 3px
      );
      mix-blend-mode: multiply;
    }
  `;

  render() {
    return html`
      ${this.scanlines ? html`<div class="scan" part="scanlines" aria-hidden="true"></div>` : nothing}
      <div class="row" part="row">
        <span class="prompt" part="prompt" aria-hidden="true">${this.prompt}</span>
        <div class="body" part="body"><slot></slot></div>
        ${this.noCursor ? nothing : html`<span class="cursor" part="cursor" aria-hidden="true"></span>`}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-terminal': HyTerminal;
  }
}
