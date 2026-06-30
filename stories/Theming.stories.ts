import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import { applyTheme, randomAccent } from '../src/theme/theme.js';
import '../src/components/index.js';

const ROOT = 'hy-theme-root';

function repaint() {
  const root = document.getElementById(ROOT) as HTMLElement | null;
  if (!root) return;
  const accent = (document.getElementById('hy-accent') as HTMLInputElement)?.value ?? '#8e7bff';
  const count = Number((document.getElementById('hy-count') as HTMLInputElement)?.value ?? 6);
  const cb = (document.getElementById('hy-cb') as HTMLInputElement)?.checked ?? false;
  const theme = applyTheme(accent, { count, colorBlind: cb }, root);
  const strip = document.getElementById('hy-support');
  if (strip) {
    strip.innerHTML = theme.support
      .map(
        (c) =>
          `<div title="${c}" style="height:52px;border-radius:8px;background:${c};border:1px solid var(--color-border-hairline)"></div>`
      )
      .join('');
  }
}

function surprise() {
  const input = document.getElementById('hy-accent') as HTMLInputElement | null;
  if (input) input.value = randomAccent(String(Math.floor(performance.now())));
  repaint();
}

const meta: Meta = {
  title: 'Foundations/Theming',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'near' }, options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

/**
 * The accent is always user-selectable. Pick any colour — every component
 * re-themes (the primitive cascades through the semantic tokens) and iwanthue
 * generates a supporting palette that stays perceptually distinct from it.
 */
export const Accent: Story = {
  render: () => {
    setTimeout(repaint, 0);
    return html`
      <div
        id=${ROOT}
        style="min-height:100vh; padding:28px; background:var(--color-palette-field-near);
               font-family:var(--font-family-sans); color:var(--color-text-primary); display:flex; flex-direction:column; gap:24px;"
      >
        <div style="display:flex; align-items:center; gap:18px; flex-wrap:wrap;">
          <label style="display:flex; align-items:center; gap:8px; font-size:13px; color:var(--color-text-secondary);">
            Accent
            <input id="hy-accent" type="color" value="#8e7bff" @input=${repaint} style="width:44px; height:30px; background:none; border:1px solid var(--color-border-hairline); border-radius:6px;" />
          </label>
          <label style="display:flex; align-items:center; gap:8px; font-size:13px; color:var(--color-text-secondary);">
            Supporting
            <input id="hy-count" type="range" min="3" max="10" value="6" @input=${repaint} />
          </label>
          <label style="display:flex; align-items:center; gap:8px; font-size:13px; color:var(--color-text-secondary);">
            <input id="hy-cb" type="checkbox" @input=${repaint} /> colour-blind-safe
          </label>
          <hy-button variant="secondary" size="sm" @hy-click=${surprise}>Surprise me</hy-button>
        </div>

        <div>
          <div style="font-size:10px; letter-spacing:.2em; text-transform:uppercase; color:var(--color-text-faint); margin-bottom:10px;">Components follow the accent</div>
          <div style="display:flex; gap:16px; align-items:center; flex-wrap:wrap;">
            <hy-button variant="primary">Primary</hy-button>
            <hy-chip pressed>Chip</hy-chip>
            <hy-toggle on></hy-toggle>
            <hy-knob value="62" size="56"></hy-knob>
            <div style="width:180px"><hy-slider value="60"></hy-slider></div>
            <hy-meter live></hy-meter>
          </div>
        </div>

        <div>
          <div style="font-size:10px; letter-spacing:.2em; text-transform:uppercase; color:var(--color-text-faint); margin-bottom:10px;">iwanthue supporting palette — distinct from the accent &amp; each other</div>
          <div id="hy-support" style="display:grid; grid-template-columns:repeat(auto-fill,minmax(80px,1fr)); gap:10px;"></div>
        </div>

        <div style="height:240px;">
          <hy-field activity="0.45" style="height:100%;"></hy-field>
        </div>
      </div>
    `;
  },
};
