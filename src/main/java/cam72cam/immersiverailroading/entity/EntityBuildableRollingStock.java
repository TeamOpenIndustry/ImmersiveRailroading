package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemPlateType;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.library.AssemblyStep;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.net.BuildableStockSyncPacket;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;

public class EntityBuildableRollingStock extends EntityRollingStock {
	private boolean isBuilt = false;
	private List<ItemComponentType> builtItems = new ArrayList<>();

	public EntityBuildableRollingStock(ModdedEntity entity) {
		super(entity);
	}

	@Override
	public void save(TagCompound data) {
		super.save(data);
		data.setBoolean("isBuilt", isBuilt);
		data.setEnumList("builtItems", builtItems);
	}
	
	@Override
	public void load(TagCompound data) {
		super.load(data);

		isBuilt = data.getBoolean("isBuilt");
		if (isBuilt) {
			setComponents(this.getDefinition().getItemComponents());
		} else {
			setComponents(data.getEnumList("builtItems", ItemComponentType.class));
		}
	}

	@Override
	public void saveSpawn(TagCompound data) {
		super.saveSpawn(data);
		data.setBoolean("isBuilt", isBuilt);
		data.setEnumList("builtItems", builtItems);
	}

	@Override
	public void loadSpawn(TagCompound data) {
		super.loadSpawn(data);
		isBuilt = data.getBoolean("isBuilt");
		setComponents(data.getEnumList("builtItems", ItemComponentType.class));
	}
	
	public void setComponents(List<ItemComponentType> items) {
		this.builtItems = new ArrayList<>(items);
		this.isBuilt = false;
		this.isBuilt = getMissingItemComponents().isEmpty();
		
		if (getWorld().isServer) {
			this.sendToObserving(new BuildableStockSyncPacket(this));
		}

		if (this.isBuilt()) {
			this.onAssemble();
		} else {
			this.onDissassemble();
		}
		if (this instanceof EntityMoveableRollingStock) {
			((EntityMoveableRollingStock)this).clearHeightMap();
		}
	}
	
	public List<ItemComponentType> getItemComponents() {
		return builtItems;
	}
	
	public boolean isBuilt() {
		return this.isBuilt;
	}
	

	public boolean areWheelsBuilt() {
		if (this.isBuilt) {
			return true;
		}
		
		for (ItemComponentType item : this.getMissingItemComponents()) {
			if (item.isWheelPart()) {
				return false;
			}
		}
		
		return true;
	}
	
	public List<ItemComponentType> getMissingItemComponents() {
		List<ItemComponentType> missing = new ArrayList<>();
		if (this.isBuilt) {
			return missing;
		}
		
		missing.addAll(this.getDefinition().getItemComponents());
		
		for (ItemComponentType item : this.getItemComponents()) {
			// Remove first occurrence
			missing.remove(item);
		}
		
		return missing;
	}

	public void addComponent(ItemComponentType item) {
		this.builtItems.add(item);
		this.isBuilt = getMissingItemComponents().isEmpty();
		if (isBuilt) {
			onAssemble();
		}
		if (this instanceof EntityMoveableRollingStock) {
			((EntityMoveableRollingStock)this).clearHeightMap();
		}
		this.sendToObserving(new BuildableStockSyncPacket(this));
	}
	
