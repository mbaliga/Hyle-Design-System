/**
 * Hyle Design System — web component entry point.
 *
 * Importing this module registers every custom element (<hy-field>, <hy-pane>,
 * <hy-chip>, …). Remember to also load the token stylesheet once in your app:
 *   import 'hyle-design-system/tokens.css';
 *
 * Core law: state is SHOWN by material behavior, never SAID by language.
 */

// — Material layer —
export { HyField } from './field/hy-field.js';
export type { HyFieldState } from './field/hy-field.js';
export { HyPane } from './pane/hy-pane.js';
export type { HyPaneDock } from './pane/hy-pane.js';
export { HyPulse } from './pulse/hy-pulse.js';
export type { HyPulseState } from './pulse/hy-pulse.js';

// — Tactile controls (extracted verbatim from the Tactile Kit) —
export { HyKnob } from './knob/hy-knob.js';
export type { HyKnobVariant } from './knob/hy-knob.js';
export { HyFader } from './fader/hy-fader.js';
export { HyToggle } from './toggle/hy-toggle.js';
export type { HyToggleVariant } from './toggle/hy-toggle.js';
export { HyMeter } from './meter/hy-meter.js';
export { HyVu } from './meter/hy-vu.js';
export { HyScrollWheel } from './scroll-wheel/hy-scroll-wheel.js';
export { HySlider } from './slider/hy-slider.js';
export type { HySliderVariant } from './slider/hy-slider.js';
export { HyTransport } from './transport/hy-transport.js';
export { HyKey } from './key/hy-key.js';
export type { HyKeyVariant } from './key/hy-key.js';
export { HyJoystick } from './joystick/hy-joystick.js';
export { HyDial } from './dial/hy-dial.js';

// — Displays & surfaces —
export { HyWaveform } from './waveform/hy-waveform.js';
export { HyScreen } from './screen/hy-screen.js';
export { HyGrille, HyJack } from './surface/hy-surface.js';

// — Controls —
export { HyChip } from './chip/hy-chip.js';
export { HyButton } from './button/hy-button.js';
export type { HyButtonVariant, HyButtonIcon } from './button/hy-button.js';
export { HyInput } from './input/hy-input.js';
export type { HyInputSize } from './input/hy-input.js';

// — Surfaces —
export { HyCard } from './card/hy-card.js';
export type { HyCardElevation } from './card/hy-card.js';

// — Colour —
export { HyColorPicker } from './color-picker/hy-color-picker.js';
export type { HyColorSpace } from './color-picker/hy-color-picker.js';

// — Iconography —
export { HyIcon } from './icon/hy-icon.js';
export { ICONS, ICON_NAMES } from './icon/icons.js';
export type { HyIconName } from './icon/icons.js';

// — Theming (user-selectable accent + iwanthue supporting palette) —
export { createTheme, applyTheme, randomAccent } from '../theme/theme.js';
export type { Theme, ThemeOptions } from '../theme/theme.js';
