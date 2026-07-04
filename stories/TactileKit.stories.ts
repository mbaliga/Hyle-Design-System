import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';

/**
 * Tactile Kit — Hyle's physical-control language ("soft brutalism": controls
 * you want to touch). A themeable hardware surface of knobs, faders, toggles,
 * crater buttons, a joystick, VU/level meters, transport, screens, and a full
 * colour picker — finished in machined metal, brushed, glass, sand, sandstone,
 * leather, or concrete.
 *
 * It is vendored verbatim under `kit/` and served here via iframe. Its accent
 * palette already includes the Hyle violet (#8e7bff) and it shares the calm
 * easing token cubic-bezier(.4,0,.2,1).
 */
const meta: Meta = {
  title: 'Material/Tactile Kit',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'field' },
    options: { showPanel: false },
  },
};
export default meta;
type Story = StoryObj;

const frame = (title: string) => html`
  <iframe
    src="kit/tactile-kit.html"
    title=${title}
    style="border:0; width:100%; height:100vh; display:block; background:#000;"
  ></iframe>
`;

/** The full kit. Use the sticky top bar to switch surface, accent, finish, and texture. */
export const Kit: Story = {
  render: () => frame('Hyle Tactile Kit'),
};
