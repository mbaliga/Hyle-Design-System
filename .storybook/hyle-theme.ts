import { create } from '@storybook/theming';

/** Hyle Storybook theme — inky, monumental, Archivo. */
export default create({
  base: 'dark',
  brandTitle: 'Hyle — matter behaving',
  brandUrl: 'https://github.com/mbaliga/Hyle-Design-System',

  colorPrimary: '#8e7bff',
  colorSecondary: '#8e7bff',

  appBg: '#0a0809',
  appContentBg: '#0a0809',
  appPreviewBg: '#000000',
  appBorderColor: 'rgba(255,255,255,0.08)',
  appBorderRadius: 10,

  textColor: '#ece8e4',
  textInverseColor: '#0a0809',
  textMutedColor: 'rgba(236,232,228,0.42)',

  barTextColor: 'rgba(236,232,228,0.42)',
  barSelectedColor: '#8e7bff',
  barHoverColor: '#8e7bff',
  barBg: '#0a0809',

  inputBg: '#121212',
  inputBorder: 'rgba(255,255,255,0.14)',
  inputTextColor: '#ece8e4',
  inputBorderRadius: 8,

  fontBase: "'Archivo Variable', ui-sans-serif, system-ui, sans-serif",
  fontCode: "'JetBrains Mono Variable', ui-monospace, monospace",
});
