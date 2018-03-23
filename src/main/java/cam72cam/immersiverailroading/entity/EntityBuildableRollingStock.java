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
import cam72cam.immersiverailroading.library.AssemblyStep;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.StockDeathType;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.net.BuildableStockSyncPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class EntityBuildableRollingStock extends EntityRollingStock {
	private boolean isBuilt = false;
	private List<ItemComponentType> builtItems = new ArrayList<ItemComponentType>();
	public EntityBuildableRollingStock(World world, String defID) {
		super(world, defID);
	}
	//TODO PACKET
	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		setComponents(BufferUtil.readItemComponentTypes(additionalData));
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		BufferUtil.writeItemComponentTypes(buffer, builtItems);
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setBoolean("isBuilt", this.isBuilt);
		
		int[] items = new int[builtItems.size()];
		for (int i = 0; i < items.length; i ++) {
			items[i] = builtItems.get(i).ordinal();
		}
		
		nbt.setIntArray("builtItems", items);
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		
		isBuilt = nbt.getBoolean("isBuilt");
		
		if (isBuilt) {
			// Grandfathered in
			this.setComponents(this.getDefinition().getItemComponents());
		} else {
			// Partially built
			List<ItemComponentType> newItems = new ArrayList<ItemComponentType>();
			
			int[] items = nbt.getIntArray("builtItems");
			
			for (int i = 0; i < items.length; i++) {
				newItems.add(ItemComponentType.values()[items[i]]);
			}
			
			this.setComponents(newItems);
		}
	}
	
	public void setComponents(List<ItemComponentType> items) {
		this.builtItems = new ArrayList<ItemComponentType>(items);
		this.isBuilt = false;
		this.isBuilt = getMissingItemComponents().isEmpty();
		
		if (!world.isRemote) {
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
			return this.isBuilt;
		}
		
		for (ItemComponentType item : this.getMissingItemComponents()) {
			if (item.isWheelPart()) {
				return false;
			}
		}
		
		return true;
	}
	
	public List<ItemComponentType> getMissingItemComponents() {
		List<ItemComponentType> missing = new ArrayList<ItemComponentType>();
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

	public boolean hasAllWheels() {
		for (ItemComponentType item : this.getMissingItemComponents()) {
			if (item.isWheelPart()) {
				return false;
			}
		}
		return true;
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
	
	public void addNextComponent(EntityPlayer player) {
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
		
		for (int i = 0; i < player.inventory.getSizeInventory(); i ++) {
			ItemStack found = player.inventory.getStackInSlot(i);
			if (found.getItem() == IRItems.ITEM_ROLLING_STOCK_COMPONENT) {
				if (ItemDefinition.getID(found).equals(this.defID)) {
					if ((player.isCreative() || ItemGauge.get(found) == this.gauge) && !ItemRollingStockComponent.requiresHammering(found)) {
						ItemComponentType type = ItemComponent.getComponentType(found);
						if (toAdd.contains(type)) {
							addComponent(type);
							player.inventory.decrStackSize(i, 1);
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
		
		for (int i = 0; i < player.inventory.getSizeInventory(); i ++) {
			ItemStack found = player.inventory.getStackInSlot(i);
			if (found.getItem() == IRItems.ITEM_PLATE) {
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
			if (found.getItem() == Item.getItemFromBlock(Blocks.PLANKS)) {
				wood += found.getCount();
			}
		}
		
		for (ItemComponentType type : toAdd) {
			if (type.isWooden(getDefinition())) {
				int woodUsed = type.getWoodCost(this.gauge, this.getDefinition());
				if (wood < woodUsed) {
					continue;
				}
				
				for (int i = 0; i < player.inventory.getSizeInventory(); i ++) {
					ItemStack found = player.inventory.getStackInSlot(i);
					if (found.getItem() == Item.getItemFromBlock(Blocks.PLANKS)) {
						ItemStack itemUsed = player.inventory.decrStackSize(i, woodUsed);
						
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
			
			for (int i = 0; i < player.inventory.getSizeInventory(); i ++) {
				ItemStack found = player.inventory.getStackInSlot(i);
				if (found.getItem() == IRItems.ITEM_PLATE) {
					if (ItemGauge.get(found) == this.gauge) {
						if (ItemPlateType.get(found) == type.getPlateType()) {
							ItemStack itemUsed = player.inventory.decrStackSize(i, platesUsed);
							
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
			player.sendMessage(new TextComponentString(str));
		}
	}
	
	public ItemComponentType removeNextComponent(EntityPlayer player) {
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
		
		
		ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1, 0);
		ItemDefinition.setID(item, defID);
		ItemGauge.set(item, gauge);
		ItemComponent.setComponentType(item, toRemove);
		world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, item));
		
		if (this instanceof EntityMoveableRollingStock) {
			((EntityMoveableRollingStock)this).clearHeightMap();
		}
		
		return toRemove;
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (super.processInitialInteract(player, hand)) {
			return true;
		}
		
		if (world.isRemote) {
			return false;
		}
		if (player.getHeldItem(hand).getItem() == IRItems.ITEM_LARGE_WRENCH || player.getHeldItem(hand).getItem() == IRItems.ITEM_ROLLING_STOCK_COMPONENT) {
			if (!player.isSneaking()) {
				addNextComponent(player);
			} else {
				this.removeNextComponent(player);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void onDeath(StockDeathType type) {
		super.onDeath(type);
		
		if (this.isBuilt && type != StockDeathType.CATACYSM) {
			ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK, 1, 0);
			ItemDefinition.setID(item, defID);
			ItemGauge.set(item, gauge);
			world.spawnEntity(new EntityItem(world, posX, posY, posZ, item));
		} else {
			for (ItemComponentType component : this.builtItems) {
				ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1, 0);
				ItemDefinition.setID(item, defID);
				ItemGauge.set(item, gauge);
				ItemComponent.setComponentType(item, component);
				world.spawnEntity(new EntityItem(world, posX, posY, posZ, item));
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
