---@meta

--- Event registration and dispatch. Registered handlers are the entry points
--- of your Lua script.
---@class Event
Event = {}

--- Registers a handler for an event. This is the entry point to your script.
---
--- Built-in events:
--- - `"onTick"`
--- - `"onControlGroupChange"`
---
--- Any other name is a custom event that can be fired with
--- `Event.triggerEvent` or `Event.triggerCouplerEvent`.
---@param name "onTick"|"onControlGroupChange"|string Name of the event to handle.
---@param func function Handler to call when the event fires.
---@return nil
function Event.registerEvent(name, func) end

--- Triggers an event on all coupled stock that registered a handler for it.
---@param event string Name of the event to trigger.
---@param ... any Arguments passed to the handlers.
---@return nil
function Event.triggerEvent(event, ...) end

--- Triggers an event on the stock coupled at the given coupler.
---@param event string Name of the event to trigger.
---@param coupler string The coupler whose connected stock receives the event.
---@param ... any Arguments passed to the handler.
---@return nil
function Event.triggerCouplerEvent(event, coupler, ...) end