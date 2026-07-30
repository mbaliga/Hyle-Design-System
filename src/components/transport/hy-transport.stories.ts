import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-transport.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Transport',
  component: 'hy-transport',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  render: () => html`
    <div style="width:420px">
      <hy-transport
        @hy-change=${(e: CustomEvent) => console.log('transport', e.detail.action)}
      ></hy-transport>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:26px; flex-wrap:wrap; align-items:flex-end">
      <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
        <div style="width:420px"><hy-transport></hy-transport></div>
        <div style=${capStyle}>transport</div>
      </div>
    </div>
  `,
};
