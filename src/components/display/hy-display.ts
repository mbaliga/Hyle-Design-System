import { html, type PropertyValues } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { KitElement } from '../../kit/kit-element.js';

/**
 * The device display — extracted verbatim from the Tactile Kit's "Display"
 * section (kit lines 612-623 markup; line 1023 driver). A lit status bar (live
 * clock, REC flag, play, wifi) over a scrolling waveform and a running timer.
 *
 * Read-only and self-driving: an idle waveform breathes; set `recording` (or
 * click the REC flag) to capture a live take, and the play button reviews it.
 * State is SHOWN by the material — the flag, the moving head — never said.
 *
 * The kit wires recording to the crater Record key; here the display owns that
 * affordance itself (its REC flag) so it stands alone. Everything drawn — the
 * bars, the play head, the dotted future, the timer — is the kit's exact code.
 *
 * @element hy-display
 * @attr recording - Capture a live take (also toggled by clicking the REC flag).
 * @fires hy-change - `detail.recording` when the record state changes.
 */
@customElement('hy-display')
export class HyDisplay extends KitElement {
  @property({ type: Boolean, reflect: true }) recording = false;

  @query('canvas') private _canvas!: HTMLCanvasElement;
  @query('.timer') private _timerEl!: HTMLElement;
  @query('.clock') private _clockEl!: HTMLElement;
  @query('.reclabel') private _reclabel!: HTMLElement;
  @query('.recflag') private _recflag!: HTMLElement;
  @query('.play-btn') private _playBtn!: HTMLButtonElement;
  @query('.play-icon') private _playIcon!: SVGElement;
  @query('.wifi-status') private _wifiStatus!: HTMLElement;

  private _raf = 0;
  private _ro?: ResizeObserver;
  private _clockTimer = 0;
  /** Bridge into the running driver so the `recording` property can drive it. */
  private _setRec?: (on: boolean) => void;

  static styles = KitElement.kitStyles;

  firstUpdated() {
    this._init();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    cancelAnimationFrame(this._raf);
    clearInterval(this._clockTimer);
    this._ro?.disconnect();
  }

  updated(changed: PropertyValues) {
    if (changed.has('recording')) this._setRec?.(this.recording);
  }

