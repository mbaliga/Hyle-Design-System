import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-key.js';

const meta: Meta = {
  title: 'Tactile/Key',
  component: 'hy-key',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    shape: { control: 'inline-radio', options: ['square', 'oval'] },
    mode: { control: 'inline-radio', options: ['momentary', 'toggle'] },
    led: { control: 'boolean' },
    pressed: { control: 'boolean' },
    disabled: { control: 'boolean' },
  },
  args: { shape: 'square', mode: 'momentary', led: false, pressed: false, disabled: false },
  render: ({ shape, mode, led, pressed, disabled }) => html`
    <hy-key shape=${shape} mode=${mode} ?led=${led} ?pressed=${pressed} ?disabled=${disabled}>
      <svg viewBox="0 0 24 24"><path d="M8 6.5v11l9-5.5z" /></svg>
    </hy-key>
  `,
};
export default meta;
type Story = StoryObj;

export const Momentary: Story = {};
export const Toggle: Story = { args: { mode: 'toggle', led: true } };
export const Oval: Story = { args: { shape: 'oval' } };

export const Bank: Story = {
  render: () => html`
    <div style="display:flex; gap:10px;">
      ${['M', 'S', '1', '2'].map(
        (l) => html`<hy-key mode="toggle" led><span style="font:600 13px var(--font-family-sans)">${l}</span></hy-key>`
      )}
    </div>
  `,
};
