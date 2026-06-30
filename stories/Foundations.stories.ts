import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';

const meta: Meta = {
  title: 'Foundations/Tokens',
  parameters: { layout: 'fullscreen', options: { showPanel: false } },
};
export default meta;
type Story = StoryObj;

const swatch = (name: string) => html`
  <div style="display:flex; flex-direction:column; gap:6px;">
    <div
      style="height:56px; border-radius:8px; border:1px solid var(--color-border-default);
             background: var(${name});"
    ></div>
    <code style="font-size:12px; color:var(--color-text-secondary);">${name}</code>
  </div>
`;

const section = (title: string, body: unknown) => html`
  <section style="margin-bottom:40px;">
    <h2 style="font:600 20px/1.2 var(--font-family-sans); color:var(--color-text-primary); margin:0 0 16px;">
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

export const Colors: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans);">
      ${section(
        'Brand',
        grid(['50', '100', '200', '300', '400', '500', '600', '700', '800', '900'].map((s) => swatch(`--color-palette-brand-${s}`)))
      )}
      ${section(
        'Neutral',
        grid(['50', '100', '200', '300', '400', '500', '600', '700', '800', '900'].map((s) => swatch(`--color-palette-neutral-${s}`)))
      )}
      ${section(
        'Semantic — text & surface',
        grid([
          '--color-text-primary',
          '--color-text-secondary',
          '--color-background-canvas',
          '--color-background-surface',
          '--color-border-default',
          '--color-action-primary',
        ].map(swatch))
      )}
      ${section(
        'Feedback',
        grid([
          '--color-feedback-success',
          '--color-feedback-warning',
          '--color-feedback-danger',
        ].map(swatch))
      )}
    </div>
  `,
};

export const Spacing: Story = {
  render: () => html`
    <div style="padding:32px; font-family:var(--font-family-sans);">
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
    <div style="padding:32px; font-family:var(--font-family-sans);">
      ${section(
        'Corner radius',
        grid(
          ['none', 'sm', 'md', 'lg', 'xl'].map(
            (r) => html`
              <div style="display:flex; flex-direction:column; gap:6px;">
                <div style="height:72px; background:var(--color-palette-brand-100); border:1px solid var(--color-palette-brand-300); border-radius:var(--radius-${r});"></div>
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
          ${['xs', 'sm', 'md', 'lg', 'xl', '2xl', '3xl', '4xl'].map(
            (s) => html`
              <div style="display:flex; align-items:baseline; gap:24px;">
                <code style="width:90px; font-size:12px; color:var(--color-text-secondary);">size.${s}</code>
                <span style="font-size:var(--font-size-${s});">The quick brown fox</span>
              </div>
            `
          )}
        </div>`
      )}
    </div>
  `,
};
