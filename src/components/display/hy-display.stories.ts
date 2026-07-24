import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-display.js';

/**
 * The kit's device display — a lit status bar (live clock, REC flag, play, wifi)
 * over a scrolling waveform and a running timer. Lifted verbatim from the
 * Tactile Kit's Display section. Click the REC flag to capture a live take;
 * the play button reviews it. State is shown by the material, never said.
 */
const meta: Meta = {
  title: 'Displays/Display',
  component: 'hy-display',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { recording: { control: 'boolean' } },
  args: { recording: false },
  render: ({ recording }) => html`
    <div style="width:340px">
      <hy-display
        ?recording=${recording}
        @hy-change=${(e: CustomEvent) => console.log('display recording:', e.detail.recording)}
      ></hy-display>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
