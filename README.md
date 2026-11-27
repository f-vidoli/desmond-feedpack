# desmond-feedpack

Feed plugins for [Desmond](https://github.com/f-vidoli/desmond).

## Included plugins

- `nature-biotech/` - Nature Biotechnology RSS feed with OA/abstract/PDF enrichment, fullscreen builtin widget.
- `weather-bologna/` - Open-Meteo weather widget with configurable location settings.
- `reuters-rss/` - Reuters world headlines RSS, declarative WidgetState widget + builtin service.

## Weather location settings

Edit `weather-bologna/settings/weather-settings.json`:

```json
{
  "data": {
    "latitude": 44.4949,
    "longitude": 11.3426,
    "label": "Bologna",
    "timezone": "Europe/Rome"
  }
}
```

## Install

Each folder is a Desmond plugin directory (folder name == manifest `id`). Zip a folder and install via Desmond Plugin Manager > Install ZIP.

```bash
zip -r weather-bologna.zip weather-bologna
```

## Notes

- `src/` contains reference Kotlin service/renderer sources for builtin host integration. External ZIP plugins cannot ship arbitrary Compose classes; builtin classNames must be registered by the host app.
- No secrets included.
