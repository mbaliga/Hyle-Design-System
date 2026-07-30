import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import '../src/components/index.js';

/**
 * The signature artifacts — the real, living pieces Hyle is built from, shown in
 * full rather than described. The individual controls each have their own entry
 * under `Controls/`; this section is the full assembled demos.
 */
const meta: Meta = {
  title: 'Showcase',
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

/** The tactile controls in full assembly, with the native orange default + user-selectable accent. */
export const TactileKit: Story = {
  name: 'The Kit — Full Assembly',
  render: () => frame('kit/tactile-kit.html', 'The Kit'),
};
