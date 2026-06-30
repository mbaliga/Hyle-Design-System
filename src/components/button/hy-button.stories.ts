import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-button.js';

const meta: Meta = {
  title: 'Controls/Button',
  component: 'hy-button',
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'ghost', 'danger'],
    },
    size: { control: 'inline-radio', options: ['sm', 'md', 'lg'] },
    disabled: { control: 'boolean' },
    fullWidth: { control: 'boolean' },
    label: { control: 'text' },
  },
  args: {
    variant: 'primary',
    size: 'md',
    disabled: false,
    fullWidth: false,
    label: 'Button',
  },
  render: ({ variant, size, disabled, fullWidth, label }) => html`
    <hy-button
      variant=${variant}
      size=${size}
      ?disabled=${disabled}
      ?full-width=${fullWidth}
      @hy-click=${() => console.log('hy-click')}
    >
      ${label}
    </hy-button>
  `,
};

export default meta;
type Story = StoryObj;

export const Primary: Story = {};
export const Secondary: Story = { args: { variant: 'secondary' } };
export const Ghost: Story = { args: { variant: 'ghost' } };
export const Danger: Story = { args: { variant: 'danger' } };
export const Disabled: Story = { args: { disabled: true } };

export const AllVariants: Story = {
  render: () => html`
    <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
      <hy-button variant="primary">Primary</hy-button>
      <hy-button variant="secondary">Secondary</hy-button>
      <hy-button variant="ghost">Ghost</hy-button>
      <hy-button variant="danger">Danger</hy-button>
    </div>
  `,
};

export const Sizes: Story = {
  render: () => html`
    <div style="display:flex; gap:12px; align-items:center;">
      <hy-button size="sm">Small</hy-button>
      <hy-button size="md">Medium</hy-button>
      <hy-button size="lg">Large</hy-button>
    </div>
  `,
};
