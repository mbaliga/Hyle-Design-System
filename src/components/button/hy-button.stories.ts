import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-button.js';

const VARIANTS = ['primary', 'secondary', 'ghost', 'danger'] as const;
const SIZES = ['sm', 'md', 'lg'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

/**
 * The general-purpose labelled button used across app UIs, panes, and mock apps.
 * Accent-driven (`primary`), with `secondary` / `ghost` / `danger` variants and
 * three sizes. For the kit's physical push-buttons, see `hy-crater`.
 */
const meta: Meta = {
  title: 'Controls/Button',
  component: 'hy-button',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    size: { control: 'inline-radio', options: SIZES },
    disabled: { control: 'boolean' },
    fullWidth: { control: 'boolean' },
  },
  args: { variant: 'primary', size: 'md', disabled: false, fullWidth: false },
  render: ({ variant, size, disabled, fullWidth }) => html`
    <hy-button
      variant=${variant}
      size=${size}
      ?disabled=${disabled}
      ?full-width=${fullWidth}
      @hy-click=${() => console.log('button clicked')}
      >Button</hy-button
    >
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; flex-direction:column; gap:22px; align-items:flex-start">
      <div style="display:flex; gap:14px; align-items:center; flex-wrap:wrap">
        ${VARIANTS.map((v) => html`<hy-button variant=${v}>${v}</hy-button>`)}
        <hy-button variant="primary" disabled>disabled</hy-button>
      </div>
      <div style="display:flex; gap:14px; align-items:center">
        ${SIZES.map((s) => html`<hy-button size=${s}>${s}</hy-button>`)}
      </div>
      <div style=${capStyle}>variants · sizes</div>
    </div>
  `,
};
