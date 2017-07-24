package cam72cam.immersiverailroading.library;

import org.lwjgl.input.Keyboard;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/*
 * yeah, yeah, yeah, I know
 * this is not what enums are supposed to be for
 */
public enum KeyBindings {
	// TODO bindings per loco type
	THROTTLE_UP("Increase Throttle", Keyboard.KEY_NUMPAD8),
	THROTTLE_DOWN("Decrease Throttle", Keyboard.KEY_NUMPAD5),
	THROTTLE_ZERO("Zero Throttle", Keyboard.KEY_ADD),
	HORN("Sound Horn", Keyboard.KEY_NUMPADENTER)
	;
	
	private KeyBinding binding;

	private KeyBindings(String text, int keycode) {
		this.binding = new KeyBinding(text, keycode, "key.categories." + ImmersiveRailroading.MODID);
	}
	
	@SideOnly(Side.CLIENT)
	public boolean isPressed() {
		return binding.isPressed();
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerKeyBindings() {
		for (KeyBindings binding : KeyBindings.values()) {
			ClientRegistry.registerKeyBinding(binding.binding);
		}
	}
}
