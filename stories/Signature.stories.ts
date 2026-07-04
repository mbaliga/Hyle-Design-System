import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import '../src/components/index.js';

/**
 * The signature artifacts — the real, living pieces Hyle is built from, shown in
 * full rather than described. Leads the sidebar so the ethos is met before any
 * token or component.
 */
const meta: Meta = {
  title: 'Signature',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'field' }, options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

const frame = (src: string, title: string) => html`
  <iframe
    src=${src}
    title=${title}
    style="border:0; width:100%; height:100vh; display:block; background:#000;"
  ></iframe>
`;

/** The Form-World engine, full and interactive — the living background's source. */
export const FormWorldEngine: Story = {
  name: 'The Field — Form-World (interactive)',
  render: () => frame('field/form-world.html', 'Form-World engine'),
};

/** The Tactile Kit in full, with its native orange default + user-selectable accent. */
export const TactileKit: Story = {
  name: 'The Tactile Kit',
  render: () => frame('kit/tactile-kit.html', 'Tactile Kit'),
};

/** The haptic scroll wheel. Drag / scroll / arrow keys — a click per detent. */
export const ScrollWheel: Story = {
  name: 'The Scroll Wheel',
  render: () => html`
    <div style="height:100vh; display:flex; align-items:center; justify-content:center; background:#ececea;">
      <hy-scroll-wheel style="height:62vh;"></hy-scroll-wheel>
    </div>
  `,
};

/** The colour picker as the accent chooser. */
export const ColourPicker: Story = {
  name: 'The Colour Picker',
  render: () => html`
    <div style="min-height:100vh; display:flex; align-items:center; justify-content:center; background:#0a0809; padding:32px;">
      <hy-color-picker value="#8e7bff"></hy-color-picker>
    </div>
  `,
};
