import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-slider.js';

const VARIANTS = ['hairline', 'channel', 'minimal'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Slider',
  component: 'hy-slider',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    value: { control: { type: 'range', min: 0, max: 100, step: 1 } },
  },
  args: { variant: 'channel', value: 60 },
  render: ({ variant, value }) => html`
    <div style="width:420px">
      <hy-slider
        variant=${variant}
        value=${value}
        @hy-input=${(e: CustomEvent) => console.log('slider', Math.round(e.detail.value))}
        @hy-change=${(e: CustomEvent) => console.log('slider change', Math.round(e.detail.value))}
      ></hy-slider>
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
      ${VARIANTS.map(
        (v) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
            <div style="width:420px"><hy-slider variant=${v}></hy-slider></div>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
