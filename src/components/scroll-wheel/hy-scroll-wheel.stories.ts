import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-scroll-wheel.js';

const meta: Meta = {
  title: 'Controls/Scroll Wheel',
  component: 'hy-scroll-wheel',
  tags: ['autodocs'],
  parameters: {
    backgrounds: { default: 'housing', values: [{ name: 'housing', value: '#ececea' }] },
  },
  argTypes: {
    pitch: { control: { type: 'range', min: 0.012, max: 0.042, step: 0.001 } },
    haptics: { control: 'boolean' },
    sound: { control: 'boolean' },
    disabled: { control: 'boolean' },
  },
  args: { pitch: 0.024, haptics: true, sound: true, disabled: false },
  render: ({ pitch, haptics, sound, disabled }) => html`
    <div style="height:62vh; display:flex; justify-content:center;">
      <hy-scroll-wheel
        pitch=${pitch}
        ?haptics=${haptics}
        ?sound=${sound}
        ?disabled=${disabled}
        style="height:100%;"
      ></hy-scroll-wheel>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

/**
 * Drag / scroll / arrow-keys the wheel; each ridge crossing fires a haptic buzz
 * (on supporting devices) and a soft mechanical click. The value tracks detents.
 */
export const WithReadout: Story = {
  render: () => {
    const onChange = (e: CustomEvent) => {
      const out = document.getElementById('sw-out');
      if (out) out.textContent = String(e.detail.value);
    };
    return html`
      <div style="height:62vh; display:flex; align-items:center; justify-content:center; gap:40px;">
        <hy-scroll-wheel
          style="height:100%;"
          @hy-input=${onChange}
          @hy-change=${onChange}
        ></hy-scroll-wheel>
        <div style="font:600 64px ui-sans-serif,system-ui,sans-serif; color:#75776f; font-variant-numeric:tabular-nums;">
          <span id="sw-out">0</span>
        </div>
      </div>
    `;
  },
};

/** A bounded wheel — value clamps between min and max. */
export const Bounded: Story = {
  render: () => html`
    <div style="height:62vh; display:flex; justify-content:center;">
      <hy-scroll-wheel min="0" max="20" value="10" style="height:100%;"></hy-scroll-wheel>
    </div>
  `,
};
