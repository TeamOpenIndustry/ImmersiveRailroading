---@meta

--- A 3D vector, used for positions and directions (e.g. `IR.getStockPosition`).
---
--- Vectors are immutable in practice: every method returns a new vector (or a
--- value) and leaves the original untouched. Methods are called with a dot on
--- an instance `local c = a.add(b)`.
---@class vec
---@field x number
---@field y number
---@field z number
vec = {}

--- Creates a new vector. Exactly three arguments are required.
---@param x number
---@param y number
---@param z number
---@return vec
function vec.new(x, y, z) end

--- Returns a new vector: this vector plus `other`.
---@param other vec
---@return vec
function vec.add(other) end

--- Returns a new vector: this vector minus `other`.
---@param other vec
---@return vec
function vec.subtract(other) end

--- Returns a new vector with every component multiplied by `scale`.
---@param scale number
---@return vec
function vec.scale(scale) end

--- Returns a new vector pointing the same way with a length of 1.
--- A zero-length vector cannot be normalized (division by zero).
---@return vec
function vec.normalize() end

--- Returns a new vector holding the smaller of each component pair.
---@param other vec
---@return vec
function vec.min(other) end

--- Returns a new vector holding the larger of each component pair.
---@param other vec
---@return vec
function vec.max(other) end

--- Returns the dot product of this vector and `other`.
---@param other vec
---@return number
function vec.dotProduct(other) end

--- Returns a new vector: the cross product of this vector and `other`.
---@param other vec
---@return vec
function vec.crossProduct(other) end

--- Returns the length (magnitude) of this vector.
---@return number
function vec.length() end

--- Returns the squared length of this vector.
--- Cheaper than `length` when you only need to compare distances.
---@return number
function vec.lengthSquared() end

--- Returns whether all three components equal those of `other` exactly.
---@param other vec
---@return boolean
function vec.equals(other) end

--- Returns a new vector rotated around the vertical (Y) axis.
---@param angle number Angle in degrees.
---@return vec
function vec.rotateYaw(angle) end

--- Returns the vector as a string, formatted `(x, y, z)`.
---@return string
function vec.toString() end