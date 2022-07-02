package cam72cam.immersiverailroading.items;

import cam72cam.mod.entity.Player;

import java.util.List;

public interface TextureSelector {

    String selectNewTexture(List<String> texNames, String currentTexture, Player player);
}
