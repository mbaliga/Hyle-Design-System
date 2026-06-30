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
export { HyPane } from './pane/hy-pane.js';
export type { HyPaneDock } from './pane/hy-pane.js';

// — Controls —
export { HyChip } from './chip/hy-chip.js';
export { HyButton } from './button/hy-button.js';
export type { HyButtonVariant, HyButtonSize } from './button/hy-button.js';
export { HyInput } from './input/hy-input.js';
export type { HyInputSize } from './input/hy-input.js';

// — Surfaces —
export { HyCard } from './card/hy-card.js';
export type { HyCardElevation } from './card/hy-card.js';
