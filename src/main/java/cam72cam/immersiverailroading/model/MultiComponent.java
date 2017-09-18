package cam72cam.immersiverailroading.model;

import java.util.List;

public class MultiComponent extends Component {
	public MultiComponent(List<Component> subComponents) {
		super(subComponents.get(0).def);
		for (Component c : subComponents) {
			modelIDs.addAll(c.modelIDs);
		}
	}
}