  /** Verbatim from the kit (line 1023): the status/wave/timer display driver. */
  private _init() {
    const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const canvas = this._canvas;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const timerEl = this._timerEl;
    const clockEl = this._clockEl;
    const reclabel = this._reclabel;
    const recflag = this._recflag;
    const playBtn = this._playBtn;
    const playIcon = this._playIcon;
    const wifiStatus = this._wifiStatus;

    let ACC = '#8e7bff';
    const readAcc = () => {
      const v = getComputedStyle(this).getPropertyValue('--acc').trim();
      if (v) ACC = v;
    };
    readAcc();

    let AC: AudioContext | null = null;
    const click = (v = 0.25) => {
      try {
        if (!AC) AC = new (window.AudioContext || (window as any).webkitAudioContext)();
        if (AC.state === 'suspended') AC.resume();
        const t = AC.currentTime,
          o = AC.createOscillator(),
          g = AC.createGain(),
          f = AC.createBiquadFilter();
        f.type = 'bandpass';
        f.frequency.value = 1200;
        f.Q.value = 1.2;
        o.type = 'square';
        o.frequency.setValueAtTime(1800, t);
        o.frequency.exponentialRampToValueAtTime(420, t + 0.04);
        o.connect(f);
        f.connect(g);
        g.connect(AC.destination);
        g.gain.setValueAtTime(v, t);
        g.gain.exponentialRampToValueAtTime(0.001, t + 0.05);
        o.start(t);
        o.stop(t + 0.06);
      } catch (e) {
        /* no audio */
      }
    };
    const vib = (p: number[]) => navigator.vibrate && navigator.vibrate(p);

    const SLOT = 4.3,
      BARW = 2.2,
      BAR_MS = 80;
    let W = 0,
      H = 0,
      dpr = 1,
      slots = 0;
    let bars: number[] = [],
      elapsed = 0,
      mode = 'paused',
      seeded = false,
      barAcc = 0,
      lastTs = 0,
      playStart = 0,
      playDur = 0;

    const resize = () => {
      const r = canvas.getBoundingClientRect();
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      W = r.width;
      H = r.height;
      canvas.width = Math.round(W * dpr);
      canvas.height = Math.round(H * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      slots = Math.max(10, Math.floor(W / SLOT));
      if (!seeded) {
        seed();
        seeded = true;
      }
      draw();
    };
    let simPhase = Math.random() * 10;
    const nextSimAmp = () => {
      simPhase += 0.16 + Math.random() * 0.05;
      const env = 0.55 + 0.45 * Math.sin(simPhase * 0.6);
      const syl = Math.pow(Math.max(0, Math.sin(simPhase * 2.2)), 1.4);
      let v = (0.16 + 0.84 * syl * env) * (0.62 + 0.38 * Math.random());
      if (Math.random() < 0.07) v *= 0.16;
      return Math.max(0.05, Math.min(1, v));
    };
    const seed = () => {
      const fill = Math.round(slots * 0.58);
      bars = [];
      for (let i = 0; i < fill; i++) bars.push(nextSimAmp());
      elapsed = 12100;
      renderTimer();
    };
    const pushBar = () => {
      bars.push(nextSimAmp());
    };
    const drawIdle = (ts: number) => {
      if (!slots) return;
      ctx.clearRect(0, 0, W, H);
      const maxBarH = H * 0.62,
        t = ts / 1000;
      ctx.fillStyle = 'rgba(210,207,200,.42)';
      for (let i = 0; i < slots; i++) {
        const x = i * SLOT + (SLOT - BARW) / 2,
          xi = i / (slots || 1);
        const env = 0.085 + 0.05 * Math.sin(t * 0.9 + xi * 6.0);
        const swell = Math.exp(-Math.pow((xi - (((t * 0.13) % 1.3) - 0.15)) / 0.11, 2)) * 0.32;
        const a =
          (Math.abs(Math.sin(t * 3.1 - i * 0.55)) * 0.6 + Math.abs(Math.sin(t * 5.7 - i * 0.9)) * 0.4) *
            (env + swell) +
          0.01;
        const h = Math.max(1.5, a * maxBarH);
        ctx.fillRect(x, (H - h) / 2, BARW, h);
      }
    };
    const draw = () => {
      ctx.clearRect(0, 0, W, H);
      const maxBarH = H * 0.62;
      if (mode === 'playing') {
        drawReview(maxBarH);
        return;
      }
      const total = bars.length;
      const visible = Math.min(total, slots);
      const offset = total - visible;
      const playX = visible * SLOT;
      ctx.fillStyle = 'rgba(210,207,200,.82)';
      for (let i = 0; i < visible; i++) {
        const a = bars[offset + i];
        const h = Math.max(2, a * maxBarH);
        const x = i * SLOT + (SLOT - BARW) / 2;
        ctx.fillRect(x, (H - h) / 2, BARW, h);
      }
      drawDotted(playX + 2, W - 4);
      drawHead(playX, maxBarH);
    };
    const drawReview = (maxBarH: number) => {
      const total = bars.length;
      if (total === 0) {
        drawDotted(2, W - 4);
        return;
      }
      const bw = Math.max(1, (W - 2) / total);
      const prog = playDur ? Math.min(1, (performance.now() - playStart) / playDur) : 1;
      const sweepX = prog * (W - 2) + 1;
      for (let i = 0; i < total; i++) {
        const a = bars[i];
        const h = Math.max(2, a * maxBarH);
        const x = i * bw + 1;
        ctx.fillStyle = x <= sweepX ? 'rgba(210,207,200,.82)' : 'rgba(210,207,200,.24)';
        ctx.fillRect(x, (H - h) / 2, Math.max(1, bw * 0.55), h);
      }
      drawHead(sweepX, maxBarH);
      if (prog >= 1) stopPlayback();
    };
    const drawDotted = (x0: number, x1: number) => {
      const y = H / 2;
      ctx.fillStyle = 'rgba(150,147,140,.38)';
      for (let x = x0; x < x1; x += 6.5) ctx.fillRect(x, y - 1.2, 2, 2.4);
    };
    const drawHead = (x: number, maxBarH: number) => {
      const topY = H / 2 - maxBarH / 2 - 2,
        botY = H / 2 + maxBarH / 2 + 2;
      ctx.strokeStyle = ACC;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(x + 0.5, topY + 4);
      ctx.lineTo(x + 0.5, botY);
      ctx.stroke();
      ctx.fillStyle = ACC;
      ctx.beginPath();
      ctx.arc(x + 0.5, topY, 4.2, 0, Math.PI * 2);
      ctx.fill();
    };
    const renderTimer = () => {
      const mm = Math.floor(elapsed / 60000),
        ss = Math.floor(elapsed / 1000) % 60,
        cs = Math.floor(elapsed / 10) % 100,
        p = (n: number) => String(n).padStart(2, '0');
      timerEl.textContent = p(mm) + ':' + p(ss) + ':' + p(cs);
    };
    const renderClock = () => {
      const d = new Date();
      let h = d.getHours();
      const m = d.getMinutes();
      const ap = h >= 12 ? 'PM' : 'AM';
      h = h % 12 || 12;
      clockEl.textContent = String(h).padStart(2, '0') + '.' + String(m).padStart(2, '0') + ' ' + ap;
    };
    const loop = (ts: number) => {
      if (!lastTs) lastTs = ts;
      const dt = ts - lastTs;
      lastTs = ts;
      readAcc();
      if (mode === 'recording') {
        elapsed += dt;
        barAcc += dt;
        while (barAcc >= BAR_MS) {
          pushBar();
          barAcc -= BAR_MS;
        }
        renderTimer();
        if (!reduceMotion)
          (recflag.querySelector('.recdot') as HTMLElement).style.opacity =
            Math.floor(ts / 450) % 2 ? '1' : '0.25';
        draw();
      } else if (mode === 'playing') {
        draw();
      } else if (!reduceMotion) {
        drawIdle(ts);
      }
      this._raf = requestAnimationFrame(loop);
    };
    const setFlag = (text: string, recording: boolean) => {
      reclabel.textContent = text;
      recflag.classList.toggle('idle', !recording);
    };
    const setPlayIcon = (playing: boolean) => {
      playIcon.innerHTML = playing
        ? '<rect x="3" y="2.4" width="3" height="10.2" rx=".6"/><rect x="9" y="2.4" width="3" height="10.2" rx=".6"/>'
        : '<path d="M3 2.2v10.6l9-5.3z"/>';
    };
    const startRec = () => {
      if (mode === 'playing') stopPlayback();
      mode = 'recording';
      lastTs = 0;
      barAcc = 0;
      setFlag('REC', true);
      setPlayIcon(false);
      click();
      vib([18, 24, 12]);
      if (!this.recording) {
        this.recording = true;
        this._emit();
      }
    };
    const stopRec = () => {
      mode = 'paused';
      (recflag.querySelector('.recdot') as HTMLElement).style.opacity = '1';
      setFlag(bars.length ? 'PAUSED' : 'READY', false);
      draw();
      click();
      vib([10]);
      if (this.recording) {
        this.recording = false;
        this._emit();
      }
    };
    const startPlayback = () => {
      if (bars.length === 0 || mode === 'recording') return;
      mode = 'playing';
      setFlag('PLAYING', false);
      setPlayIcon(true);
      playStart = performance.now();
      playDur = Math.max(600, bars.length * BAR_MS);
    };
    const stopPlayback = () => {
      if (mode === 'playing') {
        mode = 'paused';
        setFlag(bars.length ? 'PAUSED' : 'READY', false);
      }
      setPlayIcon(false);
      draw();
    };

    // The display owns its record affordance (kit couples it to the crater key).
    recflag.addEventListener('click', () => {
      mode === 'recording' ? stopRec() : startRec();
    });
    playBtn.addEventListener('click', () => {
      mode === 'playing' ? stopPlayback() : startPlayback();
    });
    wifiStatus.addEventListener('click', () => wifiStatus.classList.toggle('off'));

    // Let the reflected `recording` property drive the same start/stop path.
    this._setRec = (on: boolean) => {
      if (on && mode !== 'recording') startRec();
      else if (!on && mode === 'recording') stopRec();
    };

    this._ro = new ResizeObserver(resize);
    this._ro.observe(canvas);
    renderClock();
    this._clockTimer = window.setInterval(renderClock, 15000);
    resize();
    this._raf = requestAnimationFrame(loop);
    if (this.recording) startRec();
  }

  private _emit() {
    this.dispatchEvent(
      new CustomEvent('hy-change', { detail: { recording: this.recording }, bubbles: true, composed: true })
    );
  }

  render() {
    return html`
      <div class="disp" part="display">
        <div class="statusbar">
          <span class="clock">04.35 PM</span>
          <span class="recflag idle"><span class="recdot"></span><span class="reclabel">READY</span></span>
          <span class="sicons">
            <button class="play-btn" aria-label="Play">
              <svg class="play-icon" width="14" height="14" viewBox="0 0 15 15" fill="currentColor">
                <path d="M3 2.2v10.6l9-5.3z" />
              </svg>
            </button>
            <span class="wifi-status">
              <svg
                width="18"
                height="14"
                viewBox="0 0 19 15"
                fill="none"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
              >
                <path d="M2 5.4a11 11 0 0 1 15 0" />
                <path d="M4.6 8a7.2 7.2 0 0 1 9.8 0" />
                <path d="M7.2 10.6a3.4 3.4 0 0 1 4.6 0" />
                <circle cx="9.5" cy="12.7" r=".7" fill="currentColor" stroke="none" />
              </svg>
            </span>
          </span>
        </div>
        <div class="wave"><canvas></canvas></div>
        <div class="timer">00:12:10</div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'hy-display': HyDisplay;
  }
}
