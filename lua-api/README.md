# Immersive Railroading Lua API
Type definitions for the Lua scripting API of Immersive Railroading. They give you autocomplete, parameter hints and documentation while writing stock scripts. They are not code and are never executed.

Works with:

- [Lua Language Server](https://luals.github.io/) (VS Code)
- [EmmyLua](https://github.com/EmmyLua/IntelliJ-EmmyLua2) (IntelliJ)

## Modules

| Module   | Purpose                                                        |
|----------|----------------------------------------------------------------|
| `IR`     | Control the stock: throttle, brakes, couplers, paint, readouts |
| `vec`    | 3D vectors for positions and directions                        |
| `World`  | Weather, light, temperature, time and blocks                   |
| `Event`  | Register handlers and trigger events across coupled stock      |
| `Debug`  | Write to the game log or the passenger chat                    |
| `Markup` | Read JSON or CAML files into Lua tables                        |
| `wait`   | Run a function after a delay (see `Globals.lua`)               |

## Setup

Copy the content of the `lua-api` folder to your working direction.