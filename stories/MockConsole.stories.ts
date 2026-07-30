import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import '../src/components/index.js';

/**
 * A full device console assembled from Hyle components — screen + transport,
 * knobs, sliders, keys, a joystick, a gain dial, meters, a grille and jacks —
 * floating on a glass pane over the living Field. The harness for testing the
 * whole tactile library in one app.
 */
const meta: Meta = {
  title: 'Showcase/Console',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'field' }, options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

const lbl = (t: string) => html`
  <div
    style="font:600 var(--font-size-micro) var(--font-family-sans); letter-spacing:var(--font-tracking-label);
           text-transform:uppercase; color:var(--color-text-faint); text-align:center;"
  >
    ${t}
  </div>
`;

const col = (label: string, control: unknown) => html`
  <div style="display:flex; flex-direction:column; align-items:center; gap:10px;">${control}${lbl(label)}</div>
`;

export const Console: Story = {
  render: () => html`
    <hy-field src="field/form-world-bg.html" style="height:100vh; border-radius:0;">
      <hy-pane dock="bottom" heading="Console" style="margin:16px;">
        <div style="display:flex; flex-direction:column; gap:18px;">
          <!-- top: screen + transport + meters -->
          <div style="display:flex; gap:18px; align-items:stretch; flex-wrap:wrap;">
            <hy-screen clock="04.35 PM" recording style="flex:1; min-width:260px;">
              <hy-waveform live></hy-waveform>
            </hy-screen>
            <div style="display:flex; flex-direction:column; gap:12px; justify-content:center;">
              <hy-transport playing></hy-transport>
              <hy-meter live></hy-meter>
            </div>
            <hy-vu live></hy-vu>
          </div>

          <!-- middle: channel strips + joystick/dial -->
          <div style="display:flex; gap:26px; align-items:flex-end; flex-wrap:wrap;">
            ${col('Drive', html`<hy-knob value="62" size="60"></hy-knob>`)}
            ${col('Tone', html`<hy-knob value="38" size="60"></hy-knob>`)}
            ${col('Ch A', html`<hy-fader value="72" height="120"></hy-fader>`)}
            ${col('Ch B', html`<hy-fader value="48" height="120"></hy-fader>`)}
            ${col('Pan', html`<div style="width:160px"><hy-slider variant="hairline" origin="center" value="50"></hy-slider></div>`)}
            ${col('Nav', html`<hy-joystick size="104"></hy-joystick>`)}
            ${col('Gain', html`<hy-dial .options=${['O', 'I', 'II']} size="64"></hy-dial>`)}
          </div>

          <!-- bottom: keys, grille, jacks -->
          <div style="display:flex; gap:24px; align-items:center; flex-wrap:wrap;">
            <div style="display:flex; gap:10px;">
              ${['M', 'S', 'R', 'FX'].map(
                (k) => html`<hy-key mode="toggle" led><span style="font:600 13px var(--font-family-sans)">${k}</span></hy-key>`
              )}
            </div>
            <div style="flex:1; min-width:160px;"><hy-grille height="48"></hy-grille></div>
            <div style="display:flex; gap:16px;">
              <hy-jack></hy-jack><hy-jack></hy-jack><hy-jack></hy-jack>
            </div>
          </div>
        </div>
      </hy-pane>
    </hy-field>
  `,
};
