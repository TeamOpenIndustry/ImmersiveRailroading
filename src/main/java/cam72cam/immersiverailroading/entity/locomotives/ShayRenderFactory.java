package cam72cam.immersiverailroading.entity.locomotives;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.IRenderFactory;;

public class ShayRenderFactory implements IRenderFactory<Shay> {

	public static final IRenderFactory<Shay> INSTANCE = new ShayRenderFactory();

	@Override
	public Render<? super Shay> createRenderFor(RenderManager manager) {
		System.out.println("ASKDLASKDL:AS");
		return new ShayRender(manager);
	}
}
