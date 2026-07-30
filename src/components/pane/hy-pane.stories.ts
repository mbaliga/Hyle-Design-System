import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-pane.js';
import '../chip/hy-chip.js';

const meta: Meta = {
  title: 'Surfaces/Pane',
  component: 'hy-pane',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    heading: { control: 'text' },
    dock: { control: 'inline-radio', options: ['floating', 'bottom'] },
    hiddenPane: { control: 'boolean' },
  },
  args: { heading: 'Surface', dock: 'floating', hiddenPane: false },
  render: ({ heading, dock, hiddenPane }) => html`
    <div style="width:340px; padding:24px;">
      <hy-pane heading=${heading} dock=${dock} ?hidden-pane=${hiddenPane}>
        <div style="display:flex; gap:7px; flex-wrap:wrap;">
          <hy-chip pressed>Soil</hy-chip>
          <hy-chip>Hatch</hy-chip>
          <hy-chip>Warp</hy-chip>
          <hy-chip>Facet</hy-chip>
        </div>
      </hy-pane>
    </div>
  `,
};
export default meta;
type Story = StoryObj;

export const Floating: Story = {};
export const BottomSheet: Story = { args: { dock: 'bottom', heading: 'Form · World' } };
export const Headless: Story = { args: { heading: '' } };
