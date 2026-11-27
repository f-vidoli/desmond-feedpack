# desmond-feedpack

Feed plugins for [Desmond](https://github.com/f-vidoli/desmond).

## Included plugins

- `nature.biotech/` - Nature Biotechnology RSS feed, OA/abstract/PDF enrichment, fullscreen builtin widget.
- `weather.bologna/` - Bologna weather via Open-Meteo, builtin native widget + service.
- `reuters.news/` - Reuters world headlines RSS, declarative WidgetState widget + builtin service.

## Install

Each folder is a Desmond plugin directory (manifest `id` must match folder name). Zip a folder and install through Desmond Plugin Manager > Install ZIP.

```bash
cd nature.biotech && zip -r ../nature.biotech.zip . && cd ..
```

## Notes

- `src/` contains reference Kotlin service/renderer sources used by builtin implementations. External ZIP plugins cannot ship arbitrary Compose classes; builtin classNames must be registered by the host app.
- Keep credentials out of plugin files. No secrets included.
