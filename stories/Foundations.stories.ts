import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';

const meta: Meta = {
  title: 'Foundations/Tokens',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'field' }, options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

const swatch = (name: string, note = '') => html`
  <div style="display:flex; flex-direction:column; gap:6px;">
    <div
      style="height:64px; border-radius:8px; border:1px solid var(--color-border-hairline);
             background: var(${name});"
    ></div>
    <code style="font-size:11px; color:var(--color-text-secondary);">${name}</code>
    ${note ? html`<span style="font-size:10px; color:var(--color-text-faint);">${note}</span>` : ''}
  </div>
`;

const section = (title: string, body: unknown) => html`
  <section style="margin-bottom:40px;">
    <h2 style="font:600 16px/1.2 var(--font-family-sans); color:var(--color-text-primary); letter-spacing:.06em; margin:0 0 16px;">
      ${title}
    </h2>
    ${body}
  </section>
`;

const grid = (children: unknown) => html`
  <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(160px, 1fr)); gap:16px;">
    ${children}
  </div>
`;

export const Colour: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans); color:var(--color-text-primary);">
      ${section(
        'Field — light is scarce',
        grid([
          swatch('--color-palette-field-void', 'art canvas'),
          swatch('--color-palette-field-near', 'behind glass'),
          swatch('--color-palette-field-raised', 'UI surface'),
          swatch('--color-palette-field-deep', 'fallback'),
        ])
      )}
      ${section(
        'Ink — spent sparingly',
        grid([
          swatch('--color-palette-ink-pure'),
          swatch('--color-text-primary', '0.92'),
          swatch('--color-text-secondary', '0.42'),
          swatch('--color-text-faint', '0.18'),
        ])
      )}
      ${section(
        'Accent & provenance',
        grid([
          swatch('--color-border-focus', 'focus ring'),
          swatch('--color-action-primary', 'violet #8E7BFF'),
          swatch('--color-provenance-native', 'on-device'),
          swatch('--color-provenance-cloud', 'cloud'),
        ])
      )}
      ${section(
        'Material — stone / smog / light',
        grid([
          swatch('--color-palette-material-stone'),
          swatch('--color-palette-material-smog'),
          swatch('--color-palette-material-light'),
        ])
      )}
      ${section(
        'Feedback',
        grid([
          swatch('--color-feedback-danger'),
          swatch('--color-feedback-warning'),
          swatch('--color-feedback-success'),
        ])
      )}
    </div>
  `,
};

export const Spacing: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans); color:var(--color-text-primary);">
      ${section(
        'Spacing scale',
        html`<div style="display:flex; flex-direction:column; gap:12px;">
          ${['1', '2', '3', '4', '5', '6', '8', '10', '12', '16'].map(
            (s) => html`
              <div style="display:flex; align-items:center; gap:16px;">
                <code style="width:90px; font-size:12px; color:var(--color-text-secondary);">spacing.${s}</code>
                <div style="height:16px; width:var(--spacing-${s}); background:var(--color-action-primary); border-radius:2px;"></div>
              </div>
            `
          )}
        </div>`
      )}
    </div>
  `,
};

export const Radius: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans); color:var(--color-text-primary);">
      ${section(
        'Corner radius',
        grid(
          ['none', 'sm', 'md', 'lg', 'xl'].map(
            (r) => html`
              <div style="display:flex; flex-direction:column; gap:6px;">
                <div style="height:72px; background:var(--color-palette-field-raised); border:1px solid var(--color-border-strong); border-radius:var(--radius-${r});"></div>
                <code style="font-size:12px; color:var(--color-text-secondary);">radius.${r}</code>
              </div>
            `
          )
        )
      )}
    </div>
  `,
};

export const Typography: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans); color:var(--color-text-primary);">
      ${section(
        'Type scale',
        html`<div style="display:flex; flex-direction:column; gap:16px;">
          ${['xs', 'sm', 'md', 'lg', 'xl', '2xl', '3xl'].map(
            (s) => html`
              <div style="display:flex; align-items:baseline; gap:24px;">
                <code style="width:90px; font-size:12px; color:var(--color-text-secondary);">size.${s}</code>
                <span style="font-size:var(--font-size-${s});">Matter behaving</span>
              </div>
            `
          )}
        </div>`
      )}
      ${section(
        'Micro-labels — uppercase, tracked',
        html`<div style="display:flex; flex-direction:column; gap:14px;">
          ${[
            ['micro', 'label', 'Scene'],
            ['label', 'title', 'Form · World'],
          ].map(
            ([size, track, text]) => html`
              <div style="display:flex; align-items:center; gap:24px;">
                <code style="width:170px; font-size:12px; color:var(--color-text-secondary);">size.${size} / tracking.${track}</code>
                <span
                  style="font-size:var(--font-size-${size}); letter-spacing:var(--font-tracking-${track}); text-transform:uppercase; color:var(--color-text-secondary);"
                  >${text}</span
                >
              </div>
            `
          )}
        </div>`
      )}
    </div>
  `,
};
