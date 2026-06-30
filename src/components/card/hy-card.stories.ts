import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-card.js';
import '../button/hy-button.js';

const meta: Meta = {
  title: 'Components/Card',
  component: 'hy-card',
  tags: ['autodocs'],
  argTypes: {
    elevation: { control: 'inline-radio', options: ['flat', 'sm', 'md', 'lg'] },
  },
  args: { elevation: 'sm' },
  render: ({ elevation }) => html`
    <hy-card elevation=${elevation} style="max-width: 360px;">
      <h3 slot="header">Project settings</h3>
      <p>Configure how your workspace behaves. Tokens flow from a single source to every surface.</p>
      <div slot="footer" style="display:flex; gap:8px; justify-content:flex-end;">
        <hy-button variant="ghost" size="sm">Cancel</hy-button>
        <hy-button variant="primary" size="sm">Save</hy-button>
      </div>
    </hy-card>
  `,
};

export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const Flat: Story = { args: { elevation: 'flat' } };
export const Elevated: Story = { args: { elevation: 'lg' } };

export const BodyOnly: Story = {
  render: () => html`
    <hy-card style="max-width: 360px;">
      <p style="margin:0;">A card with body content only — header and footer collapse automatically.</p>
    </hy-card>
  `,
};

export const Elevations: Story = {
  render: () => html`
    <div style="display:flex; gap:24px; flex-wrap:wrap;">
      ${['flat', 'sm', 'md', 'lg'].map(
        (e) => html`
          <hy-card elevation=${e} style="width: 180px;">
            <strong slot="header">${e}</strong>
            <p style="margin:0;">Elevation token.</p>
          </hy-card>
        `
      )}
    </div>
  `,
};
