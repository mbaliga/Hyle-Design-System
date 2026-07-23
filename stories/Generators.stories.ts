import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';

/**
 * Generators — the standalone authoring tools that produce Hyle brand artifacts.
 * Each is a complete, self-contained application, mounted here verbatim in an
 * iframe (the same faithful method the colour picker uses) so its own code runs
 * unchanged. Open one in a new tab for the full-screen experience.
 */
const meta: Meta = {
  title: 'Generators',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'near' },
    options: { showPanel: false },
  },
};
export default meta;
type Story = StoryObj;

// Relative src so it resolves against the host document — works at a site root
// and under a project sub-path (GitHub Pages serves under /<repo>/).
const tool = (src: string, title: string) => html`
  <iframe
    src=${src}
    title=${title}
    style="border:0; width:100%; height:100vh; display:block; background:#0e0e12;"
  ></iframe>
`;

/**
 * The cells logo generator — Voronoi cellular forms over grainy gradient
 * backdrops, in line with the *a system of cells* brand. Cell count, gap,
 * roundness, nucleus glyphs, per-cell colour overrides; exports PNG / SVG /
 * animated WebM.
 */
export const CellsLogo: Story = {
  name: 'Cells Logo',
  render: () => tool('generators/cells-logo.html', 'Cells Logo Generator'),
};

/**
 * The dynamic texture generator — grainy gradient surfaces (solid / linear /
 * radial / mesh) with grain, vignette, and live motion (drift / pulse /
 * shimmer / rotate / wave). Exports PNG or a seamless 4s WebM loop.
 */
export const Texture: Story = {
  name: 'Texture',
  render: () => tool('generators/texture.html', 'Texture Generator'),
};
