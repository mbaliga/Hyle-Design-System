import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * The Field — Hyle's living material layer that sits *behind the pane*.
 *
 * This is where state is shown, never said: monumental forms in coloured haze
 * (the Form-World SDF engine), matte and near-still at rest, moving only when
 * there is real computation. Panes float on top via the default slot.
 *
 * The heavy WebGL engine is loaded in an iframe from `src` so the field stays a
 * drop-in surface for any app. With no `src`, it falls back to a quiet inky
 * gradient — the "at rest, no field" state — so the component is always usable.
 *
 * @element hy-field
 * @slot - Content layered over the field (typically one or more <hy-pane>).
 * @attr src - URL of the Form-World engine HTML (e.g. "/field/form-world.html").
 * @attr label - Accessible label for the background.
 */
@customElement('hy-field')
export class HyField extends LitElement {
  /** URL to the Form-World engine. Empty → static inky fallback. */
  @property() src = '';

  /** Accessible description of the background surface. */
  @property() label = 'Hyle field';

  static styles = css`
    :host {
      display: block;
      position: relative;
      width: 100%;
      min-height: 320px;
      overflow: hidden;
      background: var(--color-background-field, #000);
      border-radius: var(--radius-lg, 12px);
      isolation: isolate;
    }
    .bg {
      position: absolute;
      inset: 0;
      z-index: 0;
      border: 0;
      width: 100%;
      height: 100%;
      display: block;
    }
    /* Resting state: the field is still and inky, light only where it gathers. */
    .rest {
      position: absolute;
      inset: 0;
      z-index: 0;
      background:
        radial-gradient(
          120% 90% at 70% 18%,
          rgba(142, 123, 255, 0.1),
          transparent 55%
        ),
        radial-gradient(140% 120% at 30% 100%, #0a0809, #000 70%);
    }
    .overlay {
      position: relative;
      z-index: 1;
      width: 100%;
      height: 100%;
      min-height: inherit;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      pointer-events: none;
    }
    ::slotted(*) {
      pointer-events: auto;
    }
  `;

  render() {
    return html`
      ${this.src
        ? html`<iframe
            class="bg"
            part="engine"
            src=${this.src}
            title=${this.label}
            loading="lazy"
            aria-hidden="true"
            scrolling="no"
          ></iframe>`
        : html`<div class="rest" part="rest" role="img" aria-label=${this.label}></div>`}
      ${this.src ? html`<span class="visually-hidden" hidden>${this.label}</span>` : nothing}
      <div class="overlay"><slot></slot></div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-field': HyField;
  }
}
