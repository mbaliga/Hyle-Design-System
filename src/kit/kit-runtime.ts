// AUTO-EXTRACTED VERBATIM from kit/tactile-kit.html (lines 822-829).
// The Tactile Kit's shared audio + haptics engines. Every control's interaction
// routes its detents through these, so lifting them verbatim keeps the *feel*
// identical, not just the look. Do not reinterpret — the kit is the source of truth.

/* eslint-disable */
let AC: any = null;

/** Lazily-created (and resumed) shared AudioContext — verbatim from the kit. */
export const ctx = () => {
  if (!AC || AC.state === 'closed') AC = new (window.AudioContext || (window as any).webkitAudioContext)();
  if (AC.state === 'suspended') AC.resume();
  return AC;
};

/** The kit's sound effects — square-wave click, sine tick, noise thunk. Verbatim. */
export const SFX = {
  click(v = 0.28) {
    try {
      const c = ctx(),
        t = c.currentTime,
        o = c.createOscillator(),
        g = c.createGain(),
        f = c.createBiquadFilter();
      f.type = 'bandpass';
      f.frequency.value = 1200;
      f.Q.value = 1.2;
      o.type = 'square';
      o.frequency.setValueAtTime(1800, t);
      o.frequency.exponentialRampToValueAtTime(400, t + 0.04);
      o.connect(f);
      f.connect(g);
      g.connect(c.destination);
      g.gain.setValueAtTime(v, t);
      g.gain.exponentialRampToValueAtTime(0.001, t + 0.05);
      o.start(t);
      o.stop(t + 0.06);
    } catch (e) {}
  },
  tick() {
    try {
      const c = ctx(),
        t = c.currentTime,
        o = c.createOscillator(),
        g = c.createGain();
      o.type = 'sine';
      o.frequency.value = 3600;
      o.connect(g);
      g.connect(c.destination);
      g.gain.setValueAtTime(0.06, t);
      g.gain.exponentialRampToValueAtTime(0.001, t + 0.015);
      o.start(t);
      o.stop(t + 0.02);
    } catch (e) {}
  },
  thunk() {
    try {
      const c = ctx(),
        sr = c.sampleRate,
        dur = 0.08,
        buf = c.createBuffer(1, Math.ceil(sr * dur), sr),
        d = buf.getChannelData(0);
      for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.exp(-i / (sr * 0.02));
      const src = c.createBufferSource(),
        g = c.createGain(),
        lp = c.createBiquadFilter();
      lp.type = 'lowpass';
      lp.frequency.value = 240;
      src.buffer = buf;
      src.connect(lp);
      lp.connect(g);
      g.connect(c.destination);
      g.gain.value = 0.45;
      src.start();
      this.click(0.18);
    } catch (e) {}
  },
};

/** The kit's haptics — short navigator.vibrate bursts. Verbatim. */
export const HX = {
  click: () => navigator.vibrate?.([9]),
  toggle: () => navigator.vibrate?.([18, 24, 10]),
  tick: () => navigator.vibrate?.([4]),
};
