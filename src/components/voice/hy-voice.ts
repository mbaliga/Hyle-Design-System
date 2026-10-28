import { html, css, type PropertyValues } from 'lit';
import { customElement, property, queryAll, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';
import { SFX, HX, ctx } from '../../kit/kit-runtime.js';

/** One axis of a voice: left/right pole labels, detent positions, and the active detent. */
export interface HyVoiceAxis { left: string; right: string; pos?: number[]; act?: number }

/**
 * Voice settings — stepped sliders linked by a glowing halo, lifted from the Tactile
 * Kit's "Voice Settings" section (kit lines 393-405 CSS, 680-688 markup, 859-878 JS).
 * Each row is one axis of a sound/feel character (warm↔bright, soft↔hard…); the halo
 * threads the active detents so the whole voice reads as one shape.
 *
 * @element hy-voice
 * @fires hy-input - `detail.{row, act, values}` when a row changes detent
 *   (`values` = each row's normalised 0..1 position).
 */
@customElement('hy-voice')
export class HyVoice extends KitElement {
  @property({ type: Array }) axes: HyVoiceAxis[] = [
    { left: 'Warm', right: 'Bright', act: 3 },
    { left: 'Soft', right: 'Hard', act: 3 },
    { left: 'Dry', right: 'Wet', act: 1 },
    { left: 'Narrow', right: 'Wide', act: 0 },
    { left: 'Clean', right: 'Driven', act: 2 },
  ];
  /** Row index highlighted as the one being edited (kit `.hot`); -1 for none. */
  @property({ type: Number }) hot = 1;

  @query('.sset') private _sset!: HTMLElement;
  @queryAll('.sset-row') private _rows!: NodeListOf<HTMLElement>;
  @query('.g1') private _g1!: SVGPathElement;
  @query('.g2') private _g2!: SVGPathElement;
  @query('.nodes') private _nodes!: SVGGElement;

  private _state: { pos: number[]; act: number; dot?: HTMLElement }[] = [];
  private _ro?: ResizeObserver;

  static styles = [
    KitElement.kitStyles,
    css`
      :host { display: block; }
      /* verbatim from the kit, lines 393-405 */
      .sset{position:relative;display:flex;flex-direction:column;gap:26px;padding:10px 2px}
      .sset-halo{position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:0;overflow:visible}
      .sset-row{position:relative;display:grid;grid-template-columns:74px 1fr 74px;align-items:center;gap:12px;z-index:1}
      .sl{font-size:9px;letter-spacing:.1em;text-transform:uppercase;color:var(--ink2);text-align:right;white-space:nowrap}
      .sr{font-size:9px;letter-spacing:.1em;text-transform:uppercase;color:var(--ink2);white-space:nowrap}
      .sset-track{position:relative;height:22px;display:flex;align-items:center;cursor:pointer;touch-action:none}
      .sset-track::before{content:'';position:absolute;left:0;right:0;height:1.5px;background:var(--edge);border-radius:1px}
      .sset-opt{position:absolute;width:7px;height:7px;border-radius:50%;top:50%;transform:translate(-50%,-50%);background:var(--ink3);opacity:.5;z-index:2}
      .sset-dot{position:absolute;width:16px;height:16px;border-radius:50%;top:50%;transform:translate(-50%,-50%);background:radial-gradient(circle at 40% 34%,#3a3a42,#070709);box-shadow:0 1px 2px var(--drk),0 0 0 1px var(--rim2);z-index:3;transition:left .14s var(--ease)}
      .sset-row.hot .sset-dot{background:radial-gradient(circle at 42% 36%,#ffd9a8,var(--acc) 55%,var(--acc-lo));box-shadow:0 0 12px rgba(var(--acc-rgb),.85),0 1px 2px var(--drk)}
      .sset-row.hot .sl,.sset-row.hot .sr{background:var(--srf2);padding:5px 9px;border-radius:11px;box-shadow:0 0 0 1px var(--rim2)}
    `,
  ];

  /** Current normalised positions, one per row. */
  get values() { return this._state.map((s) => s.pos[s.act]); }

  /** Move a row to a detent (as a finger would). Fires hy-input when it changes. */
  setDetent(row: number, act: number) {
    const s = this._state[row];
    if (!s) return;
    const a = Math.max(0, Math.min(s.pos.length - 1, act));
    if (a === s.act) return;
    s.act = a; this.hot = row; this._place(); this._animHalo();
    this._fire(row);
  }

  firstUpdated() {
    this._state = this.axes.map((a) => ({ pos: a.pos ?? [0.2, 0.4, 0.6, 0.8], act: a.act ?? 0 }));
    this._rows.forEach((r, i) => {
      const track = r.querySelector('.sset-track') as HTMLElement, s = this._state[i];
      s.pos.forEach((p) => { const o = document.createElement('div'); o.className = 'sset-opt'; o.style.left = p * 100 + '%'; track.appendChild(o); });
      const dot = document.createElement('div'); dot.className = 'sset-dot'; track.appendChild(dot); s.dot = dot;
      // verbatim selection logic (kit line 875): nearest detent to the finger
      const sel = (e: any) => {
        const tb = track.getBoundingClientRect(), cx = (e.touches ? e.touches[0] : e).clientX;
        const x = Math.max(0, Math.min(1, (cx - tb.left) / tb.width));
        let best = 0, bd = 9; s.pos.forEach((p, k) => { const q = Math.abs(p - x); if (q < bd) { bd = q; best = k; } });
        const ch = best !== s.act; s.act = best; s.dot!.style.left = s.pos[best] * 100 + '%';
        if (ch) { SFX.tick(); HX.tick(); this.hot = i; this._fire(i); }
        this._animHalo();
      };
      let drag = false;
      track.addEventListener('pointerdown', (e) => { drag = true; sel(e); ctx(); e.preventDefault(); });
      window.addEventListener('pointermove', (e) => { if (drag) sel(e); });
      window.addEventListener('pointerup', () => { drag = false; });
    });
    this._ro = new ResizeObserver(() => this._place());
    this._ro.observe(this._sset);
    this._place();
  }

  disconnectedCallback() { super.disconnectedCallback(); this._ro?.disconnect(); }

  protected updated(changed: PropertyValues) {
    if (changed.has('hot')) this._rows?.forEach((r, i) => r.classList.toggle('hot', i === this.hot));
  }

  private _fire(row: number) {
    this.dispatchEvent(new CustomEvent('hy-input', { detail: { row, act: this._state[row].act, values: this.values }, bubbles: true, composed: true }));
  }

  private _animHalo() { const t0 = performance.now(); const an = () => { this._halo(); if (performance.now() - t0 < 240) requestAnimationFrame(an); }; an(); }

  private _place() { this._state.forEach((s) => { if (s.dot) s.dot.style.left = s.pos[s.act] * 100 + '%'; }); this._halo(); }

  /** Verbatim halo path (kit line 869): Catmull-Rom through the active dots, two blurred strokes + node glows. */
  private _halo() {
    if (!this._sset || !this._state.length || !this._state[0].dot) return;
    const sb = this._sset.getBoundingClientRect();
    const pts = this._state.map((s) => { const d = s.dot!.getBoundingClientRect(); return { x: d.left + d.width / 2 - sb.left, y: d.top + d.height / 2 - sb.top }; });
    let dd = 'M ' + pts[0].x.toFixed(1) + ' ' + pts[0].y.toFixed(1);
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || pts[i + 1];
      dd += ` C ${(p1.x + (p2.x - p0.x) / 6).toFixed(1)} ${(p1.y + (p2.y - p0.y) / 6).toFixed(1)} ${(p2.x - (p3.x - p1.x) / 6).toFixed(1)} ${(p2.y - (p3.y - p1.y) / 6).toFixed(1)} ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
    }
    this._g1.setAttribute('d', dd); this._g2.setAttribute('d', dd);
    this._nodes.innerHTML = pts.map((p) => `<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="15" fill="var(--halo, #5fb8ff)" filter="url(#hvB1)" opacity="0.55"></circle>`).join('');
  }

  render() {
    return html`
      <div class="sset">
        <svg class="sset-halo" preserveAspectRatio="none">
          <defs>
            <filter id="hvB1" x="-90%" y="-90%" width="280%" height="280%"><feGaussianBlur stdDeviation="18" /></filter>
            <filter id="hvB2" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="10" /></filter>
          </defs>
          <path class="g1" fill="none" stroke="var(--halo, #46abff)" stroke-width="28" stroke-linecap="round" stroke-linejoin="round" filter="url(#hvB1)" opacity="0.30"></path>
          <path class="g2" fill="none" stroke="var(--halo, #5fb8ff)" stroke-width="14" stroke-linecap="round" stroke-linejoin="round" filter="url(#hvB2)" opacity="0.46"></path>
          <g class="nodes"></g>
        </svg>
        ${this.axes.map((a, i) => html`<div class="sset-row ${i === this.hot ? 'hot' : ''}"><span class="sl">${a.left}</span><div class="sset-track"></div><span class="sr">${a.right}</span></div>`)}
      </div>
    `;
  }
}

declare global { interface HTMLElementTagNameMap { 'hy-voice': HyVoice } }
