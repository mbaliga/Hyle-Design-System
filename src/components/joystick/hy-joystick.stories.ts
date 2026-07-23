import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-joystick.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Joystick',
  component: 'hy-joystick',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  render: () => html`
    <hy-joystick
      @hy-input=${(e: CustomEvent) => console.log('joy', e.detail.x.toFixed(2), e.detail.y.toFixed(2))}
      @hy-change=${(e: CustomEvent) => console.log('joy change', e.detail.x, e.detail.y)}
    ></hy-joystick>
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
        <hy-joystick></hy-joystick>
        <div style=${capStyle}>joystick</div>
      </div>
    </div>
  `,
};
