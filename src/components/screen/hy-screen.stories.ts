import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-screen.js';
import '../waveform/hy-waveform.js';

const meta: Meta = {
  title: 'Displays/Screen',
  component: 'hy-screen',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    clock: { control: 'text' },
    status: { control: 'text' },
    recording: { control: 'boolean' },
  },
  args: { clock: '04.35 PM', status: 'READY', recording: false },
  render: ({ clock, status, recording }) => html`
    <div style="width:320px">
      <hy-screen clock=${clock} status=${status} ?recording=${recording}>
        <hy-waveform live></hy-waveform>
        <div style="margin-top:10px; font:600 22px var(--font-family-mono); letter-spacing:.06em;">00:12:10</div>
      </hy-screen>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Ready: Story = {};
export const Recording: Story = { args: { recording: true } };
