import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-digital-display.js';

const VARIANTS = ['halo', 'led'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

/**
 * The kit's digital matrix displays — a halftone metaball `halo` and a
 * Tidbyt-style LED `led` ticker. Lifted verbatim from the Tactile Kit's Digital
 * Displays. Both tint to the active accent.
 */
const meta: Meta = {
  title: 'Displays/Digital Display',
  component: 'hy-digital-display',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { variant: { control: 'inline-radio', options: VARIANTS } },
  args: { variant: 'halo' },
  render: ({ variant }) => html`
    <div style="width:320px">
      <hy-digital-display variant=${variant}></hy-digital-display>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; flex-direction:column; gap:20px; width:320px">
      ${VARIANTS.map(
        (v) => html`
          <div style="display:flex; flex-direction:column; gap:10px">
            <hy-digital-display variant=${v}></hy-digital-display>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
