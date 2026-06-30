import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-chip.js';

const meta: Meta = {
  title: 'Controls/Chip',
  component: 'hy-chip',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    pressed: { control: 'boolean' },
    disabled: { control: 'boolean' },
  },
  args: { pressed: false, disabled: false },
  render: ({ pressed, disabled }) => html`
    <hy-chip ?pressed=${pressed} ?disabled=${disabled} @hy-toggle=${(e: CustomEvent) => console.log(e.detail)}>
      Towers
    </hy-chip>
  `,
};
export default meta;
type Story = StoryObj;

export const Idle: Story = {};
export const Pressed: Story = { args: { pressed: true } };
export const Disabled: Story = { args: { disabled: true } };

/** A single-select group, as used in Form-World's Scene picker. */
export const Group: Story = {
  render: () => html`
    <div
      style="display:flex; gap:7px; flex-wrap:wrap; max-width:320px;"
      @hy-toggle=${(e: CustomEvent) => {
        const group = (e.currentTarget as HTMLElement).querySelectorAll('hy-chip');
        group.forEach((c) => ((c as HTMLElement & { pressed: boolean }).pressed = c === e.target));
      }}
    >
      <hy-chip pressed>Bowl</hy-chip>
      <hy-chip>Helix</hy-chip>
      <hy-chip>Towers</hy-chip>
      <hy-chip>Arch</hy-chip>
      <hy-chip>Ruins</hy-chip>
      <hy-chip>Planet</hy-chip>
    </div>
  `,
};
