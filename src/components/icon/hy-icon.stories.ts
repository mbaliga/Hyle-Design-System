import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-icon.js';
import { ICON_NAMES } from './icons.js';

const meta: Meta = {
  title: 'Foundations/Icons',
  component: 'hy-icon',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    name: { control: 'select', options: ICON_NAMES },
    size: { control: { type: 'range', min: 16, max: 64, step: 2 } },
  },
  args: { name: 'bolt', size: 32 },
  render: ({ name, size }) => html`<hy-icon name=${name} size=${size} style="color:var(--color-text-primary)"></hy-icon>`,
};
export default meta;
type Story = StoryObj;

export const Single: Story = {};

/** The full sharp set — blocky, miter-cornered, built for legibility. */
export const Gallery: Story = {
  render: () => html`
    <div style="display:grid; grid-template-columns:repeat(auto-fill,minmax(96px,1fr)); gap:14px; color:var(--color-text-primary); font-family:var(--font-family-sans);">
      ${ICON_NAMES.map(
        (n) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:8px; padding:14px; border:1px solid var(--color-border-hairline); border-radius:8px;">
            <hy-icon name=${n} size="28"></hy-icon>
            <code style="font-size:10px; color:var(--color-text-faint);">${n}</code>
          </div>
        `
      )}
    </div>
  `,
};

/** On an accent button — icons inherit currentColor. */
export const OnAccent: Story = {
  render: () => html`
    <div style="display:flex; gap:8px; align-items:center; padding:10px 14px; border-radius:8px; background:var(--color-action-primary); color:var(--color-action-on-primary);">
      <hy-icon name="bolt" size="20"></hy-icon>
      <span style="font:600 14px var(--font-family-sans)">Accent</span>
    </div>
  `,
};
