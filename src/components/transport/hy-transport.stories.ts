import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-transport.js';

const meta: Meta = {
  title: 'Tactile/Transport',
  component: 'hy-transport',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: { playing: { control: 'boolean' } },
  args: { playing: false },
  render: ({ playing }) => html`
    <hy-transport
      ?playing=${playing}
      @hy-transport=${(e: CustomEvent) => console.log('transport', e.detail.action)}
    ></hy-transport>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const Playing: Story = { args: { playing: true } };
export const Minimal: Story = {
  render: () => html`<hy-transport .actions=${['prev', 'play', 'next']}></hy-transport>`,
};
