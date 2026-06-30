import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-surface.js';

const meta: Meta = {
  title: 'Surfaces/Panel',
  parameters: { backgrounds: { default: 'near' } },
};
export default meta;
type Story = StoryObj;

export const Grille: Story = {
  render: () => html`<div style="width:320px"><hy-grille height="72"></hy-grille></div>`,
};

export const Jacks: Story = {
  render: () => html`
    <div style="display:flex; gap:20px; align-items:center;">
      ${['In L', 'In R', 'Out'].map(
        (l) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:8px;">
            <hy-jack></hy-jack>
            <span style="font:600 var(--font-size-micro) var(--font-family-sans); letter-spacing:var(--font-tracking-label); text-transform:uppercase; color:var(--color-text-faint)">${l}</span>
          </div>
        `
      )}
    </div>
  `,
};
