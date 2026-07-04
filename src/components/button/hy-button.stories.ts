import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-button.js';

const VARIANTS = ['crater', 'power'] as const;
const ICONS = ['record', 'mix', 'save'] as const;
const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

const meta: Meta = {
  title: 'Controls/Button',
  component: 'hy-button',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    variant: { control: 'select', options: VARIANTS },
    icon: { control: 'select', options: ICONS },
    on: { control: 'boolean' },
  },
  args: { variant: 'crater', icon: 'record', on: false },
  render: ({ variant, icon, on }) =>
    variant === 'crater'
      ? html`
          <div style="width:96px;height:96px">
            <hy-button
              variant="crater"
              icon=${icon}
              ?on=${on}
              @hy-change=${(e: CustomEvent) => console.log('button', e.detail.value)}
            ></hy-button>
          </div>
        `
      : html`
          <hy-button
            variant="power"
            ?on=${on}
            @hy-change=${(e: CustomEvent) => console.log('button', e.detail.value)}
          ></hy-button>
        `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

export const Gallery: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:26px; flex-wrap:wrap; align-items:flex-end">
      ${ICONS.map(
        (ic) => html`
          <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
            <div style="width:96px;height:96px">
              <hy-button variant="crater" icon=${ic}></hy-button>
            </div>
            <div style=${capStyle}>crater · ${ic}</div>
          </div>
        `
      )}
      <div style="display:flex; flex-direction:column; align-items:center; gap:10px">
        <div style="display:flex; gap:16px; align-items:flex-end">
          <hy-button variant="power"></hy-button>
          <hy-button variant="power" on></hy-button>
        </div>
        <div style=${capStyle}>power</div>
      </div>
    </div>
  `,
};
