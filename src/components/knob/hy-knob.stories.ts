import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-knob.js';

const VARIANTS = ['precision', 'standard', 'minimal', 'dial'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Knob',
  component: 'hy-knob',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    min: { control: 'number' },
    max: { control: 'number' },
    value: { control: 'number' },
  },
  args: { variant: 'precision', min: 0, max: 100, value: 40 },
  render: ({ variant, min, max, value }) => html`
    <hy-knob
      variant=${variant}
      min=${min}
      max=${max}
      value=${value}
      @hy-input=${(e: CustomEvent) => console.log('knob', Math.round(e.detail.value))}
      @hy-change=${(e: CustomEvent) => console.log('knob change', Math.round(e.detail.value))}
    ></hy-knob>
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
            <hy-knob variant=${v} value=${40}></hy-knob>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
