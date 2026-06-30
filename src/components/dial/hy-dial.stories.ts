import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-dial.js';

const meta: Meta = {
  title: 'Tactile/Dial',
  component: 'hy-dial',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    index: { control: { type: 'number', min: 0 } },
    size: { control: { type: 'range', min: 56, max: 120, step: 4 } },
    disabled: { control: 'boolean' },
  },
  args: { index: 0, size: 72, disabled: false },
  render: ({ index, size, disabled }) => html`
    <hy-dial
      .options=${['O', 'I', 'II']}
      index=${index}
      size=${size}
      ?disabled=${disabled}
      @hy-change=${(e: CustomEvent) => console.log('dial', e.detail.value)}
    ></hy-dial>
  `,
};
export default meta;
type Story = StoryObj;

export const Gain: Story = {};
export const Modes: Story = {
  render: () => html`
    <hy-dial .options=${['Mic', 'Line', 'Hi-Z', 'Phantom']} index="1" size="92"></hy-dial>
  `,
};
