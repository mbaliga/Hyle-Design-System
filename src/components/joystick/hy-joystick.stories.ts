import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-joystick.js';

const meta: Meta = {
  title: 'Tactile/Joystick',
  component: 'hy-joystick',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    sticky: { control: 'boolean' },
    size: { control: { type: 'range', min: 90, max: 200, step: 5 } },
    disabled: { control: 'boolean' },
  },
  args: { sticky: false, size: 120, disabled: false },
  render: ({ sticky, size, disabled }) => html`
    <hy-joystick
      ?sticky=${sticky}
      size=${size}
      ?disabled=${disabled}
      @hy-move=${(e: CustomEvent) => console.log('joy', e.detail.x.toFixed(2), e.detail.y.toFixed(2))}
    ></hy-joystick>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const Sticky: Story = { args: { sticky: true } };
