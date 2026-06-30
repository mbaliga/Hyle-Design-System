import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-pulse.js';

const meta: Meta = {
  title: 'Material/Pulse',
  component: 'hy-pulse',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { state: { control: 'inline-radio', options: ['still', 'watched', 'active'] } },
  args: { state: 'watched' },
  render: ({ state }) => html`
    <hy-pulse state=${state}>
      <span
        style="display:block; width:12px; height:12px; border-radius:50%;
               background:var(--color-provenance-native); box-shadow:0 0 12px var(--color-provenance-native);"
      ></span>
    </hy-pulse>
  `,
};
export default meta;
type Story = StoryObj;

export const Watched: Story = {};
export const Active: Story = { args: { state: 'active' } };
export const Still: Story = { args: { state: 'still' } };

/** "Heartbeat, not weather" — provenance lights breathing to mean alive/connected. */
export const Provenance: Story = {
  render: () => html`
    <div style="display:flex; gap:28px; align-items:center;">
      ${[
        ['--color-provenance-native', 'On-device'],
        ['--color-provenance-cloud', 'Cloud'],
      ].map(
        ([c, t]) => html`
          <div style="display:flex; align-items:center; gap:10px; color:var(--color-text-secondary); font:14px var(--font-family-sans)">
            <hy-pulse state="watched"
              ><span style="display:block; width:10px; height:10px; border-radius:50%; background:var(${c}); box-shadow:0 0 12px var(${c});"></span
            ></hy-pulse>
            ${t}
          </div>
        `
      )}
    </div>
  `,
};
