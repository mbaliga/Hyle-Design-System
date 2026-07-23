import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import '../src/components/index.js';

/**
 * A mock app assembled entirely from Hyle components — a small mixer panel
 * floating over the living Field. Drag the faders and knobs, flip the mutes.
 * This is the harness for testing ported controls in a real layout.
 */
const meta: Meta = {
  title: 'Showcase/Mixer',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'field' }, options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

const label = (text: string) => html`
  <div
    style="font:600 var(--font-size-micro) var(--font-family-sans); letter-spacing:var(--font-tracking-label);
           text-transform:uppercase; color:var(--color-text-faint); text-align:center;"
  >
    ${text}
  </div>
`;

const channel = (name: string, level: number, pan: number, muted = false) => html`
  <div style="display:flex; flex-direction:column; align-items:center; gap:12px;">
    <hy-knob min="0" max="100" value=${pan} size="56"></hy-knob>
    ${label('Pan')}
    <hy-fader min="0" max="100" value=${level} height="150"></hy-fader>
    <hy-toggle ?on=${!muted}></hy-toggle>
    ${label(name)}
  </div>
`;

export const Mixer: Story = {
  render: () => html`
    <hy-field src="field/form-world-bg.html" style="height:100vh; border-radius:0;">
      <hy-pane dock="bottom" heading="Mixer · 4 ch" style="margin:18px;">
        <div style="display:flex; gap:28px; justify-content:center; align-items:flex-end; flex-wrap:wrap; padding:8px 4px;">
          ${channel('Ch A', 72, 40)} ${channel('Ch B', 50, 55)} ${channel('Ch C', 38, 50, true)}
          ${channel('Master', 85, 50)}
          <div style="display:flex; flex-direction:column; gap:12px; align-items:center;">
            <hy-vu live></hy-vu>
            <hy-meter live></hy-meter>
            ${label('Output')}
          </div>
        </div>
      </hy-pane>
    </hy-field>
  `,
};

/** The same panel without the engine — a plain inky field, for quick control testing. */
export const PanelOnly: Story = {
  render: () => html`
    <div style="min-height:100vh; background:#0a0809; display:flex; align-items:center; justify-content:center; padding:24px;">
      <hy-pane heading="Mixer · 4 ch">
        <div style="display:flex; gap:28px; justify-content:center; flex-wrap:wrap; padding:8px 4px;">
          ${channel('Ch A', 72, 40)} ${channel('Ch B', 50, 55)} ${channel('Ch C', 38, 50, true)}
          ${channel('Master', 85, 50)}
        </div>
      </hy-pane>
    </div>
  `,
};
