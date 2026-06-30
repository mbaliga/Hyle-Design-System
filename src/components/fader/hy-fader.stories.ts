import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-fader.js';

const meta: Meta = {
  title: 'Tactile/Fader',
  component: 'hy-fader',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    min: { control: 'number' },
    max: { control: 'number' },
    value: { control: 'number' },
    height: { control: { type: 'range', min: 100, max: 260, step: 4 } },
    disabled: { control: 'boolean' },
  },
  args: { min: 0, max: 100, value: 60, height: 168, disabled: false },
  render: ({ min, max, value, height, disabled }) => html`
    <hy-fader
      min=${min}
      max=${max}
      value=${value}
      height=${height}
      ?disabled=${disabled}
      @hy-input=${(e: CustomEvent) => console.log('fader', Math.round(e.detail.value))}
    ></hy-fader>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const Disabled: Story = { args: { disabled: true } };
