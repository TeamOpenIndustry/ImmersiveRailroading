package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemPaintBrush;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.animation.ControlPositionMapper;
import cam72cam.immersiverailroading.model.animation.IAnimatable;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.*;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.entity.custom.*;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.*;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.SingleCache;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRollingStock extends CustomEntity implements ITickable, IClickable, IKillable, IAnimatable {
	@TagField("defID")
    protected String defID;
	@TagField("gauge")
	public Gauge gauge;
	@TagField("tag")
	@TagSync
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

	public void setup(EntityRollingStockDefinition def, Gauge gauge, String texture) {
		this.defID = def.defID;
		this.gauge = gauge;
		this.texture = texture;
		def.cgDefaults.forEach(this::setControlPosition);
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
		if (player.getHeldItem(hand).is(IRItems.ITEM_PAINT_BRUSH) && player.hasPermission(Permissions.PAINT_BRUSH)) {
			ItemPaintBrush.onStockInteract(this, player, hand);
			return ClickResult.ACCEPTED;
		}

		if (player.getHeldItem(hand).is(Fuzzy.NAME_TAG) && player.hasPermission(Permissions.STOCK_ASSEMBLY)) {
			if (getWorld().isClient) {
				return ClickResult.ACCEPTED;
			}
			tag = player.getHeldItem(hand).getDisplayName();
			player.sendMessage(PlayerMessage.direct(tag));
			return ClickResult.ACCEPTED;
		}
		return ClickResult.PASS;
	}

	public void setTexture(String variant) {
		if (getDefinition().textureNames.containsKey(variant)) {
			this.texture = variant;
		}
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

	public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {

	}


	/**
	 * @return Stock Weight in Kg
	 */
	public double getWeight() {
		return this.getDefinition().getWeight(gauge);
	}

	public double getMaxWeight() {
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

	public float soundScale() {
		if (this.getDefinition().shouldScalePitch()) {
			double scale = gauge.scale() * getDefinition().internal_model_scale;
			return (float) Math.sqrt(Math.sqrt(scale));
		}
		return 1;
	}

	public ISound createSound(Identifier oggLocation, boolean repeats, double attenuationDistance, Supplier<Float> category) {
		ISound snd = Audio.newSound(
				oggLocation, SoundCategory.MASTER,
				repeats,
				(float) (attenuationDistance * ConfigSound.soundDistanceScale * gauge.scale()),
				soundScale()
		);
		return new ISound() {
			@Override
			public void play(Vec3d pos) {
				snd.play(pos);
			}

			@Override
			public void stop() {
				snd.stop();
			}

			@Override
			public void setPosition(Vec3d pos) {
				snd.setPosition(pos);
			}

			@Override
			public void setPitch(float f) {
				snd.setPitch(f);
			}

			@Override
			public void setVelocity(Vec3d vel) {
				snd.setVelocity(vel);
			}

			@Override
			public void setVolume(float f) {
				snd.setVolume(f * category.get());
			}

			@Override
			public boolean isPlaying() {
				return snd.isPlaying();
			}
		};
	}

	public String getTexture() {
		return texture;
	}

    @Override
	public Matrix4 getModelMatrix() {
		return this.modelMatrix.get(getPosition()).copy();
	}

	public boolean hasElectricalPower() {
		return false;
	}

	@TagSync
	@TagField(value="controlPositions", mapper = ControlPositionMapper.class)
	protected Map<String, Pair<Boolean, Float>> controlPositions = new HashMap<>();

    @Override
    public float defaultControlPosition(Control<?> control) {
        return 0;
    }

    @Override
    public Pair<Boolean, Float> getControlData(String control) {
        return controlPositions.getOrDefault(control, Pair.of(false, 0f));
    }

    @Override
    public Pair<Boolean, Float> getControlData(Control<?> control) {
        return controlPositions.getOrDefault(control.controlGroup, Pair.of(false, defaultControlPosition(control)));
    }

    @Override
    public float getControlPosition(Control<?> control) {
        return getControlData(control).getRight();
    }

    @Override
    public float getControlPosition(String control) {
        return getControlData(control).getRight();
    }

    @Override
    public void setControlPosition(Control<?> control, float val) {
        val = Math.min(1, Math.max(0, val));
        controlPositions.put(control.controlGroup, Pair.of(getControlPressed(control), val));
    }

    @Override
    public void setControlPosition(String control, float val) {
        val = Math.min(1, Math.max(0, val));
        controlPositions.put(control, Pair.of(false, val));
    }

    @Override
    public void setControlPositions(ModelComponentType type, float val) {
        getDefinition().getModel().getControls().stream().filter(x -> x.part.type == type).forEach(c -> setControlPosition(c, val));
    }

	public void onDragStart(Control<?> control) {
		setControlPressed(control, true);
	}

	public void onDrag(Control<?> control, double newValue) {
		setControlPressed(control, true);
		setControlPosition(control, (float)newValue);
	}

	public void onDragRelease(Control<?> control) {
        setControlPressed(control, false);

        if (control.toggle) {
            setControlPosition(control, Math.abs(getControlPosition(control) - 1));
        }
        if (control.press) {
            setControlPosition(control, 0);
        }
    }

    public boolean getControlPressed(Control<?> control) {
        return getControlData(control).getLeft();
    }

    public void setControlPressed(Control<?> control, boolean pressed) {
        controlPositions.put(control.controlGroup, Pair.of(pressed, getControlPosition(control)));
    }

	public boolean playerCanDrag(Player player, Control<?> control) {
		return control.part.type != ModelComponentType.INDEPENDENT_BRAKE_X || player.hasPermission(Permissions.BRAKE_CONTROL);
	}
}