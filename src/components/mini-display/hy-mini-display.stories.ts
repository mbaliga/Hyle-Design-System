import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-mini-display.js';

const KINDS = ['eq', 'wave', 'pulse'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

/**
 * The kit's tiny LED dot-matrix displays — a bouncing spectrum (`eq`), a
 * travelling `wave`, and an expanding `pulse` ring. Lifted verbatim from the
 * Tactile Kit's Mini Displays. Read-only: the motion is the state.
 */
const meta: Meta = {
  title: 'Displays/Mini Display',
  component: 'hy-mini-display',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { kind: { control: 'inline-radio', options: KINDS } },
  args: { kind: 'eq' },
  render: ({ kind }) => html`<hy-mini-display kind=${kind}></hy-mini-display>`,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div class="mini-row" style="display:flex; gap:16px; align-items:flex-end">
      ${KINDS.map(
        (k) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
            <hy-mini-display kind=${k}></hy-mini-display>
            <div style=${capStyle}>${k}</div>
          </div>
        `
      )}
    </div>
  `,
};
