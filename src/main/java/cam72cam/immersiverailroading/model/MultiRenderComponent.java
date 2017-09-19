package cam72cam.immersiverailroading.model;

import java.util.List;

public class MultiRenderComponent extends RenderComponent {
	public MultiRenderComponent(List<RenderComponent> subComponents) {
		super(subComponents.get(0).def);
		for (RenderComponent c : subComponents) {
			modelIDs.addAll(c.modelIDs);
		}
	}
}
