import { LitElement, css, html, svg } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

/**
 * A joystick. Drag the knob in its bowl; direction arrows light as you push.
 * Releases back to centre unless `sticky`. Reports a normalised vector.
 *
 * @element hy-joystick
 * @fires hy-move - `detail.x`, `detail.y` in -1..1.
 * @fires hy-end  - on release.
 */
@customElement('hy-joystick')
export class HyJoystick extends LitElement {
  @property({ type: Boolean }) sticky = false;
  @property({ type: Number }) size = 120;
  @property({ type: Boolean, reflect: true }) disabled = false;

  @state() private _x = 0;
  @state() private _y = 0;
  private _dragging = false;

  static styles = css`
    :host {
      display: inline-block;
      touch-action: none;
    }
    .wrap {
      position: relative;
    }
    .housing {
      position: absolute;
      inset: 14%;
      border-radius: 50%;
      background: radial-gradient(circle at 50% 35%, var(--control-surface-high, #2c2c34), var(--control-groove, #050506) 80%);
      box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.85), inset 0 1px 0 var(--control-rim-soft, rgba(255, 255, 255, 0.09));
    }
    .knob {
      position: absolute;
      left: 50%;
      top: 50%;
      width: 38%;
      height: 38%;
      border-radius: 50%;
      cursor: grab;
      background: radial-gradient(circle at 50% 32%, var(--control-surface-high, #2c2c34), var(--control-surface, #16161a) 72%);
      box-shadow: inset 0 1px 0 var(--control-rim, rgba(255, 255, 255, 0.16)), 0 3px 8px rgba(0, 0, 0, 0.6);
      transition: transform 60ms linear;
    }
    .knob:active {
      cursor: grabbing;
    }
    .arrow {
      fill: var(--control-indicator, #6b6760);
      transition: fill var(--duration-instant, 120ms) var(--easing-standard, ease);
    }
    .arrow.on {
      fill: var(--color-action-primary, #8e7bff);
    }
    :host([disabled]) .knob {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `;

  private _emit(type: 'hy-move' | 'hy-end') {
    this.dispatchEvent(
      new CustomEvent(type, { detail: { x: this._x, y: this._y }, bubbles: true, composed: true })
    );
  }

  private _onDown(e: PointerEvent) {
    if (this.disabled) return;
    this._dragging = true;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    this._move(e);
  }
  private _move(e: PointerEvent) {
    if (!this._dragging) return;
    const host = this.getBoundingClientRect();
    const cx = host.left + host.width / 2;
    const cy = host.top + host.height / 2;
    const r = host.width * 0.31; // travel radius
    let dx = (e.clientX - cx) / r;
    let dy = (e.clientY - cy) / r;
    const mag = Math.hypot(dx, dy);
    if (mag > 1) {
      dx /= mag;
      dy /= mag;
    }
    this._x = dx;
    this._y = -dy; // up is positive
    this._emit('hy-move');
  }
  private _onUp(e: PointerEvent) {
    if (!this._dragging) return;
    this._dragging = false;
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    if (!this.sticky) {
      this._x = 0;
      this._y = 0;
    }
    this._emit('hy-end');
  }
  private _onKey(e: KeyboardEvent) {
    const d: Record<string, [number, number]> = {
      ArrowUp: [0, 1],
      ArrowDown: [0, -1],
      ArrowLeft: [-1, 0],
      ArrowRight: [1, 0],
    };
    if (!d[e.key]) return;
    e.preventDefault();
    this._x = d[e.key][0];
    this._y = d[e.key][1];
    this._emit('hy-move');
  }

  render() {
    const s = this.size;
    const travel = s * 0.31; // px
    const tx = this._x * travel;
    const ty = -this._y * travel;
    const lit = (cond: boolean) => (cond ? 'arrow on' : 'arrow');
    const T = 0.4;
    return html`
      <div
        class="wrap"
        part="wrap"
        style="width:${s}px;height:${s}px"
        role="application"
        aria-label="Joystick"
        tabindex=${this.disabled ? -1 : 0}
        @keydown=${this._onKey}
      >
        <svg viewBox="0 0 100 100" style="position:absolute;inset:0;width:100%;height:100%">
          ${svg`
            <polygon class=${lit(this._y > T)} points="50,2 56,12 44,12"/>
            <polygon class=${lit(this._y < -T)} points="50,98 56,88 44,88"/>
            <polygon class=${lit(this._x < -T)} points="2,50 12,44 12,56"/>
            <polygon class=${lit(this._x > T)} points="98,50 88,44 88,56"/>
          `}
        </svg>
        <div class="housing"></div>
        <div
          class="knob"
          part="knob"
          style="transform:translate(-50%,-50%) translate(${tx}px, ${ty}px)"
          @pointerdown=${this._onDown}
          @pointermove=${this._move}
          @pointerup=${this._onUp}
          @pointercancel=${this._onUp}
        ></div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-joystick': HyJoystick;
  }
}
