import { html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX, ctx } from '../../kit/kit-runtime.js';

/**
 * An analogue joystick — extracted verbatim from the Tactile Kit's
 * "Dial & Joystick" section (line 778, behaviour lines 843–869). Drag the knob
 * within its bowl; the four direction arrows glow as you push and the knob
 * springs back to centre on release. The travel edge ticks (haptic); the
 * release clicks (audio).
 *
 * @element hy-joystick
 * @fires hy-input  - `detail.{x,y}` normalised -1..1 as the stick moves.
 * @fires hy-change - `detail.{x:0,y:0}` on release.
 */
@customElement('hy-joystick')
export class HyJoystick extends KitElement {
  static styles = KitElement.kitStyles;

  firstUpdated() {
    const emit = (type: 'hy-input' | 'hy-change', x: number, y: number) =>
      this.dispatchEvent(new CustomEvent(type, { detail: { x, y }, bubbles: true, composed: true }));

    // Verbatim from the kit (lines 843–857), scoped to the shadow root.
    this.renderRoot.querySelectorAll('.joy2').forEach((stickEl) => {
      const stick = stickEl as HTMLElement;
      var housing = stick.querySelector('.j2-housing') as HTMLElement,
        bowl = stick.querySelector('.j2-bowl') as HTMLElement,
        knob = stick.querySelector('.j2-knob') as HTMLElement,
        knobSh = stick.querySelector('.j2-ksh') as HTMLElement;
      var A = {
        up: stick.querySelector('.j2a-up') as HTMLElement,
        down: stick.querySelector('.j2a-down') as HTMLElement,
        left: stick.querySelector('.j2a-left') as HTMLElement,
        right: stick.querySelector('.j2a-right') as HTMLElement,
      };
      A.up.dataset.base = 'translate(-50%,-50%)';
      A.down.dataset.base = 'translate(-50%,-50%) rotate(180deg)';
      A.left.dataset.base = 'translate(-50%,-50%) rotate(-90deg)';
      A.right.dataset.base = 'translate(-50%,-50%) rotate(90deg)';
      var cx = 0,
        cy = 0,
        maxTravel = 0,
        baseDrop = 0,
        active = false,
        atEdge = false,
        curx = 0,
        cury = 0;
      function measure() {
        var r = housing.getBoundingClientRect();
        cx = r.left + r.width / 2;
        cy = r.top + r.height / 2;
        var bR = bowl.getBoundingClientRect().width / 2,
          kR = knob.getBoundingClientRect().width / 2;
        maxTravel = bR - kR * 0.8;
        baseDrop = kR * 0.16;
        place(0, 0);
      }
      function place(dx: number, dy: number) {
        knob.style.transform =
          'translate(-50%,-50%) translate(' + dx.toFixed(1) + 'px,' + dy.toFixed(1) + 'px)';
        knobSh.style.transform =
          'translate(-50%,-50%) translate(' +
          (dx * 1.12).toFixed(1) +
          'px,' +
          (dy * 1.12 + baseDrop).toFixed(1) +
          'px)';
      }
      function setArrow(el: HTMLElement, t: number) {
        el.style.filter =
          'drop-shadow(0 0 ' +
          (5 + t * 15).toFixed(1) +
          'px rgba(var(--acc-rgb),.6)) drop-shadow(0 0 ' +
          (3 + t * 4).toFixed(1) +
          'px rgba(var(--acc-rgb),.6)) brightness(' +
          (1 + t * 0.45).toFixed(2) +
          ')';
        el.style.transform = el.dataset.base + ' scale(' + (1 + t * 0.16).toFixed(2) + ')';
      }
      function render() {
        setArrow(A.up, Math.max(0, cury));
        setArrow(A.down, Math.max(0, -cury));
        setArrow(A.right, Math.max(0, curx));
        setArrow(A.left, Math.max(0, -curx));
      }
      function update(px: number, py: number) {
        var dx = px - cx,
          dy = py - cy;
        var d = Math.hypot(dx, dy);
        if (maxTravel > 0 && d > maxTravel) {
          var k = maxTravel / d;
          dx *= k;
          dy *= k;
          if (!atEdge) {
            HX.tick();
            atEdge = true;
          }
        } else atEdge = false;
        place(dx, dy);
        curx = maxTravel > 0 ? dx / maxTravel : 0;
        cury = maxTravel > 0 ? -dy / maxTravel : 0;
        render();
        emit('hy-input', curx, cury);
      }
      function onDown(e: any) {
        measure();
        active = true;
        atEdge = false;
        stick.classList.add('active');
        stick.classList.remove('snapping');
        try {
          housing.setPointerCapture(e.pointerId);
        } catch (_) {}
        ctx();
        HX.tick();
        var p = e.touches ? e.touches[0] : e;
        update(p.clientX, p.clientY);
        e.preventDefault();
      }
      function onMove(e: any) {
        if (!active) return;
        var p = e.touches ? e.touches[0] : e;
        update(p.clientX, p.clientY);
      }
      function onUp() {
        if (!active) return;
        active = false;
        stick.classList.remove('active');
        stick.classList.add('snapping');
        place(0, 0);
        curx = 0;
        cury = 0;
        render();
        SFX.click(0.12);
        emit('hy-change', 0, 0);
      }
      housing.addEventListener('pointerdown', onDown);
      housing.addEventListener('pointermove', onMove);
      housing.addEventListener('pointerup', onUp);
      housing.addEventListener('pointercancel', onUp);
      housing.addEventListener('lostpointercapture', onUp);
      window.addEventListener('resize', measure);
      measure();
    });
  }

  render() {
    return html`
      <div class="joy2">
        <span class="j2a j2a-up"
          ><svg viewBox="0 0 28 26">
            <polygon points="14,5 23.5,22 4.5,22" stroke-width="4" stroke-linejoin="round" /></svg
        ></span>
        <span class="j2a j2a-down"
          ><svg viewBox="0 0 28 26">
            <polygon points="14,5 23.5,22 4.5,22" stroke-width="4" stroke-linejoin="round" /></svg
        ></span>
        <span class="j2a j2a-left"
          ><svg viewBox="0 0 28 26">
            <polygon points="14,5 23.5,22 4.5,22" stroke-width="4" stroke-linejoin="round" /></svg
        ></span>
        <span class="j2a j2a-right"
          ><svg viewBox="0 0 28 26">
            <polygon points="14,5 23.5,22 4.5,22" stroke-width="4" stroke-linejoin="round" /></svg
        ></span>
        <div class="j2-housing">
          <div class="j2-bowl"></div>
          <div class="j2-ksh"></div>
          <div class="j2-knob"></div>
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-joystick': HyJoystick;
  }
}
