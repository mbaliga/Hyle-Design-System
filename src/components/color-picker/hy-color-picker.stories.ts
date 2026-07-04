import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-color-picker.js';
import '../button/hy-button.js';
import '../chip/hy-chip.js';
import '../knob/hy-knob.js';
import '../toggle/hy-toggle.js';
import '../slider/hy-slider.js';
import { applyTheme } from '../../theme/theme.js';

/**
 * The 3D colour picker — the Tactile Kit's own THREE.js picker, lifted verbatim
 * and mounted in an iframe so its code runs byte-for-byte unchanged. Switch the
 * space (RGB / HSV / Lab / HCL), drag the hue ring or slice, spin the 3D model,
 * and build a palette. It reports the chosen hex through `hy-change`.
 */
const meta: Meta = {
  title: 'Foundations/Colour Picker',
  component: 'hy-color-picker',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
  argTypes: {
    value: { control: 'color' },
  },
  args: { value: '#8e7bff' },
  render: ({ value }) => html`
    <hy-color-picker
      value=${value}
      @hy-change=${(e: CustomEvent) => console.log('colour', e.detail.value)}
    ></hy-color-picker>
  `,
};
export default meta;
type Story = StoryObj;

export const Default: Story = {};

/**
 * The picker as the app's accent chooser — its output drives `applyTheme`, so
 * every component re-themes live to the chosen accent.
 */
export const AsAccentPicker: Story = {
  parameters: { layout: 'fullscreen', controls: { disable: true } },
  render: () => {
    const onPick = (e: CustomEvent) => {
      const root = document.getElementById('hy-accent-demo');
      if (root) applyTheme(e.detail.value, { count: 6 }, root);
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
        <hy-color-picker value="#8e7bff" @hy-change=${onPick}></hy-color-picker>
        <div style="display:flex; flex-direction:column; gap:18px; color:var(--color-text-primary);">
          <div style="font-size:10px; letter-spacing:.2em; text-transform:uppercase; color:var(--color-text-faint);">
            Everything follows the accent
          </div>
          <div style="display:flex; gap:22px; align-items:flex-end; flex-wrap:wrap;">
            <hy-knob variant="precision" value="62"></hy-knob>
            <hy-toggle variant="standard" on></hy-toggle>
            <hy-toggle variant="smooth" on></hy-toggle>
            <div style="width:220px"><hy-slider variant="channel" value="60"></hy-slider></div>
          </div>
        </div>
      </div>
    `;
  },
};
