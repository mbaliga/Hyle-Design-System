import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';
import './hy-terminal.js';

const capStyle =
  'font:600 9px sans-serif;letter-spacing:.1em;color:#6b6760;text-transform:uppercase';

/**
 * The terminal well: the surface a machine reads its own work back out of.
 *
 * Glass is the layer you read through, grille and jack are the panel you touch,
 * and terminal is the layer something writes to while you watch. It is cut into
 * the surface rather than laid on it, which is why its ground sits below the
 * raised field and it carries an inner shadow rather than a drop shadow.
 *
 * No phosphor green and no scanlines by default. A terminal that cosplays a
 * VT100 is saying "this is technical" in language, which is the exact move the
 * house law forbids. What makes this read as a terminal is the fixed advance,
 * the well, and the cursor, all of which do work.
 */
const meta: Meta = {
  title: 'Surfaces/Terminal',
  tags: ['autodocs'],
  parameters: { backgrounds: { default: 'near' } },
};
export default meta;
type Story = StoryObj;

export const Default: Story = {
  argTypes: {
    prompt: { control: 'text' },
    state: { control: 'inline-radio', options: ['idle', 'working', 'failed'] },
    provenance: { control: 'inline-radio', options: ['native', 'cloud'] },
    scanlines: { control: 'boolean' },
  },
  args: { prompt: '>', state: 'idle', provenance: 'native', scanlines: false },
  render: ({ prompt, state, provenance, scanlines }) => html`
    <div style="width:min(560px, 92vw)">
      <hy-terminal prompt=${prompt} state=${state} provenance=${provenance} ?scanlines=${scanlines}
        >build tokens --platform web</hy-terminal
      >
    </div>
  `,
};

/**
 * State is carried entirely by the cursor. Idle holds and blinks, working
 * breathes, failed stops and takes the danger hue. There is no status word in
 * the component and no spinner, and because the cursor's *motion* changes as
 * well as its colour, the state survives greyscale and colour-vision deficiency
 * (WCAG 1.4.1).
 *
 * Under `prefers-reduced-motion` every one of these holds solid instead. The
 * cursor is still the state channel, it just stops moving.
 */
export const State: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; flex-direction:column; gap:22px; width:min(560px, 92vw)">
      ${(['idle', 'working', 'failed'] as const).map(
        (s) => html`
          <div style="display:flex; flex-direction:column; gap:8px">
            <hy-terminal state=${s}
              >${
                s === 'failed' ? 'build tokens --platform ios' : 'build tokens --platform web'
              }</hy-terminal
            >
            <div style=${capStyle}>${s}</div>
          </div>
        `
      )}
    </div>
  `,
};

/**
 * The prompt glyph takes the provenance hue, so where the compute is happening
 * is legible from the prompt alone: native means this machine is the one about
 * to act, cloud means the work is going elsewhere. The glyph itself is always
 * present, so the hue is never the only channel.
 */
export const Provenance: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; flex-direction:column; gap:22px; width:min(560px, 92vw)">
      <div style="display:flex; flex-direction:column; gap:8px">
        <hy-terminal provenance="native" state="working">embedding 1,284 notes</hy-terminal>
        <div style=${capStyle}>native &middot; on this device</div>
      </div>
      <div style="display:flex; flex-direction:column; gap:8px">
        <hy-terminal provenance="cloud" prompt="&#8599;" state="working"
          >embedding 1,284 notes</hy-terminal
        >
        <div style=${capStyle}>cloud &middot; leaves this device</div>
      </div>
    </div>
  `,
};

/**
 * A finished transcript: no cursor, because nothing is about to happen. Set
 * `no-cursor` when the terminal is a record rather than a live surface.
 */
export const Transcript: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="width:min(560px, 92vw)">
      <hy-terminal no-cursor>
        <div style="color:var(--color-terminal-dim, rgba(236,232,228,.42))">
          2026-07-25 &nbsp; build tokens --platform web
        </div>
        <div style="color:var(--color-terminal-dim, rgba(236,232,228,.42))">
          2026-07-25 &nbsp; build tokens --platform android
        </div>
        <div>2026-07-25 &nbsp; build tokens --platform ios</div>
      </hy-terminal>
    </div>
  `,
};

/**
 * Scanlines are opt-in and off by default. They are here for the one case where
 * a CRT is literally the subject, not as a default texture: as a default they
 * would be decoration asserting technicality rather than a material doing work.
 */
export const Scanlines: Story = {
  parameters: { controls: { disable: true } },
  render: () => html`
    <div style="display:flex; gap:20px; flex-wrap:wrap">
      <div style="display:flex; flex-direction:column; gap:8px; width:min(320px, 92vw)">
        <hy-terminal>ls -la /dev/hyle</hy-terminal>
        <div style=${capStyle}>default</div>
      </div>
      <div style="display:flex; flex-direction:column; gap:8px; width:min(320px, 92vw)">
        <hy-terminal scanlines>ls -la /dev/hyle</hy-terminal>
        <div style=${capStyle}>scanlines</div>
      </div>
    </div>
  `,
};
