import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-vu.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Displays/VU',
  component: 'hy-vu',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  render: () => html`<div style="width:420px"><hy-vu></hy-vu></div>`,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:26px; flex-wrap:wrap; align-items:flex-end">
      <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
        <div style="width:420px"><hy-vu></hy-vu></div>
        <div style=${capStyle}>vu meter</div>
      </div>
    </div>
  `,
};
