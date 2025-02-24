Open Computers is currently the preferred way to automate your Immersive Railroading railway

Placing a Adaptor next to or under a Detector Augment provides the following functions under components.ir_augment_detector:

* info(): returns information about the stock/locomotive overhead
* consist(): returns summary information about the full train overhead
* getTag(): returns the stock/locomotive tag
* setTag(string): sets the stock/locomotive tag
* getPos(): returns the augment's position
* getAugmentType(): returns the augment's type

(Don't forget to set the augment to COMPUTER mode by right-clicking it with a redstone torch)

Placing a Adaptor next to or under a Loco Control Augment provides the following functions under components.ir_augment_control:

* setThrottle(number): Sets the throttle to a value between -1 and 1 (reverse/forward)
* setBrake(number): Sets the brake to a value between 0 and 1
* setHorn(number): fires off the locomotive's horn for the given number of ticks
* getPos(): returns the augment's position
* getAugmentType(): returns the augment's type

These augments will also fire opencomputers events.  You can listen for them with:

event_name, net_address, augment_type, stock_uuid = event.pull("ir_train_overhead")


![setup](immersiverailroading:wiki/images/oc1.png)
![code](immersiverailroading:wiki/images/oc2.png)