package cam72cam.immersiverailroading.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarArtillery;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CarArtilleryDefinition extends CarFreightDefinition {

	public enum ARTILLERYTYPE { ROCKET, GUN, NONE }
	
	private int range;
	private float muzzleVelocity;
	private float projectileMass;
	private float projectileExplosive;
	public Rotations orientLimit;
	public ARTILLERYTYPE projectileType;
	
	public CarArtilleryDefinition(String defID, JsonObject data) throws Exception {
		super(defID, data);
	}
	
	@Override
	public void parseJson(JsonObject data) throws Exception {
		super.parseJson(data);
		JsonObject artillery = data.get("artillery").getAsJsonObject();
		range = artillery.get("range").getAsInt();
		muzzleVelocity = artillery.get("velocity").getAsFloat();
		projectileMass = artillery.get("mass").getAsFloat();
		projectileExplosive = artillery.get("explosive").getAsFloat();
		orientLimit = new Rotations(artillery.get("elevation").getAsInt(), artillery.get("traverse").getAsInt(), 0);
		String typeOfProjectile = artillery.get("type").getAsString();
		switch(typeOfProjectile) {
		case "missile":
			projectileType = ARTILLERYTYPE.ROCKET;
			break;
		case "gun":
			projectileType = ARTILLERYTYPE.GUN;
			break;
		default:
			ImmersiveRailroading.warn("Invalid projectile type %s in %s", typeOfProjectile, defID);
			
		}
	}
	
	/** Returns a packaged Vec3d(projectileMass, projectileExplosive, muzzleVelocity) **/
	public Vec3d getProjectile() {
		return new Vec3d(projectileMass, projectileExplosive, muzzleVelocity);
	}
	
	public int getRange() {
		return range;
	}
	
	@Override
	public List<String> getTooltip(Gauge gauge) {
		List<String> tips = super.getTooltip(gauge);
		tips.add(GuiText.FREIGHT_CAPACITY_TOOLTIP.toString(this.getInventorySize(gauge)));
		return tips;
	}
	
	@Override
	public EntityRollingStock instance(World world) {
		return new CarArtillery(world, defID);
	}
	

	@Override
	protected Set<String> parseComponents() {
		Set<String> groups = super.parseComponents();
		
		switch (this.projectileType) {
		case ROCKET:
			for (int i = 0; i < 10; i++) {
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.MISSILE_LAUNCHER, this, groups, i), true);
				addComponentIfExists(RenderComponent.parseID(RenderComponentType.MISSILE_X, this, groups, i), false);
				addComponentIfExists(RenderComponent.parse(RenderComponentType.MISSILE_PIVOT_X, this, groups), false);
				addComponentIfExists(RenderComponent.parse(RenderComponentType.MISSILE_PIVOT_Y, this, groups), false);
			};
			break;
		case GUN:
			addComponentIfExists(RenderComponent.parse(RenderComponentType.GUN_TURRET, this, groups), true);
			addComponentIfExists(RenderComponent.parse(RenderComponentType.GUN_BREECH, this, groups), true);
			addComponentIfExists(RenderComponent.parse(RenderComponentType.GUN_BARREL, this, groups), true);
			addComponentIfExists(RenderComponent.parse(RenderComponentType.GUN_PIVOT_X, this, groups), false);
			addComponentIfExists(RenderComponent.parse(RenderComponentType.GUN_PIVOT_Y, this, groups), false);
			break;
		}
		return groups;
	}


	
	@Override
	public boolean acceptsLivestock() {
		return false;
	}	
}
