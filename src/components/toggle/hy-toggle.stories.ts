import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-toggle.js';

const meta: Meta = {
  title: 'Tactile/Toggle',
  component: 'hy-toggle',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    on: { control: 'boolean' },
    disabled: { control: 'boolean' },
  },
  args: { on: false, disabled: false },
  render: ({ on, disabled }) => html`
    <hy-toggle
      ?on=${on}
      ?disabled=${disabled}
      @hy-change=${(e: CustomEvent) => console.log('toggle', e.detail.on)}
    ></hy-toggle>
  `,
};
export default meta;
type Story = StoryObj;

export const Off: Story = {};
export const On: Story = { args: { on: true } };
export const Disabled: Story = { args: { disabled: true } };
