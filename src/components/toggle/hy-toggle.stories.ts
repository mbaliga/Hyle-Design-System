import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-toggle.js';

const VARIANTS = ['standard', 'flip', 'gain', 'smooth', 'big', 'square'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Toggle',
  component: 'hy-toggle',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    on: { control: 'boolean' },
    pos: { control: { type: 'inline-radio', options: [0, 1, 2] } },
  },
  args: { variant: 'standard', on: false, pos: 0 },
  render: ({ variant, on, pos }) => html`
    <hy-toggle
      variant=${variant}
      ?on=${on}
      pos=${pos}
      @hy-change=${(e: CustomEvent) => console.log('toggle', e.detail.value)}
    ></hy-toggle>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:26px; flex-wrap:wrap; align-items:flex-end">
      ${VARIANTS.map((v) =>
        v === 'gain'
          ? html`
              <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
                <div style="display:flex; gap:16px; align-items:flex-end">
                  <hy-toggle variant="gain" pos=${0}></hy-toggle>
                  <hy-toggle variant="gain" pos=${1}></hy-toggle>
                  <hy-toggle variant="gain" pos=${2}></hy-toggle>
                </div>
                <div style=${capStyle}>gain</div>
              </div>
            `
          : html`
              <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
                <div style="display:flex; gap:16px; align-items:flex-end">
                  <hy-toggle variant=${v}></hy-toggle>
                  <hy-toggle variant=${v} on></hy-toggle>
                </div>
                <div style=${capStyle}>${v}</div>
              </div>
            `
      )}
    </div>
  `,
};
