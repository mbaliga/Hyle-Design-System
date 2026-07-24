import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import '../src/components/index.js';

/**
 * Frosted glass over a living surface — the core Hyle move: state is SHOWN by
 * material, not said. A shifting surface animates beneath; `hy-pane` floats over
 * it as the readable layer, its 18px backdrop-blur turning the motion into a
 * calm, legible frost. The pane is still; only the world behind it moves.
 */
const meta: Meta = {
  title: 'Showcase',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'field' },
    options: { showPanel: false },
  },
};
export default meta;
type Story = StoryObj;

// Relative src so it resolves under the GitHub Pages project sub-path.
const scene = (src: string, title: string, bg: string) => html`
  <div style="position:relative; width:100%; height:100vh; overflow:hidden; background:${bg}">
    <iframe
      src=${src}
      title=${title}
      style="position:absolute; inset:0; border:0; width:100%; height:100%; display:block;"
    ></iframe>
    <div
      style="position:absolute; inset:0; display:flex; align-items:center; justify-content:center; padding:24px; pointer-events:none"
    >
      <hy-pane
        heading="System State"
        style="width:min(420px, 92vw); pointer-events:auto"
      >
        <div style="display:flex; flex-direction:column; gap:14px">
          <div style="font:400 14px/1.6 var(--font-family-sans, system-ui); color:rgba(236,232,228,.92)">
            The surface behind this pane never stops shifting. The glass does not
            fight it — it blurs the motion into a still, readable frost. Nothing
            here is <em>said</em> to be legible; the material makes it so.
          </div>
          <div style="display:flex; gap:8px; flex-wrap:wrap">
            <hy-chip pressed>Live</hy-chip>
            <hy-chip>Buffered</hy-chip>
            <hy-chip>Idle</hy-chip>
          </div>
          <hy-button variant="primary">Commit</hy-button>
        </div>
      </hy-pane>
    </div>
  </div>
`;

/** The frosted pane over the dynamic texture generator's shifting surface. */
export const GlassOverTexture: Story = {
  name: 'Glass over Texture',
  render: () => scene('generators/texture-surface.html', 'Shifting texture surface', '#0e0e12'),
};

/** The frosted pane over the living Field (Form-World, defaulting to the Helix). */
export const GlassOverField: Story = {
  name: 'Glass over Field',
  render: () => scene('field/form-world-bg.html', 'The living Field', '#000'),
};
