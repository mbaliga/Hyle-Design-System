import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-color-picker.js';
import '../button/hy-button.js';
import '../chip/hy-chip.js';
import '../knob/hy-knob.js';
import '../toggle/hy-toggle.js';
import '../slider/hy-slider.js';
import { applyTheme } from '../../theme/theme.js';

const meta: Meta = {
  title: 'Foundations/Colour Picker',
  component: 'hy-color-picker',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    value: { control: 'color' },
    space: { control: 'inline-radio', options: ['hsl', 'hsv', 'oklch'] },
    paletteCount: { control: { type: 'range', min: 3, max: 10 } },
    colorBlind: { control: 'boolean' },
  },
  args: { value: '#8e7bff', space: 'hsl', paletteCount: 6, colorBlind: false },
  render: ({ value, space, paletteCount, colorBlind }) => html`
    <hy-color-picker
      value=${value}
      space=${space}
      palette-count=${paletteCount}
      ?color-blind=${colorBlind}
      @hy-change=${(e: CustomEvent) => console.log('colour', e.detail.hex)}
    ></hy-color-picker>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};
export const OKLCH: Story = { args: { space: 'oklch' } };

/**
 * The picker as the app's accent chooser — its output drives `applyTheme`, so
 * every component re-themes live, and the picker's own palette panel shows the
 * iwanthue supporting colours kept distinct from the accent.
 */
export const AsAccentPicker: Story = {
  parameters: { layout: 'fullscreen' },
  render: () => {
    const onPick = (e: CustomEvent) => {
      const root = document.getElementById('hy-accent-demo');
      if (root) applyTheme(e.detail.hex, { count: 6 }, root);
    };
    setTimeout(() => {
      const root = document.getElementById('hy-accent-demo');
      if (root) applyTheme('#8e7bff', { count: 6 }, root);
    }, 0);
    return html`
      <div
        id="hy-accent-demo"
        style="min-height:100vh; padding:28px; background:var(--color-palette-field-near);
               display:flex; gap:32px; align-items:flex-start; flex-wrap:wrap; font-family:var(--font-family-sans);"
      >
        <hy-color-picker value="#8e7bff" @hy-input=${onPick} @hy-change=${onPick}></hy-color-picker>
        <div style="display:flex; flex-direction:column; gap:18px; color:var(--color-text-primary);">
          <div style="font-size:10px; letter-spacing:.2em; text-transform:uppercase; color:var(--color-text-faint);">
            Everything follows the accent
          </div>
          <div style="display:flex; gap:14px; align-items:center; flex-wrap:wrap;">
            <hy-button variant="primary">Primary</hy-button>
            <hy-chip pressed>Chip</hy-chip>
            <hy-toggle on></hy-toggle>
            <hy-knob value="62" size="56"></hy-knob>
            <div style="width:180px"><hy-slider value="60"></hy-slider></div>
          </div>
        </div>
      </div>
    `;
  },
};
