package cam72cam.immersiverailroading.library;

public enum LockType {
	UNLOCKED,
	SEMI_LOCKED,	//only owner can break the stock
	LOCKED,			//only owner can enter or break the stock
	;
	
	public LockType next () {
		return values()[((ordinal() + 1) % (values().length))];
	}
}