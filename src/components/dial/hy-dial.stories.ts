import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-dial.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Tactile/Dial',
  component: 'hy-dial',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    min: { control: 'number' },
    max: { control: 'number' },
    value: { control: 'number' },
  },
  args: { min: 0, max: 100, value: 60 },
  render: ({ min, max, value }) => html`
    <hy-dial
      min=${min}
      max=${max}
      value=${value}
      @hy-input=${(e: CustomEvent) => console.log('dial', Math.round(e.detail.value))}
      @hy-change=${(e: CustomEvent) => console.log('dial change', Math.round(e.detail.value))}
    ></hy-dial>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:26px; flex-wrap:wrap; align-items:flex-end">
      ${[20, 60, 90].map(
        (v) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
            <hy-dial value=${v}></hy-dial>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
