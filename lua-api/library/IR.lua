---@meta

--- ImmersiveRailroading Lua API for rolling stock scripts.
---
--- Each function acts on the stock (locomotive, car, etc.) the script is
--- attached to, unless stated otherwise (see `IR.setGlobal`).

---@alias IR.Position "FRONT"|"BACK"
---@alias IR.SpeedUnit "ms"|"kmh"|"mph"
---@alias IR.TemperatureUnit "celcius"|"kelvin"|"farenheit" # Spelled this way in the API; correct spellings are not accepted.
---@alias IR.PressureUnit "bar"|"psi"|"kpa"

---@class IR.CoupledInfo
---@field coupler string
---@field uuid string
---@field defID string
---@field tag string

---@class IR
IR = {}

--- Sets the value of a control group on this stock.
---@param control string Name of the control group.
---@param value number New value, between 0 and 1.
---@return nil
function IR.setCG(control, value) end

--- Returns the value of a control group on this stock.
---@param control string Name of the control group.
---@return number value Between 0 and 1.
function IR.getCG(control) end

--- Sets a control group to the given value on every stock connected to this one.
--- Unlike `IR.setCG`, this affects the whole consist.
---@param control string Name of the control group.
---@param value number New value.
---@return nil
function IR.setGlobal(control, value) end

--- Sets the throttle.
---@param value number
---@return nil
function IR.setThrottle(value) end

--- Returns the current throttle value.
---@return number
function IR.getThrottle() end

--- Sets the reverser.
---@param value number
---@return nil
function IR.setReverser(value) end

--- Returns the current reverser value.
---@return number
function IR.getReverser() end

--- Sets the train (automatic) brake.
---@param value number
---@return nil
function IR.setTrainBrake(value) end

--- Returns the current train (automatic) brake value.
---@return number
function IR.getTrainBrake() end

--- Sets the independent (locomotive) brake.
---@param value number
---@return nil
function IR.setIndependentBrake(value) end

--- Returns the current independent (locomotive) brake value.
---@return number
function IR.getIndependentBrake() end

--- Turns the engine on or off.
---@param value boolean `true` to start, `false` to stop.
---@return nil
function IR.engineStartStop(value) end

--- Returns whether the engine is turned on.
---@return boolean
function IR.isTurnedOn() end

--- Engages or disengages the coupler at the given end of the stock.
---@param position IR.Position Which end of the stock.
---@param engaged boolean `true` to engage, `false` to disengage.
---@return nil
function IR.couplerEngaged(position, engaged) end

--- Returns information about the stock coupled at the given end.
---@param type IR.Position Which end of the stock.
---@return table<string, IR.CoupledInfo>
function IR.getCoupled(type) end

--- Returns the consist this stock belongs to.
---@return table<string, table>
function IR.getTrainConsist() end

--- Sets the tag of this stock.
---@param tag string
---@return nil
function IR.setTag(tag) end

--- Returns the tag of this stock.
---@return string
function IR.getTag() end

--- Returns the name of the current paint.
---@return string
function IR.getPaint() end

--- Sets the paint by name.
---@param name string Paint name.
---@return nil
function IR.setPaint(name) end

--- Returns whether the stock is fully built.
---@return boolean
function IR.isBuilt() end

--- Returns the world position of this stock.
---@return vec
function IR.getStockPosition() end

--- Returns the position of a named object in the stock's model.
---@param name string Object name in the model.
---@return vec
function IR.getObjectPos(name) end

--- Plays a sound at a position.
---@param identifier string Path to the sound file.
---@param pos vec Position the sound plays at.
---@param volume number Volume of the sound.
---@return nil
function IR.playSound(identifier, pos, volume) end

--- Returns the value of a readout.
---@param readout string Readout name; see `cam72cam.immersiverailroading.gui.overlay.Readouts`.
---@return number
function IR.getReadout(readout) end

--- Returns the speed of the stock.
---@param unit? IR.SpeedUnit Defaults to `"kmh"`.
---@return number
function IR.getSpeed(unit) end

--- Returns the temperature of the engine or boiler.
---@param unit? IR.TemperatureUnit Defaults to `"celcius"`.
---@return number
function IR.getTemperature(unit) end

--- Returns the boiler pressure.
---@param unit? IR.PressureUnit Defaults to `"psi"`.
---@return number
function IR.getBoilerPressure(unit) end

--- Returns the overall brake pressure.
---@return number
function IR.getBrakePressure() end

--- Returns the current weight of the stock.
---@return number
function IR.getWeight() end

--- Returns whether the stock is currently sliding (wheel slip).
---@return boolean
function IR.isSliding() end