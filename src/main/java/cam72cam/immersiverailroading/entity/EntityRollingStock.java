package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.*;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class EntityRollingStock extends Entity implements IWorldData, ISpawnData, ITickable, IClickable, IKillable {
	public static final EntitySettings settings = new EntitySettings().setCollisionReduction(1f).setImmuneToFire(true).setAttachedToPlayer(false).setDefaultMovement(false);

    protected String defID;
	public Gauge gauge;
	public String tag = "";

	public void setup(String defID, Gauge gauge, String texture) {
		this.defID = defID;
		this.gauge = gauge;
		this.sync.setString("texture", texture);
	}

	/* TODO?
	@Override
	public String getName() {
		return this.getDefinition().name();
	}
	*/

	public String tryJoinWorld() {
		if (DefinitionManager.getDefinition(defID) == null) {
			String error = String.format("Missing definition %s, do you have all of the required resource packs?", defID);
			ImmersiveRailroading.error(error);
			return error;
		}
		return null;
	}

	public EntityRollingStockDefinition getDefinition() {
		return this.getDefinition(EntityRollingStockDefinition.class);
	}
	public <T extends EntityRollingStockDefinition> T getDefinition(Class<T> type) {
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		if (def == null) {
			try {
				return type.getConstructor(String.class, JsonObject.class).newInstance(defID, null);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return type.cast(def);
		}
	}
	public String getDefinitionID() {
		return this.defID;
	}

	@Override
	public void onTick() {
		if (getWorld().isServer && this.getTickCount() % 5 == 0) {
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def == null) {
				this.kill();
			}
		}
	}

	/*
	 * 
	 * Data RW for Spawn and Entity Load
	 */

	@Override
	public void save(TagCompound nbttagcompound) {
		nbttagcompound.setString("defID", defID);
		nbttagcompound.setDouble("gauge", gauge.value());
		nbttagcompound.setString("tag", tag);
        nbttagcompound.setString("texture", sync.getString("texture"));
	}

	@Override
	public void load(TagCompound nbttagcompound) {
		defID = nbttagcompound.getString("defID");
        gauge = Gauge.from(nbttagcompound.getDouble("gauge"));

		tag = nbttagcompound.getString("tag");

		sync.setString("texture", nbttagcompound.getString("texture"));
	}


	@Override
	public void saveSpawn(TagCompound data) {
		data.setString("defID", defID);
		data.setDouble("gauge", gauge.value());
		data.setString("tag", tag);

        data.setString("texture", sync.getString("texture"));
	}
	@Override
	public void loadSpawn(TagCompound data) {
		defID = data.getString("defID");
        gauge = Gauge.from(data.getDouble("gauge"));
		tag = data.getString("tag");

		sync.setString("texture", data.getString("texture"));
	}


	/*
	 * Player Interactions
	 */
	
	@Override
	public ClickResult onClick(Player player, Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_PAINT_BRUSH)) {
			List<String> texNames = new ArrayList<>(this.getDefinition().textureNames.keySet());
			if (texNames.size() > 1) {
				int idx = texNames.indexOf(sync.getString("texture"));
				idx = (idx + (player.isCrouching() ? -1 : 1) + texNames.size()) % (texNames.size());
				sync.setString("texture", texNames.get(idx));
                sync.send();
				return ClickResult.ACCEPTED;
			} else {
				player.sendMessage(ChatText.BRUSH_NO_VARIANTS.getMessage());
				return ClickResult.ACCEPTED;
			}
		}
		return ClickResult.PASS;
	}

	@Override
	public void onDamage(DamageType type, Entity source, float amount) {
		if (getWorld().isClient) {
			return;
		}

		switch (type) {
			case EXPLOSION:
				if (amount > 5 && ConfigDamage.trainMobExplosionDamage) {
					this.kill();
				}
				break;
			case PLAYER:
				Player player = source.asPlayer();
				if (player.isCrouching()) {
					this.kill();
				}
		}
	}

	@Override
	public void onRemoved() {

	}

	protected boolean shouldDropItems(DamageType type, float amount) {
		return type != DamageType.EXPLOSION || amount < 20;
	}

	public void handleKeyPress(Player source, KeyTypes key) {

	}


	/**
	 * @return Stock Weight in Kg
	 */
	public double getWeight() {
		return this.getDefinition().getWeight(gauge);
	}

	/*
	 * Helpers
	 */
	/* TODO RENDER

	@SideOnly(Side.CLIENT)
	@Override
	public boolean isInRangeToRenderDist(double distance)
    {
        return true;
    }

	@Override
	public boolean shouldRenderInPass(int pass) {
		return false;
	}
	*/

	public void triggerResimulate() {
	}

	public Gauge soundGauge() {
		return this.getDefinition().shouldScalePitch() ? gauge : Gauge.from(Gauge.STANDARD);
	}

	public String getTexture() {
		return sync.getString("texture");
	}
}