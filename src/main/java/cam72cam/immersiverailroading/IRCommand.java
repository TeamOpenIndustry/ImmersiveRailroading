package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class IRCommand extends Command {

	@Override
	public String getPrefix() {
		return ImmersiveRailroading.MODID;
	}

	@Override
	public String getUsage() {
		return "Usage: " + ImmersiveRailroading.MODID + " (reload|debug)";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return PermissionLevel.LEVEL4;
	}

	@Override
	public boolean execute(Consumer<PlayerMessage> sender, Optional<Player> player, String[] args) {
		if (args.length == 0) {
			return false;
		}
		if (args[0].equals("cargoFill")) {
			if (player.isPresent()) {
				int distance = 2;
				if (args.length > 1) {
					try {
						distance = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.accept(PlayerMessage.direct("Invalid number " + args[1]));
						return true;
					}
				}
				IBoundingBox bb = player.get().getBounds().grow(new Vec3d(distance, 4, distance));
				List<Freight> carsNearby = player.get().getWorld().getEntities((Freight stock) -> bb.intersects(stock.getBounds()), Freight.class);
				ItemStack stack = player.get().getHeldItem(Player.Hand.PRIMARY);

				if (carsNearby.isEmpty()) {
					sender.accept(PlayerMessage.direct("No rolling stock within range to fill"));
				}

				for (Freight freight : carsNearby) {
					sender.accept(PlayerMessage.direct(String.format("Filling %s@%s with %s", freight.getDefinition().name(), new Vec3i(freight.getPosition()), stack.getDisplayName())));
					for (int i = 0; i < freight.cargoItems.getSlotCount(); i++) {
						if (stack.isEmpty() || freight.cargoItems.get(i).isEmpty()) {
							freight.cargoItems.set(i, stack.copy());
						}
					}
				}
			} else {
				sender.accept(PlayerMessage.direct("This command is not supported for non-players (yet)"));
			}
			return true;
		}
		
		if (args[0].equals("debug")) {
			if (player.isPresent()) {
				List<EntityRollingStock> ents = player.get().getWorld().getEntities(EntityRollingStock.class);
				ents.sort(Comparator.comparing(a -> a.getUUID().toString()));
				for (EntityRollingStock ent : ents) {
					sender.accept(PlayerMessage.direct(String.format("%s : %s - %s : %s", ent.getUUID(), ent.getId(), ent.getDefinitionID(), ent.getPosition())));
				}
			} else {
				sender.accept(PlayerMessage.direct("This command is not supported for non-players (yet)"));
			}
			return true;
		}
		return false;
	}
}