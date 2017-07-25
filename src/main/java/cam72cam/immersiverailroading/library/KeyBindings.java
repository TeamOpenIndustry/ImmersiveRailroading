package cam72cam.immersiverailroading.library;

import org.lwjgl.input.Keyboard;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.Minecraft;
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
	HORN("Sound Horn", Keyboard.KEY_NUMPADENTER),
	PLAYER_FORWARD(Minecraft.getMinecraft().gameSettings.keyBindForward),
	PLAYER_BACKWARD(Minecraft.getMinecraft().gameSettings.keyBindBack),
	PLAYER_LEFT(Minecraft.getMinecraft().gameSettings.keyBindLeft),
	PLAYER_RIGHT(Minecraft.getMinecraft().gameSettings.keyBindRight),
	;
	
	private KeyBinding binding;

	private KeyBindings(String text, int keycode) {
		this.binding = new KeyBinding(text, keycode, "key.categories." + ImmersiveRailroading.MODID);
	}
	private KeyBindings(KeyBinding existingBinding) {
		this.binding = existingBinding;
	}
	
	@SideOnly(Side.CLIENT)
	public boolean isPressed() {
		//TODO find away around key repeat delay
		return binding.isKeyDown();
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerKeyBindings() {
		for (KeyBindings binding : KeyBindings.values()) {
			ClientRegistry.registerKeyBinding(binding.binding);
		}
	}
}
