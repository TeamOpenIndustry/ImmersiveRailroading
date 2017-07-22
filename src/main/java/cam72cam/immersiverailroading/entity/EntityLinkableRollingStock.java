package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityLinkableRollingStock extends EntityRollingStock {
	
	public boolean isAttaching = false;

	private UUID LinkFront;
	private UUID LinkBack;
	private EntityLinkableRollingStock cartLinkedFront;
	private EntityLinkableRollingStock cartLinkedBack;

	public EntityLinkableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setString("LinkFront", LinkFront == null ? "" : LinkFront.toString());
		nbttagcompound.setString("LinkBack", LinkBack == null ? "" : LinkBack.toString());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		String frontLink = nbttagcompound.getString("LinkFront");
		if (frontLink.length() > 0) {
			System.out.println(String.format("'%s'", frontLink));
			LinkFront = UUID.fromString(frontLink);
		}
		
		String backLink = nbttagcompound.getString("LinkBack");
		if (backLink.length() > 0) {
			LinkBack = UUID.fromString(backLink);
		}
	}

	public final boolean isLinked() {
		return LinkFront != null || LinkBack != null;
	}

	public void unlink() {
		unlinkFront();
		unlinkBack();
	}

	public void link(EntityLinkableRollingStock train) {
		if (train.getUniqueID() == this.LinkFront || train.getUniqueID() == this.LinkBack) {
			// already linked
			return;
		}
		if (this.LinkFront == null) {
			this.LinkFront = train.getUniqueID();
			train.link(this);
			this.isAttaching = false;
		} else if (this.LinkBack == null) {
			this.LinkBack = train.getUniqueID();
			train.link(this);
			this.isAttaching = false;
		}
		// no free link slots
	}

	public void unlink(EntityLinkableRollingStock train) {
		if (train.getUniqueID() == this.LinkFront) {
			unlinkFront();
		} else if (train.getUniqueID() == this.LinkBack) {
			unlinkBack();
		}
	}

	public void unlinkFront() {
		EntityLinkableRollingStock cartLinkedFront = getLinkedCartFront();

		// Break the link
		this.LinkFront = null;
		this.cartLinkedFront = null;

		// Ask the connected car to do the same
		if (cartLinkedFront != null) {
			cartLinkedFront.unlink(this);
		}
	}

	public void unlinkBack() {
		EntityLinkableRollingStock cartLinkedBack = getLinkedCartBack();

		// Break the link
		this.LinkBack = null;
		this.cartLinkedBack = null;

		// Ask the connected car to do the same
		if (cartLinkedBack != null) {
			cartLinkedBack.unlink(this);
		}
	}

	public boolean isLinked(EntityLinkableRollingStock cart) {
		return this.getLinkedCartFront() == cart || this.getLinkedCartBack() == cart;
	}

	public EntityLinkableRollingStock getLinkedCartFront() {
		if (this.cartLinkedFront == null && this.LinkFront != null) {
			this.cartLinkedFront = findByUUID(this.world, this.LinkFront);
		}
		return this.cartLinkedFront;
	}

	public EntityLinkableRollingStock getLinkedCartBack() {
		if (this.cartLinkedBack == null && this.LinkBack != null) {
			this.cartLinkedBack = findByUUID(this.world, this.LinkBack);
		}
		return this.cartLinkedBack;
	}

	public static EntityLinkableRollingStock findByUUID(World world, UUID uuid) {
		// May want to cache this if it happens a lot
		for (Object e : world.getLoadedEntityList()) {
			if (e instanceof EntityLinkableRollingStock) {
				EntityLinkableRollingStock train = (EntityLinkableRollingStock) e;
				if (train.getUniqueID() == uuid) {
					return train;
				}
			}
		}
		return null;
	}
	
	public final List<EntityRollingStock> getTrain() {
		return this.buildTrain(new ArrayList<EntityRollingStock>());
	}
	private final List<EntityRollingStock> buildTrain(List<EntityRollingStock> train) {
		if (!train.contains(this)) {
			train.add(this);
			if (this.getLinkedCartFront() != null) {
				train = this.getLinkedCartFront().buildTrain(train);
			}
			if (this.getLinkedCartBack() != null) {
				train = this.getLinkedCartBack().buildTrain(train);
			}
		}
		return train;
	}
}
