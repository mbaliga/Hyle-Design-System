import { LitElement, unsafeCSS, type CSSResultGroup } from 'lit';
import { KIT_CSS } from './kit-css.js';

/**
 * Base class for every control lifted verbatim from the Tactile Kit.
 *
 * The kit is a single self-contained document: one `:root` token block, a body
 * of exact markup, and one script. To reproduce a control *byte-for-byte* rather
 * than reinterpret it, each Hyle component:
 *
 *   1. adopts the kit's exact CSS (`KIT_CSS`, extracted verbatim — `:root`→`:host`,
 *      selectable-accent skins host-scoped, default accent violet #8E7BFF), so it
 *      renders pixel-identically;
 *   2. renders the kit's exact markup fragment; and
 *   3. runs the kit's exact init JS, scoped to its shadow root.
 *
 * CSS custom properties inherit *through* the shadow boundary, so the kit tokens
 * declared on `:host` cascade to the lifted markup exactly as they do in the kit.
 *
 * Core law: state is SHOWN by material behavior, never SAID by language.
 */
export class KitElement extends LitElement {
  /**
   * The kit CSS, shared as one constructable stylesheet across every instance
   * (parsed once, adopted many times). Subclasses add their own scoped rules via
   * the normal static `styles` — this base sheet is prepended to them.
   */
  static kitStyles: CSSResultGroup = unsafeCSS(KIT_CSS);

  static styles: CSSResultGroup = KitElement.kitStyles;
}
