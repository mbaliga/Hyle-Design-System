import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-fader.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Fader',
  component: 'hy-fader',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    value: { control: { type: 'range', min: 0, max: 100, step: 1 } },
  },
  args: { value: 72 },
  render: ({ value }) => html`
    <div style="height:220px">
      <hy-fader
        value=${value}
        @hy-input=${(e: CustomEvent) => console.log('fader', Math.round(e.detail.value))}
        @hy-change=${(e: CustomEvent) => console.log('fader change', Math.round(e.detail.value))}
      ></hy-fader>
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
      ${[20, 50, 72, 95].map(
        (v) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
            <div style="height:220px"><hy-fader value=${v}></hy-fader></div>
            <div style=${capStyle}>${v}</div>
          </div>
        `
      )}
    </div>
  `,
};
