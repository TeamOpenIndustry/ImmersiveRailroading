package cam72cam.immersiverailroading.gui.overlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.remotecontrol.RemoteControlData;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

public class RemoteOverlay extends GuiBuilder {
	private List<RemoteOverlay> elements;

	protected RemoteOverlay(DataBlock data) throws IOException {
		super(data);
		
		elements = new ArrayList<>();

        // Children
        List<DataBlock> elem = data.getBlocks("elements");
        if (elem == null) {
            elem = data.getBlocks("element");
        }
        if (elem != null) {
            for (DataBlock element : elem) {
                elements.add(new RemoteOverlay(element));
            }
        }
	}
	
    public static RemoteOverlay parse(Identifier overlay) throws IOException {
        return new RemoteOverlay(DataBlock.load(overlay));
    }

	public void render(RenderState state, RemoteControlData data) {
		render(data, state.clone().color(1,1,1,1), GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xFFFFFFFF);
	}

	// TODO better render code
	private void render(RemoteControlData data, RenderState state, int maxx, int maxy, int baseColor) {
		float value = getValue(data);
        if (translucent) {
            if (value == 0) {
                return;
            }
            float alpha = (baseColor >> 24 & 255) / 255f * value;
            baseColor = baseColor & 0x00FFFFFF | ((int)(alpha * 255f) << 24);
        }

        state = state.clone(); // TODO mem opt?
        applyPosition(state.model_view(), maxx, maxy);
        applyValue(state.model_view(), value);

        Float colorKey = null;
        for (float key : colors.keySet()) {
            if (key <= value && (colorKey == null || key > colorKey)) {
                colorKey = key;
            }
        }

        if (colorKey != null && colors.containsKey(colorKey)) {
            float oldAlpha = (baseColor >> 24 & 255) / 255f;

            int newColor = colors.get(colorKey);
            float newAlpha = (newColor >> 24 & 255) / 255f;
            baseColor = newColor & 0x00FFFFFF | ((int)(newAlpha * oldAlpha * 255f) << 24);
        }

        if (colorKey != null || translucent) {
            state.color((baseColor >> 16 & 255) / 255.0f, (baseColor >> 8 & 255) / 255.0f, (baseColor & 255) / 255.0f, (baseColor >> 24 & 255) / 255.0f);
        }

        if (image != null) {
            DirectDraw draw = new DirectDraw();
            draw.vertex(0, 0, 0).uv(0, 0);
            draw.vertex(0, height, 0).uv(0, 1);
            draw.vertex(width, height, 0).uv(1, 1);
            draw.vertex(width, 0, 0).uv(1, 0);
            draw.draw(state.clone()
                    .texture(Texture.wrap(image))
                    .alpha_test(false)
                    .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA))
            );
        }
        if (text != null) {
            String out = text;
            for (Stat stat : Stat.values()) {
                String statStr = stat.toString();
                int index = out.indexOf(statStr);
                if (index == -1 /* !contain() */ ) continue;

                if (stat.hasDecimalSetting()) {
                    int decimalIndex = index + statStr.length();

                    if (decimalIndex + 1 < out.length() // Check if we have both dot and number
                        && out.charAt(decimalIndex) == '.'
                        && Character.isDigit(out.charAt(decimalIndex + 1))) {
                        // [stat].[digit(0~5)]
                        int dig = Character.getNumericValue(out.charAt(decimalIndex + 1));
                        dig = MathUtil.clamp(dig, 0, 5);

                        out = out.replace(out.substring(index, decimalIndex + 2), getStat(data, stat, dig));
                    } else {
                        out = out.replace(statStr, getStat(data, stat, null));
                    }
                } else {
                    out = out.replace(statStr, getStat(data, stat, null));
                }
            }
            for (GuiText label : new GuiText[]{GuiText.LABEL_THROTTLE, GuiText.LABEL_REVERSER, GuiText.LABEL_BRAKE}) {
                out = out.replace(label.getValue(), label.toString());
            }
            // Text is 8px tall
            float scale = textHeight / 8f;
            Matrix4 mat = state.model_view().copy();
            mat.scale(scale, scale, scale);
            GUIHelpers.drawCenteredString(out, 0, 0, baseColor, mat);
        }
        for (RemoteOverlay element : elements) {
            element.render(data, state, maxx, maxy, baseColor);
        }
	}
	
	private float getValue(RemoteControlData data) {
        float value = 0;
        if (readout != null) {
        	switch (readout) {
			case THROTTLE: {
				return data.throttle;
			}
			case BRAKE_PRESSURE: {
				return data.brakePressure;
			}
			case INDEPENDENT_BRAKE: {
				return data.indBrake;
			}
			case REVERSER: {
				return data.reverser;
			}
			default:
				return 0;
			}
        } else if (setting != null) {
            if (!ConfigGraphics.settings.containsKey(setting) && setting_default != null) {
                ConfigGraphics.settings.put(setting, setting_default);
            }

            value = ConfigGraphics.settings.getOrDefault(setting, 0f);
        }

        switch (clamp) {
            case FLOOR:
                value = value < 0.95 ? 0 : 1;
            case CEIL:
                value = value < 0.05 ? 0 : 1;
			default:
				break;
        }

        if (invert) {
            value = 1 - value;
        }

        return value;
	}
	
	private String getStat(RemoteControlData data, Stat stat, Integer digit) {
		String format = Stat.formats[digit != null ? digit : 0];
		switch(stat) {
			case SPEED:
				Speed speed = data.speed;
				switch (ConfigGraphics.speedUnit) {
	                case mph:
	                    return String.format(format, Math.abs(speed.imperial()));
	                case ms:
	                    return String.format(format, Math.abs(speed.metersPerSecond()));
	                case kmh:
	                    return String.format(format, Math.abs(speed.metric()));
	            }
			default:
				return "no stat";
		}
	}

}
