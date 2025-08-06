The Detector Augment is used to check what locomotive/stock is passing overhead.  Right-click on a track segment to apply the augment. Like any augment, it may be removed from the track by right-clicking with the wrench. It can be filtered by right-clicking on the augment with a locomotive or rolling stock item.

Right-click on the augment with a redstone torch in your hand will switch it between a few modes:
* Stock: Outputs a signal of 15 if there's a stock on the detector and it matches the filter (if set), otherwise outputs no signal (this is the default mode).
* Speed: Outputs a signal with strength equal to `floor(speedInKmh / 10)` of the stock on the detector if it matches the filter (if set), otherwise outputs no signal.
* Number of passengers: Outputs a signal with strength equal to the `min(15, numberOfPassengersRiding)` the stock on the detector if it matches the filter (if set), otherwise outputs no signal.
* Freight Cargo Fullness: Outputs a signal with strength proportional to the `numberOfNonEmptyItemSlots / totalNumberOfItemSlots` of the stock on the detector if it matches the filter (if set), otherwise outputs no signal.
* Liquid Cargo Fullness: Outputs a signal with strength proportional to the `amountOfFluid / maxAmountOfFluid` of the stock on the detector if it matches the filter (if set), otherwise outputs no signal.

## Examples:
A plain detector:

![Plain](immersiverailroading:wiki/images/detector1.png)

A liquid detector:

![Liquid](immersiverailroading:wiki/images/detector2.png)