	public void addNextComponent(Player player) {
		if (this.isBuilt()) {
			player.sendMessage(ChatText.STOCK_BUILT.getMessage(this.getDefinition().name()));
			return;
		}
		
		List<ItemComponentType> toAdd = new ArrayList<ItemComponentType>();
		
		for (AssemblyStep step : AssemblyStep.values()) {
			for (ItemComponentType component : this.getMissingItemComponents()) {
				if (component.step == step) {
					toAdd.add(component);
				}
			}
			if (toAdd.size() != 0) {
				break;
			}
		}
		
		for (int i = 0; i < player.internal.inventory.getSizeInventory(); i ++) {
			cam72cam.mod.item.ItemStack found = new cam72cam.mod.item.ItemStack(player.internal.inventory.getStackInSlot(i));
			if (found.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
				if (ItemDefinition.getID(found).equals(this.defID)) {
					if ((player.isCreative() || ItemGauge.get(found) == this.gauge) && !ItemRollingStockComponent.requiresHammering(found)) {
						ItemComponentType type = ItemComponent.getComponentType(found);
						if (toAdd.contains(type)) {
							addComponent(type);
							player.internal.inventory.decrStackSize(i, 1);
							return;
						}
					}
				}
			}
		}
		
		int largePlates = 0;
		int mediumPlates = 0;
		int smallPlates = 0;
		int wood = 0;
		
		for (int i = 0; i < player.internal.inventory.getSizeInventory(); i ++) {
			cam72cam.mod.item.ItemStack found = new cam72cam.mod.item.ItemStack(player.internal.inventory.getStackInSlot(i));
			if (found.is(IRItems.ITEM_PLATE)) {
				if (ItemGauge.get(found) == this.gauge) {
					switch (ItemPlateType.get(found)) {
					case LARGE:
						largePlates+=found.getCount();
						break;
					case MEDIUM:
						mediumPlates+=found.getCount();
						break;
					case SMALL:
						smallPlates+=found.getCount();
						break;
					default:
						break;
					}
				}
			}
			if (found.is(Fuzzy.WOOD_PLANK)) {
				wood += found.getCount();
			}
		}
		
		for (ItemComponentType type : toAdd) {
			if (type.isWooden(getDefinition())) {
				int woodUsed = type.getWoodCost(this.gauge, this.getDefinition());
				if (wood < woodUsed) {
					continue;
				}
				
				for (int i = 0; i < player.internal.inventory.getSizeInventory(); i ++) {
					cam72cam.mod.item.ItemStack found = new cam72cam.mod.item.ItemStack(player.internal.inventory.getStackInSlot(i));
					if (found.is(Fuzzy.WOOD_PLANK)) {
						net.minecraft.item.ItemStack itemUsed = player.internal.inventory.decrStackSize(i, woodUsed);
						
						woodUsed -= itemUsed.getCount();
						
						if (woodUsed <= 0) {
							break;
						}
					}
				}
				
				addComponent(type);
			}
		}
		
		for (ItemComponentType type : toAdd) {
			if (type.isWooden(getDefinition())) {
				continue;
			}
			
			int platesStart = 0;
			int platesUsed = 0;
			
			switch (type.crafting) {
			case PLATE_LARGE:
				platesStart = largePlates;
				break;
			case PLATE_MEDIUM:
				platesStart = mediumPlates;
				break;
			case PLATE_SMALL:
				platesStart = smallPlates;
				break;
			default:
				continue;
			}
			
			platesUsed = type.getPlateCost(this.gauge, this.getDefinition());
			if (platesStart < platesUsed) {
				continue;
			}
			
			for (int i = 0; i < player.internal.inventory.getSizeInventory(); i ++) {
				cam72cam.mod.item.ItemStack found = new cam72cam.mod.item.ItemStack(player.internal.inventory.getStackInSlot(i));
				if (found.is(IRItems.ITEM_PLATE)) {
					if (ItemGauge.get(found) == this.gauge) {
						if (ItemPlateType.get(found) == type.getPlateType()) {
							ItemStack itemUsed = new ItemStack(player.internal.inventory.decrStackSize(i, platesUsed));
							
							platesUsed -= itemUsed.getCount();
							
							if (platesUsed <= 0) {
								break;
							}
						}
					}
				}
			}
			
			addComponent(type);
			
			return;
		}
		
		Map<ItemComponentType, Integer> addMap = new HashMap<ItemComponentType, Integer>();
		for (ItemComponentType component : toAdd) {
			if (!addMap.containsKey(component)) {
				addMap.put(component, 0);
			}
			addMap.put(component, addMap.get(component)+1);
		}
		player.sendMessage(ChatText.STOCK_MISSING.getMessage(""));
		
		for (ItemComponentType component : addMap.keySet()) {
			String str = String.format("%d x %s", addMap.get(component), component);
			if (!component.isWooden(getDefinition())) {
			switch (component.crafting) {
				case CASTING:
				case CASTING_HAMMER:
				case PLATE_BOILER:
					str += String.format(" (%s)", component.crafting.toString());
					break;
				case PLATE_LARGE:
				case PLATE_MEDIUM:
				case PLATE_SMALL:
					str += String.format(" (%d x %s)", component.getPlateCost(gauge, getDefinition()) * addMap.get(component), component.getPlateType());
					break;
				default:
					break;
				}
			} else {
				str += String.format(" (%d x %s)", component.getWoodCost(gauge, getDefinition()), ChatText.WOOD_PLANKS.toString());
			}
			player.sendMessage(PlayerMessage.direct(str));
		}
	}
	
	public ItemComponentType removeNextComponent(Player player) {
		if (this.isBuilt) {
			this.onDissassemble();
		}
		
		this.isBuilt = false;
		if (this.builtItems.size() <= 1) {
			player.sendMessage(ChatText.STOCK_DISSASEMBLED.getMessage(this.getDefinition().name()));
			return null;
		}
		
		ItemComponentType toRemove = null;
		
		for (AssemblyStep step : AssemblyStep.reverse()) {
			for (ItemComponentType component : this.builtItems) {
				if (component == ItemComponentType.FRAME) {
					continue;
				}
				if (component.step == step) {
					toRemove = component;
					break;
				}
			}
			if (toRemove != null) {
				break;
			}
		}
		
		this.builtItems.remove(toRemove);
		this.sendToObserving(new BuildableStockSyncPacket(this));
		
		
		cam72cam.mod.item.ItemStack item = new cam72cam.mod.item.ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
		ItemDefinition.setID(item, defID);
		ItemGauge.set(item, gauge);
		ItemComponent.setComponentType(item, toRemove);
		getWorld().dropItem(item, player.getBlockPosition());

		if (this instanceof EntityMoveableRollingStock) {
			((EntityMoveableRollingStock)this).clearHeightMap();
		}
		
		return toRemove;
	}
	
	@Override
	public ClickResult onClick(Player player, Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (getWorld().isClient) {
			return ClickResult.PASS;
		}
		if (player.getHeldItem(hand).is(IRItems.ITEM_LARGE_WRENCH) || player.getHeldItem(hand).is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
			if (!player.isCrouching()) {
				addNextComponent(player);
			} else {
				this.removeNextComponent(player);
			}
			return ClickResult.ACCEPTED;
		}
        return ClickResult.PASS;
	}
	
	@Override
    public void onDamage(DamageType type, Entity source, float amount) {
		super.onDamage(type, source, amount);

		if (this.isDead() && shouldDropItems(type, amount)) {
			if (isBuilt) {
				ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK, 1);
				ItemDefinition.setID(item, defID);
				ItemGauge.set(item, gauge);
				ItemTextureVariant.set(item, texture);
				getWorld().dropItem(item, source.getBlockPosition());
			} else {
				for (ItemComponentType component : this.builtItems) {
					ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
					ItemDefinition.setID(item, defID);
					ItemGauge.set(item, gauge);
					ItemComponent.setComponentType(item, component);
					getWorld().dropItem(item, source.getBlockPosition());
				}
			}
		}
	}

	public void onAssemble() {
		// NOP
	}
	public void onDissassemble() {
		// NOP
	}
}
