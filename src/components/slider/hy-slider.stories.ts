import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-slider.js';

const meta: Meta = {
  title: 'Tactile/Slider',
  component: 'hy-slider',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'inline-radio', options: ['channel', 'hairline', 'minimal'] },
    origin: { control: 'inline-radio', options: ['start', 'center'] },
    value: { control: { type: 'range', min: 0, max: 100 } },
    disabled: { control: 'boolean' },
  },
  args: { variant: 'channel', origin: 'start', value: 60, disabled: false },
  render: ({ variant, origin, value, disabled }) => html`
    <div style="width:280px">
      <hy-slider variant=${variant} origin=${origin} value=${value} ?disabled=${disabled}></hy-slider>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Channel: Story = {};
export const Pan: Story = { args: { variant: 'hairline', origin: 'center', value: 50 } };
export const Minimal: Story = { args: { variant: 'minimal', value: 45 } };
