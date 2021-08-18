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
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class EntityRollingStock extends CustomEntity implements ITickable, IClickable, IKillable {
	@TagField("defID")
    protected String defID;
	@TagField("gauge")
	public Gauge gauge;
	@TagField("tag")
	public String tag = "";

	@TagSync
	@TagField(value = "texture", mapper = StrictTagMapper.class)
	private String texture = null;

	public void setup(String defID, Gauge gauge, String texture) {
		this.defID = defID;
		this.gauge = gauge;
		this.texture = texture;
	}

	public boolean isImmuneToFire() {
		return true;
	}

	public float getCollisionReduction() {
		return 1;
	}

	public boolean canBePushed() {
		return false;
	}

	public boolean allowsDefaultMovement() {
		return false;
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
			// This should not be hit, entity should be removed handled by tryJoinWorld
			throw new RuntimeException(String.format("Definition %s has been removed!  This stock will not function!", defID));
		}
		return (T) def;
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
	 * Player Interactions
	 */
	
	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_PAINT_BRUSH)) {
			List<String> texNames = new ArrayList<>(this.getDefinition().textureNames.keySet());
			if (texNames.size() > 1) {
				int idx = texNames.indexOf(texture);
				idx = (idx + (player.isCrouching() ? -1 : 1) + texNames.size()) % (texNames.size());
				texture = texNames.get(idx);
				return ClickResult.ACCEPTED;
			} else {
				player.sendMessage(ChatText.BRUSH_NO_VARIANTS.getMessage());
				return ClickResult.ACCEPTED;
			}
		}
		return ClickResult.PASS;
	}

	@Override
	public void onDamage(DamageType type, Entity source, float amount, boolean bypassesArmor) {
		if (getWorld().isClient) {
			return;
		}

		if (type == DamageType.EXPLOSION) {
			if (source == null || !source.isMob()) {
				if (amount > 5 && ConfigDamage.trainMobExplosionDamage) {
					this.kill();
				}
			}
		}

		if (type == DamageType.OTHER && source != null && source.isPlayer()) {
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
		return texture;
	}

    public boolean internalLightsEnabled() {
		return false;
    }
}