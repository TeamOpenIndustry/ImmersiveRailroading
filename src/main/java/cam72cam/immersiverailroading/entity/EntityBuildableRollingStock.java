package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemPlate;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.AssemblyStep;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.net.BuildableStockSyncPacket;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IWorldData;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;

public class EntityBuildableRollingStock extends EntityRollingStock implements IWorldData {
	@TagField("isBuilt")
	private boolean isBuilt = false;
	@TagField(value = "builtItems", typeHint = ItemComponentType.class)
	private List<ItemComponentType> builtItems = new ArrayList<>();

	public void setComponents(List<ItemComponentType> items) {
		this.builtItems = new ArrayList<>(items);
		this.isBuilt = false;
		this.isBuilt = getMissingItemComponents().isEmpty();
		
		if (getWorld().isServer) {
			new BuildableStockSyncPacket(this).sendToObserving(this);
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
		new BuildableStockSyncPacket(this).sendToObserving(this);
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
		
		for (int i = 0; i < player.getInventory().getSlotCount(); i ++) {
			ItemStack found = player.getInventory().get(i);
			if (found.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
				ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(found);
				if (data.def.equals(this.getDefinition())) {
					if ((player.isCreative() || data.gauge == this.gauge) && !data.requiresHammering()) {
						if (toAdd.contains(data.componentType)) {
							addComponent(data.componentType);
							player.getInventory().extract(i, 1, false);
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
		
		for (int i = 0; i < player.getInventory().getSlotCount(); i ++) {
			ItemStack found = player.getInventory().get(i);
			if (found.is(IRItems.ITEM_PLATE)) {
				ItemPlate.Data data = new ItemPlate.Data(found);
				if (data.gauge == this.gauge) {
					switch (data.type) {
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
				
				for (int i = 0; i < player.getInventory().getSlotCount(); i ++) {
					ItemStack found = player.getInventory().get(i);
					if (found.is(Fuzzy.WOOD_PLANK)) {
						ItemStack itemUsed = player.getInventory().extract(i, woodUsed, false);
						
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
			
			for (int i = 0; i < player.getInventory().getSlotCount(); i ++) {
				ItemStack found = player.getInventory().get(i);
				if (found.is(IRItems.ITEM_PLATE)) {
					ItemPlate.Data data = new ItemPlate.Data(found);
					if (data.gauge == this.gauge) {
						if (data.type == type.getPlateType()) {
							ItemStack itemUsed = player.getInventory().extract(i, platesUsed, false);
							
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
		new BuildableStockSyncPacket(this).sendToObserving(this);
		
		
		ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
		ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
		data.def = getDefinition();
		data.gauge = gauge;
		data.componentType = toRemove;
		data.write();
		getWorld().dropItem(item, player.getBlockPosition());

		if (this instanceof EntityMoveableRollingStock) {
			((EntityMoveableRollingStock)this).clearHeightMap();
		}
		
		return toRemove;
	}
	
	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (getWorld().isServer && !player.hasPermission(Permissions.STOCK_ASSEMBLY)) {
			return ClickResult.PASS;
		}
		if (player.getHeldItem(hand).is(IRItems.ITEM_LARGE_WRENCH) || player.getHeldItem(hand).is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
			if (getWorld().isServer) {
				if (!player.isCrouching()) {
					addNextComponent(player);
				} else {
					this.removeNextComponent(player);
				}
			}
			return ClickResult.ACCEPTED;
		}
        return ClickResult.PASS;
	}

	@Override
	public void load(TagCompound tag) {
		onAssemble();
	}

	@Override
	public void save(TagCompound data) {
		// NOP
	}

	@Override
    public void onDamage(DamageType type, Entity source, float amount, boolean bypassArmor) {
		super.onDamage(type, source, amount, bypassArmor);

		if (isDead() && getWorld().isServer) {
			onDissassemble();
		}

		if (this.isDead() && shouldDropItems(type, amount)) {
			if(getWorld().isServer && !Config.ConfigBalance.StockDropInCreativeMode && source.isPlayer() && source.asPlayer().isCreative()){
				return;
			}

			if (isBuilt) {
				ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK, 1);
				ItemRollingStock.Data data = new ItemRollingStock.Data(item);
				data.def = getDefinition();
				data.gauge = gauge;
				data.texture = getTexture();
				data.write();
				getWorld().dropItem(item, source != null ? source.getBlockPosition() : getBlockPosition());
			} else {
				for (ItemComponentType component : this.builtItems) {
					ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
					ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
					data.def = getDefinition();
					data.gauge = gauge;
					data.componentType = component;
					data.write();
					System.out.println(component);
					System.out.println(item.getTagCompound());
					getWorld().dropItem(item, source != null ? source.getBlockPosition() : getBlockPosition());
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
