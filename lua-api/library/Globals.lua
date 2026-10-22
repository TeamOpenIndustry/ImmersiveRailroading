---@meta

--- Persistent storage for this stock.
---
--- Values you store in this table are serialized with the stock, so they are
--- kept when you quit and rejoin the world.
---@type table
_TagField = {}

--- Runs a function once after the given delay.
---@param sec number Delay before the function runs, in seconds.
---@param func function The function to run.
---@return nil
function wait(sec, func) end