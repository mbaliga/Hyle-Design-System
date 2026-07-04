import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-key.js';

const VARIANTS = ['square', 'oval', 'dot', 'press'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Surface Buttons',
  component: 'hy-key',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    on: { control: 'boolean' },
  },
  args: { variant: 'square', on: false },
  render: ({ variant, on }) => html`
    <hy-key
      variant=${variant}
      ?on=${on}
      @hy-change=${(e: CustomEvent) => console.log('key', e.detail.value)}
    ></hy-key>
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
            <div style="display:flex; gap:16px; align-items:flex-end">
              <hy-key variant=${v}></hy-key>
              <hy-key variant=${v} on></hy-key>
            </div>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
