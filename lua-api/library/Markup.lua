---@meta

--- Reading of structured data files (JSON or CAML).
---@class Markup
Markup = {}

--- Parses a JSON or CAML file and returns its contents as a Lua table.
---@param identifier string Resource path of the file, in `namespace:path` form, e.g. `"immersiverailroading:path/to/file.json"`.
---@return table
function Markup.read(identifier) end