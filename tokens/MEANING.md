# Meaning colours

Colour never carries meaning alone (directive I-3). Each meaning-colour role in
`color.json` also has a shape and a word, kept in `scripts/meaning-roles.json`:

| Role | Shape | Word |
|---|---|---|
| selected (violet) | ring | Selected |
| on-device | disc | On-device |
| cloud (cyan) | diamond | Cloud |
| error (the red) | cross | Error |
| warning | triangle | Warning |
| success | check | Done |

The red token, `color.palette.signal.danger`, is the error role's colour and nothing
else's. `node scripts/check-meaning-colours.js` fails if a role lacks a shape or word,
if two roles share one, if a meaning-colour token has no role, or if any other colour
token is red-hued or equals the red value. The one allowed exception is
`color.palette.material.smog`, the Form-World fog colour, which no UI role uses.
Add new roles to `meaning-roles.json`, not here.
