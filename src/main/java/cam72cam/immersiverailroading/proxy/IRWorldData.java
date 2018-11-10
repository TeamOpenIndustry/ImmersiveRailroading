package cam72cam.immersiverailroading.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.WorldSavedData;

public class IRWorldData extends WorldSavedData {
	
	private static String serialize(List<BlockPos> connections) {
		return connections.stream()
				.map((BlockPos pos) -> pos.toLong() + "")
				.collect(Collectors.joining(","));
	}
	private static NBTTagCompound serialize(Map<BlockPos, List<BlockPos>> data) {
		NBTTagCompound nbt = new NBTTagCompound();
		for (BlockPos pos : data.keySet()) {
			nbt.setString(pos.toLong() + "", serialize(data.get(pos)));
		}
		return nbt;
	}
	
	private static List<BlockPos> deserialize(String connections) {
		return Arrays.stream(connections.split(","))
			.map((String pos) -> BlockPos.fromLong(Long.parseLong(pos)))
			.collect(Collectors.toList());
	}
	private static Map<BlockPos, List<BlockPos>> deserialize(NBTTagCompound nbt) {
		Map<BlockPos, List<BlockPos>> data = new HashMap<BlockPos, List<BlockPos>>();
		for (String pos : nbt.getKeySet()) {
			data.put(BlockPos.fromLong(Long.parseLong(pos)), deserialize(nbt.getString(pos)));
		}
		return data;
	}
	
	private Map<BlockPos, List<BlockPos>> main_to_gags = new HashMap<BlockPos, List<BlockPos>>();
	private Map<BlockPos, List<BlockPos>> gag_to_mains = new HashMap<BlockPos, List<BlockPos>>();

	public IRWorldData(String name) {
		super(name);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		
		if (nbt.hasKey("electrification")) {
			main_to_gags = deserialize(nbt.getCompoundTag("electrification"));
		} else {
			main_to_gags = new HashMap<BlockPos, List<BlockPos>>();
		}

		gag_to_mains.clear();
		for (BlockPos main : main_to_gags.keySet()) {
			for (BlockPos gag : main_to_gags.get(main)) { 
				List<BlockPos> mains = gag_to_mains.get(gag);
				if (mains == null) {
					mains = new ArrayList<BlockPos>();
				}
				mains.add(main);
				gag_to_mains.put(gag, mains);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (main_to_gags.size() > 0) {
			nbt.setTag("electrification", serialize(main_to_gags));
		}
		return nbt;
	}
	

	public void addElectrical(BlockPos main, List<BlockPos> connections) {
		main_to_gags.put(main, connections);
		
		for (BlockPos gag : connections) { 
			List<BlockPos> mains = gag_to_mains.get(gag);
			if (mains == null) {
				mains = new ArrayList<BlockPos>();
			}
			mains.add(main);
			gag_to_mains.put(gag, mains);
		}
		this.markDirty();
	}
	public boolean isElectrical(BlockPos pos) {
		//TODO
		return gag_to_mains.containsKey(pos) || main_to_gags.containsKey(pos);
	}
	public void removeElectrical(BlockPos main) {
		List<BlockPos> gags = main_to_gags.remove(main);
		if (gags != null) {
			for (BlockPos gag : gags) {
				List<BlockPos> mains = gag_to_mains.get(gag);
				mains.remove(main); // modifying ptr to list (I think)
				if (mains.size() == 0) {
					gag_to_mains.remove(gag);
				}
			}
		}
	}

	public Iterable<BlockPos> iterateElecrified(BlockPos startPos) {
		// Assume start pos is an electrical track
		
		return new Iterable<BlockPos>() {

			@Override
			public Iterator<BlockPos> iterator() {
				return new Iterator<BlockPos>() {
					// ONLY MAIN BLOCKS!
					Set<BlockPos> visited = new HashSet<BlockPos>();
					List<BlockPos> current = null;
					List<BlockPos> next = null;

					@Override
					public boolean hasNext() {
						return current == null || current.size() != 0;
					}

					@Override
					public BlockPos next() {
						if (current == null) {
							//Starting
							BlockPos firstPos = startPos;
							
							if (!main_to_gags.containsKey(firstPos)) {
								firstPos = gag_to_mains.get(firstPos).get(0);
							}
							
							current = new ArrayList<BlockPos>();
							current.add(firstPos);
							visited.add(firstPos);
							genNext();
						}
						
						BlockPos nextPos = current.remove(0);
						
						if (current.size() == 0) {
							current = next;
							genNext();
						}
						return nextPos;
					}

					private void genNext() {
						next = new ArrayList<BlockPos>();
						Set<BlockPos> potentials = new HashSet<BlockPos>();
						for (BlockPos main : current) {
							List<BlockPos> gags = main_to_gags.get(main);
							for (BlockPos gag : gags) {
								potentials.add(gag);
								// Look for connections around this connection
								for (EnumFacing facing : EnumFacing.HORIZONTALS) {
									potentials.add(gag.offset(facing));
								}
							}
						}
						for (BlockPos potential : potentials) {
							List<BlockPos> mains = gag_to_mains.get(potential);
							if (mains != null) {
								for (BlockPos main : mains) {
									if (!visited.contains(main)) {
										visited.add(main);
										next.add(main);
									}
									/*
									if (!next.contains(main) && !current.contains(main) && !visited.contains(main)) {
										next.add(main);
									}*/
								}
							}
						}
					}
				};
			}
		};
	}
}
