package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;

import java.util.List;
import java.util.concurrent.*;

public class RailRender {
	private static final ExpireableMap<String, RailRender> cache = new ExpireableMap<>();

	private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
			5L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(),
			runnable -> {
				Thread thread = new Thread(runnable);
				thread.setName("IR-TrackLoader");
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			});


	private final RailInfo info;
	private boolean isLoaded;
	private boolean isLoading;
	private List<BuilderBase.VecYawPitch> renderData;
	private List<TrackBase> tracks;

	private RailRender(RailInfo info) {
		this.info = info;
		this.isLoaded = false;
	}

	private void startLoad() {
		if (!isLoading) {
			isLoading = true;
			pool.submit(this::load);
		}
	}

	public void load() {
		// This may have some thread safety problems client side...
		// Might need to add synchronization inside the builders
		BuilderBase builder = info.getBuilder(MinecraftClient.getPlayer().getWorld());
		renderData = builder.getRenderData();
		tracks = builder.getTracksForRender();
		isLoaded = true;
		isLoading = false;
	}
	public void renderRailModel(RenderState state) {
		if (info.settings.type == TrackItems.TURNTABLE) {
			load();
		}

		if (!isLoaded) {
			startLoad();
		} else {
			RailBuilderRender.renderRailBuilder(info, renderData, state);
		}
	}

	public void renderRailBase(RenderState state) {
		if (!isLoaded) {
			startLoad();
		} else {
			RailBaseRender.draw(info, tracks, state);
		}

	}

	public void renderRailMissing(World world, Vec3i pos, RenderState state) {
		if (!isLoaded) {
			startLoad();
		} else {
			RailBaseOverlayRender.draw(info, tracks, pos, state);
		}
	}

	public static RailRender get(RailInfo info) {
		RailRender cached = cache.get(info.uniqueID);
		if (cached == null) {
			cached = new RailRender(info);
			cache.put(info.uniqueID, cached);
		}
		return cached;
	}



	public static void render(RailInfo info, World world, Vec3i pos, boolean renderOverlay, RenderState state) {
		state.lighting(false);

		RailRender renderer = get(info);

		MinecraftClient.startProfiler("rail");
		renderer.renderRailModel(state);
		MinecraftClient.endProfiler();

		if (renderOverlay) {
			Vec3d off = info.placementInfo.placementPosition;
			// TODO Is this needed?
			off = off.subtract(new Vec3d(new Vec3i(off)));
			state.translate(-off.x, -off.y, -off.z);

			MinecraftClient.startProfiler("base");
			renderer.renderRailBase(state);
			MinecraftClient.endProfiler();

			MinecraftClient.startProfiler("overlay");
			renderer.renderRailMissing(world, pos, state);
			MinecraftClient.endProfiler();
		}
	}
}
