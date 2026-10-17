package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.track.graph.TrackMultiGeometrySegment;
import cam72cam.immersiverailroading.track.graph.TrackNode;
import cam72cam.immersiverailroading.track.graph.TrackSingleGeometrySegment;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

		if(args[0].equals("offworld")) {
			if(player.isPresent()) {
				try{
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					int z = Integer.parseInt(args[3]);
					TrackMultiGeometrySegment trackBlock = WorldData.get(player.get().getWorld()).getTrackBlock(new Vec3i(x, y, z));
					for(Map.Entry<Gauge, TrackSingleGeometrySegment> single : trackBlock.paths.getFirst().entrySet()){
						TrackNode node = new TrackNode(single.getValue(), true, player.get().getWorld());
						TrackNode next = node.getConn(player.get().getWorld(), false);
						sender.accept(PlayerMessage.direct("node:" + node.point + " next:" + (next == null ? "null" : next.point)));

						TrackNode node2 = new TrackNode(single.getValue(), false, player.get().getWorld());
						TrackNode next2 = node2.getConn(player.get().getWorld(), true);
						sender.accept(PlayerMessage.direct("node2:" + node2.point + " next2:" + (next2 == null ? "null" : next2.point)));
					}
					return true;
				}catch(Exception e){
					sender.accept(PlayerMessage.direct("Invalid arguments exception"));
				}
			} else {
				sender.accept(PlayerMessage.direct("This command is not supported for non-players (yet)"));
			}
		}

		if(args[0].equals("getTrackBlock")) {
			if(player.isPresent()) {
				try{
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					int z = Integer.parseInt(args[3]);
					Vec3i pos = new Vec3i(x, y, z);
					TrackMultiGeometrySegment trackBlock = WorldData.get(MinecraftClient.getPlayer().getWorld()).getTrackBlock(pos);
					if(trackBlock != null) {
						sender.accept(PlayerMessage.direct("succeed"));
						return true;
					} else {
						sender.accept(PlayerMessage.direct("failed"));
					}
				}catch(Exception e){
					sender.accept(PlayerMessage.direct("Invalid arguments exception"));
				}
			} else {
				sender.accept(PlayerMessage.direct("This command is not supported for non-players (yet)"));
			}
		}

		return false;
	}
}