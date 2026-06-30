import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-meter.js';
import './hy-vu.js';

const meta: Meta = {
  title: 'Tactile/Meters',
  parameters: { backgrounds: { default: 'near' } },
};
export default meta;
type Story = StoryObj;

export const Level: Story = {
  argTypes: {
    left: { control: { type: 'range', min: 0, max: 100 } },
    right: { control: { type: 'range', min: 0, max: 100 } },
    live: { control: 'boolean' },
  },
  args: { left: 62, right: 48, live: false },
  render: ({ left, right, live }) => html`
    <hy-meter left=${left} right=${right} ?live=${live}></hy-meter>
  `,
};

export const VU: Story = {
  argTypes: {
    value: { control: { type: 'range', min: 0, max: 100 } },
    live: { control: 'boolean' },
  },
  args: { value: 55, live: false },
  render: ({ value, live }) => html`<hy-vu value=${value} ?live=${live}></hy-vu>`,
};

export const Live: Story = {
  render: () => html`
    <div style="display:flex; gap:20px; align-items:flex-start; flex-wrap:wrap;">
      <hy-meter live></hy-meter>
      <hy-vu live></hy-vu>
    </div>
  `,
};
