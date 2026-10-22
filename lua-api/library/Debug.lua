---@meta

--- Debugging helpers for writing to the game log and the in-game chat.
---@class Debug
Debug = {}

--- Writes a message to the game log at INFO level.
---@param arg string The message to log.
---@return nil
function Debug.printToInfoLog(arg) end

--- Writes a message to the game log at WARN level.
---@param arg string The message to log.
---@return nil
function Debug.printToWarnLog(arg) end

--- Writes a message to the game log at ERROR level.
---@param arg string The message to log.
---@return nil
function Debug.printToErrorLog(arg) end

--- Sends a message to the chat of every passenger riding this stock.
---@param arg string The message to show.
---@return nil
function Debug.printToPassengerDialog(arg) end