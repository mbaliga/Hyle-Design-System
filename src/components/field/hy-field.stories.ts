import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-field.js';
import '../pane/hy-pane.js';
import '../chip/hy-chip.js';
import '../button/hy-button.js';
import '../pulse/hy-pulse.js';

const setState = (s: string) => {
  document.querySelector('hy-field#state-demo')?.setAttribute('state', s);
};

const meta: Meta = {
  title: 'Material/Field',
  component: 'hy-field',
  parameters: { layout: 'fullscreen', backgrounds: { default: 'field' } },
  argTypes: {
    src: { control: 'text' },
    label: { control: 'text' },
  },
  args: {
    src: 'field/form-world-bg.html',
    label: 'Hyle field — monumental forms in coloured haze',
  },
};
export default meta;
type Story = StoryObj;

/**
 * The Field with the live Form-World engine behind a frosted Pane.
 * State is shown by the material moving, never by a status word.
 */
export const WithPane: Story = {
  render: ({ src, label }) => html`
    <hy-field src=${src} label=${label} style="height:100vh; border-radius:0;">
      <hy-pane dock="bottom" heading="Form · World" style="margin:18px;">
        <div style="display:flex; gap:7px; flex-wrap:wrap; margin-bottom:14px;">
          <hy-chip pressed>Bowl</hy-chip>
          <hy-chip>Helix</hy-chip>
          <hy-chip>Towers</hy-chip>
          <hy-chip>Arch</hy-chip>
          <hy-chip>Ruins</hy-chip>
          <hy-chip>Planet</hy-chip>
        </div>
        <div style="display:flex; gap:8px; justify-content:flex-end;">
          <hy-button variant="ghost" size="sm">Reseed</hy-button>
          <hy-button variant="primary" size="sm">Hold</hy-button>
        </div>
      </hy-pane>
    </hy-field>
  `,
};

/**
 * The procedural state field, bound to system state. Flip the state and watch
 * the material respond: idle is matte and near-still; thinking stirs; deep fills
 * the frame with slow, monumental motion. No engine, fully owned by the system.
 */
export const StateField: Story = {
  parameters: { backgrounds: { default: 'field' } },
  render: () => html`
    <hy-field id="state-demo" state="idle" style="height:100vh; border-radius:0;">
      <hy-pane dock="bottom" heading="System state" style="margin:18px;">
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <hy-button variant="secondary" size="sm" @hy-click=${() => setState('idle')}>Idle</hy-button>
          <hy-button variant="secondary" size="sm" @hy-click=${() => setState('thinking')}>Thinking</hy-button>
          <hy-button variant="primary" size="sm" @hy-click=${() => setState('deep')}>Deep</hy-button>
        </div>
      </hy-pane>
    </hy-field>
  `,
};

/** Driven by an explicit activity level (0..1) via the control. */
export const Activity: Story = {
  argTypes: { activity: { control: { type: 'range', min: 0, max: 1, step: 0.01 } } },
  args: { src: '', activity: 0.3 } as Record<string, unknown>,
  render: ({ activity }) => html`
    <hy-field activity=${activity} style="height:100vh; border-radius:0;"></hy-field>
  `,
};

/** A floating pane over the field, rather than a bottom sheet. */
export const FloatingPane: Story = {
  render: ({ src, label }) => html`
    <hy-field src=${src} label=${label} style="height:100vh; border-radius:0;">
      <div style="margin:auto; padding:24px; pointer-events:none;">
        <hy-pane
          heading="Provenance"
          style="width:300px; pointer-events:auto;"
        >
          <div style="display:flex; flex-direction:column; gap:12px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <hy-pulse state="watched">
                <span
                  style="width:8px; height:8px; border-radius:50%; background:var(--color-provenance-native); box-shadow:0 0 10px var(--color-provenance-native);"
                ></span>
              </hy-pulse>
              <span style="font-size:13px;">On-device · native</span>
            </div>
            <div style="display:flex; align-items:center; gap:10px;">
              <hy-pulse state="watched">
                <span
                  style="width:8px; height:8px; border-radius:50%; background:var(--color-provenance-cloud); box-shadow:0 0 10px var(--color-provenance-cloud);"
                ></span>
              </hy-pulse>
              <span style="font-size:13px;">Cloud · foreign</span>
            </div>
          </div>
        </hy-pane>
      </div>
    </hy-field>
  `,
};
