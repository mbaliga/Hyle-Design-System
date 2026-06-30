import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-knob.js';

const meta: Meta = {
  title: 'Tactile/Knob',
  component: 'hy-knob',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    min: { control: 'number' },
    max: { control: 'number' },
    value: { control: 'number' },
    size: { control: { type: 'range', min: 48, max: 140, step: 4 } },
    disabled: { control: 'boolean' },
  },
  args: { min: 0, max: 100, value: 40, size: 72, disabled: false },
  render: ({ min, max, value, size, disabled }) => html`
    <hy-knob
      min=${min}
      max=${max}
      value=${value}
      size=${size}
      ?disabled=${disabled}
      @hy-input=${(e: CustomEvent) => console.log('knob', Math.round(e.detail.value))}
    ></hy-knob>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const Large: Story = { args: { size: 120, value: 65 } };
export const Disabled: Story = { args: { disabled: true } };
