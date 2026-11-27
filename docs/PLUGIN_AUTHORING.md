# Desmond Feedpack - Plugin Authoring

See main Desmond docs: https://github.com/f-vidoli/desmond/blob/master/docs/PLUGIN_AUTHORING.md

## Feedpack specifics

- Manifest schema: `1`; folder name == `id`.
- WidgetState JSON for declarative widgets (`title/status/body/items/actions/fullscreen/data`).
- `builtin` service/widget classNames require host registration in Desmond app.
- Reuters plugin uses file widget + builtin service; Nature and Weather use builtin widget/service patterns.
