This page is WIP document for the [StockName].json format.

//TODO Re-write

```json
{
    "era": "<Era of this stock>",
    "name": "<Human Readable Name of the Stock>",
    "works": "<Name of the manufacturer of the stock>",
    "model": "immersiverailroading:models/rolling_stock/<model_type>/<model_file_name>.obj",
    "darken_model": "<decimal value>",
    "properties": {
        "weight_kg": <integer weight>,
        "horsepower": <integer horsepower>,
        "tractive_effort_lbf": <integer tractive effort>,
        "max_speed_kmh": <integer maximum speed>
    },
    "passenger": {
        "slots": 4,
        "center_x": 0,
        "center_y": 0.7,
        "length": 1,
        "width": 1
    },
    "trucks": {
        "front": 0.5, 
        "rear": -0.5
    },
    "couplers": {
        "front_offset": 0.1,
        "rear_offset": 0.1
    },
    "extra_tooltip_info": [
        "Extra Line in the tooltip",
        "Another extra line in the tooltip"
    ]
}
```