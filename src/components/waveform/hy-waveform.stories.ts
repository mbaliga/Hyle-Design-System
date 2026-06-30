import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-waveform.js';

const meta: Meta = {
  title: 'Displays/Waveform',
  component: 'hy-waveform',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { live: { control: 'boolean' }, bars: { control: { type: 'range', min: 16, max: 96, step: 4 } } },
  args: { live: false, bars: 48 },
  render: ({ live, bars }) => html`
    <div style="width:320px"><hy-waveform ?live=${live} bars=${bars}></hy-waveform></div>
  `,
};
export default meta;
type Story = StoryObj;

export const Static: Story = {};
export const Live: Story = { args: { live: true } };
