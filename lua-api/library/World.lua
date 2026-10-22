---@meta

--- Queries about the Minecraft world around the stock: weather, light,
--- temperature, time and blocks.
---@class World
World = {}

--- Returns whether it is raining at the given position.
---@param pos vec
---@return boolean
function World.isRainingAt(pos) end

--- Returns whether it is snowing at the given position.
---@param pos vec
---@return boolean
function World.isSnowingAt(pos) end

--- Returns the temperature at the given position in degrees Celsius.
---@param pos vec
---@return number
function World.getTemperatureCelsius(pos) end

--- Returns the Minecraft biome temperature value at the given position.
--- This is the game's internal temperature value, not degrees Celsius; use
--- `World.getTemperatureCelsius` for that.
---@see https://www.reddit.com/r/Minecraft/comments/3eh7yu/the_rl_temperature_of_minecraft_biomes_revealed/
---@param pos vec
---@return number
function World.getBiomeTemperature(pos) end

--- Returns the snow level at the given position.
---@param pos vec
---@return number
function World.getSnowLevelAt(pos) end

--- Returns the block light level at the given position (light from torches,
--- lamps and other blocks).
---@param pos vec
---@return number
function World.getBlockLightAt(pos) end

--- Returns the sky light level at the given position (light from the sky).
---@param pos vec
---@return number
function World.getSkyLightAt(pos) end

--- Returns whether the sky is visible from the given position.
---@param pos vec
---@return boolean
function World.isSkyVisible(pos) end

--- Returns the id of the current dimension.
---@return number
function World.getDimension() end

--- Returns the current world time.
---@return number
function World.getTime() end

--- Returns the number of ticks since the world was created.
---@return number
function World.getTicks() end

--- Returns the name of the block at the given position.
---@param pos vec
---@return string
function World.getBlock(pos) end