package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.physics.simulation.RigidBodyBox;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.custom.ICollision;
import cam72cam.mod.entity.custom.IKillable;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.ArrayList;
import java.util.List;

public class RollingStockComponent extends CustomEntity implements IKillable, ICollision {
    @TagField("defID")
    public String defID;

    @TagField("gauge")
    public Gauge gauge;

    @TagField("componentType")
    public ItemComponentType componentType;

    @TagField("roll")
    public float roll = 0;

    @TagField("texture")
    public String texture;

    @TagField(value = "modelIDs", mapper = StringListMapper.class)
    public List<String> modelIDs;

    @TagField("modelMin")
    public Vec3d modelMin;

    @TagField("modelMax")
    public Vec3d modelMax;

    @TagField("weightKg")
    public double weightKg;

    public RigidBodyBox rbb;

    public String tryJoinWorld() {
        if (DefinitionManager.getDefinition(defID) == null) {
            String error = String.format("Missing definition %s, do you have all of the required resource packs?", defID);
            ImmersiveRailroading.error(error);
            return error;
        }
        return null;
    }


    @Override
    public void onDamage(DamageType type, Entity source, float amount, boolean bypassesArmor) {
        if (getWorld().isClient) {
            return;
        }

        if (type == DamageType.EXPLOSION) {
            if (source == null || !source.isMob()) {
                if (amount > 5 && Config.ConfigDamage.trainMobExplosionDamage) {
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

        if (this.isDead()) {
            ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
            ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
            data.def = DefinitionManager.getDefinition(defID);
            data.gauge = gauge;
            data.componentType = componentType;
            data.write();
            getWorld().dropItem(item, source != null ? source.getBlockPosition() : getBlockPosition());
        }
    }

    @Override
    public void onRemoved() {

    }

    @Override
    public IBoundingBox getCollision() {
        Vec3d mm = modelMax.subtract(modelMin).scale(0.5);
        return IBoundingBox.from(mm.scale(-1), mm).offset(getPosition());
    }

    public void setup(String defID, String texture, Gauge gauge, ItemComponentType component, ModelComponent mc, double density, RigidBodyBox parentRBB) {
        this.defID = defID;
        this.gauge = gauge;
        this.componentType = component;
        this.modelIDs = new ArrayList<>(mc.modelIDs);
        this.modelMin = mc.min;
        this.modelMax = mc.max;
        this.texture = texture;

        Vec3d areaV = mc.max.subtract(mc.min);
        this.weightKg = (areaV.x * areaV.y * areaV.z) * density;

        Vec3d mm = this.modelMax.subtract(this.modelMin);
        this.rbb = new RigidBodyBox((float) mm.x, (float) mm.y, (float) mm.z, (float) this.weightKg);
        this.rbb.setRestitution(0.3f);

        Vec3d center = mc.min.add(mc.max).scale(0.5);
        parentRBB.previousState().copyPhysicsAtPoint(rbb, center);

        this.setPosition(this.rbb.previousState().getPosition());
    }

    private static class StringListMapper implements TagMapper<List<String>> {
        @Override
        public TagAccessor<List<String>> apply(Class<List<String>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setList(fieldName, o, s -> new TagCompound().setString("s", s)),
                    d -> d.getList(fieldName, t -> t.getString("s"))
            );
        }
    }
}
