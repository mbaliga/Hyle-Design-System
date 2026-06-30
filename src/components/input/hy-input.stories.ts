import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-input.js';

const meta: Meta = {
  title: 'Components/Input',
  component: 'hy-input',
  tags: ['autodocs'],
  argTypes: {
    size: { control: 'inline-radio', options: ['sm', 'md', 'lg'] },
    type: { control: 'select', options: ['text', 'email', 'password', 'number'] },
    disabled: { control: 'boolean' },
    required: { control: 'boolean' },
    label: { control: 'text' },
    placeholder: { control: 'text' },
    helperText: { control: 'text' },
    error: { control: 'text' },
  },
  args: {
    label: 'Email',
    placeholder: 'you@example.com',
    type: 'email',
    size: 'md',
    helperText: "We'll never share your address.",
    error: '',
    disabled: false,
    required: true,
  },
  render: ({ label, placeholder, type, size, helperText, error, disabled, required }) => html`
    <div style="max-width: 320px;">
      <hy-input
        label=${label}
        placeholder=${placeholder}
        type=${type}
        size=${size}
        helper-text=${helperText}
        error=${error}
        ?disabled=${disabled}
        ?required=${required}
      ></hy-input>
    </div>
  `,
};

export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const WithError: Story = { args: { error: 'Enter a valid email address.', helperText: '' } };
export const Disabled: Story = { args: { disabled: true, value: 'locked@example.com' } };

export const Sizes: Story = {
  render: () => html`
    <div style="display:flex; flex-direction:column; gap:16px; max-width:320px;">
      <hy-input size="sm" label="Small" placeholder="Small"></hy-input>
      <hy-input size="md" label="Medium" placeholder="Medium"></hy-input>
      <hy-input size="lg" label="Large" placeholder="Large"></hy-input>
    </div>
  `,
};
