package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.*;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;
import cam72cam.mod.util.SingleCache;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
	private SingleCache<Vec3d, Matrix4> modelMatrix = new SingleCache<>(v -> new Matrix4()
			.translate(this.getPosition().x, this.getPosition().y, this.getPosition().z)
			.rotate(Math.toRadians(180 - this.getRotationYaw()), 0, 1, 0)
			.rotate(Math.toRadians(this.getRotationPitch()), 1, 0, 0)
			.rotate(Math.toRadians(-90), 0, 1, 0)
			.scale(this.gauge.scale(), this.gauge.scale(), this.gauge.scale())
	);

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
    public boolean externalLightsEnabled() {
		return internalLightsEnabled();
	}
	public Matrix4 getModelMatrix() {
		return this.modelMatrix.get(getPosition()).copy();
	}

	@TagSync
	@TagField(value="controlPositions", mapper = ControlPositionMapper.class)
	protected Map<String, Pair<Boolean, Float>> controlPositions = new HashMap<>();

	public void onDragStart(Control control) {
		setControlPressed(control, true);
	}

	public void onDrag(Control control, double delta) {
		setControlPressed(control, true);
		setControlPosition(control, (float)delta + getControlPosition(control));
	}

	public void onDragRelease(Control control) {
		setControlPressed(control, false);

		if (control.toggle) {
			setControlPosition(control, Math.abs(getControlPosition(control) - 1));
		}
		if (control.press) {
			setControlPosition(control, 0);
		}
	}

	protected float defaultControlPosition(Control control) {
		return 0;
	}

	public Pair<Boolean, Float> getControlData(String control) {
		return controlPositions.getOrDefault(control, Pair.of(false, 0f));
	}

	public Pair<Boolean, Float> getControlData(Control control) {
		return controlPositions.getOrDefault(control.controlGroup, Pair.of(false, defaultControlPosition(control)));
	}

	public boolean getControlPressed(Control control) {
		return getControlData(control).getLeft();
	}

	public void setControlPressed(Control control, boolean pressed) {
		controlPositions.put(control.controlGroup, Pair.of(pressed, getControlPosition(control)));
	}

	public float getControlPosition(Control control) {
		return getControlData(control).getRight();
	}

	public float getControlPosition(String control) {
		return getControlData(control).getRight();
	}

	public void setControlPosition(Control control, float val) {
		val = Math.min(1, Math.max(0, val));
		controlPositions.put(control.controlGroup, Pair.of(getControlPressed(control), val));
	}

	public void setControlPosition(String control, float val) {
		val = Math.min(1, Math.max(0, val));
		controlPositions.put(control, Pair.of(false, val));
	}

	public void setControlPositions(ModelComponentType type, float val) {
		getDefinition().getModel().getDraggableComponents().stream().filter(x -> x.part.type == type).forEach(c -> setControlPosition(c, val));
	}

	public boolean playerCanDrag(Player player, Control control) {
		return true;
	}

	private static class ControlPositionMapper implements TagMapper<Map<String, Pair<Boolean, Float>>> {
		@Override
		public TagAccessor<Map<String, Pair<Boolean, Float>>> apply(
				Class<Map<String, Pair<Boolean, Float>>> type,
				String fieldName,
				TagField tag) throws SerializationException {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, Function.identity(), x -> new TagCompound().setBoolean("pressed", x.getLeft()).setFloat("pos", x.getRight())),
					d -> d.getMap(fieldName, Function.identity(), x -> Pair.of(x.hasKey("pressed") && x.getBoolean("pressed"), x.getFloat("pos")))
			);
		}
	}
}