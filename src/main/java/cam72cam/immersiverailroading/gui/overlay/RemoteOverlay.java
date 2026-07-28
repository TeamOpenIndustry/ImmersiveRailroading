package cam72cam.immersiverailroading.gui.overlay;

import java.awt.Polygon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.net.RemoteControlClientPacket;
import cam72cam.immersiverailroading.remotecontrol.RemoteControlData;
import cam72cam.immersiverailroading.remotecontrol.WirelessRemotecontrolClient;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

public class RemoteOverlay extends GuiBuilder {
	private List<RemoteOverlay> elements;
	
	private static RemoteOverlay target = null;
    private static RemoteOverlay scrollTarget = null;
    private static int lastScrolled = 0;

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
				value = data.throttle;
			}
			case BRAKE_PRESSURE: {
				value = data.brakePressure;
			}
			case INDEPENDENT_BRAKE: {
				value = data.indBrake;
			}
			case REVERSER: {
				value = data.reverser;
			}
			case EMERGENCY: {
				value = data.emergency ? 1 : 0;
			}
			case HORN, WHISTLE: {
				value = data.horn > 0 ? 1 : 0;
			}
			default:
				value = 0;
			}
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
            case UNITS_SPEED:
                return ConfigGraphics.speedUnit.toUnitString();
			default:
				return "not valid";
		}
	}
	
	private RemoteOverlay find(RemoteControlData data, Matrix4 matrix, int maxx, int maxy, int x, int y) {
	    float value = getValue(data);
	    if (translucent && value == 0) {
	        return null;
	    }
	    matrix = matrix.copy();
	    applyPosition(matrix, maxx, maxy);
	    applyValue(matrix, value);
	    for (RemoteOverlay element : elements) {
	        RemoteOverlay found = element.find(data, matrix, maxx, maxy, x, y);
	        if (found != null) {
	            return found;
	        }
	    }

	    if (interactable() && (image != null || text != null)) {
	        if (readout == null) {
	            return null;
	        }
	        switch (readout) {
	            case THROTTLE:
	            case REVERSER:
	            case BRAKE_PRESSURE:
	            case INDEPENDENT_BRAKE:
	            case EMERGENCY:
	            case WHISTLE, HORN:
	                break;
	            default:
	                return null;
	        }

	        int border = 2;
	        Vec3d cornerA = matrix.apply(new Vec3d((image == null ? -width : 0) - border, -border, 0));
	        Vec3d cornerB = matrix.apply(new Vec3d((image == null ? -width : 0) - border, height + border, 0));
	        Vec3d cornerC = matrix.apply(new Vec3d(width + border, -border, 0));
	        Vec3d cornerD = matrix.apply(new Vec3d(width + border, height + border, 0));

	        Polygon poly = new Polygon(
	                new int[]{(int) cornerA.x, (int) cornerB.x, (int) cornerC.x, (int) cornerD.x},
	                new int[]{(int) cornerA.y, (int) cornerB.y, (int) cornerC.y, (int) cornerD.y},
	                4
	        );
	        if (poly.getBounds2D().contains(x, y)) {
	            return this;
	        }
	    }
	    return null;
	}
	
	private boolean interactable() {
	    return tlx != 0 || tly != 0 || rotdeg != 0 || scalex != null || scaley != null || toggle;
	}
	
	private void onMouseMove(RemoteControlData data, Matrix4 matrix, RemoteOverlay target, int maxx, int maxy, int x, int y) {
	    float value = getValue(data);
	    matrix = matrix.copy();
	    applyPosition(matrix, maxx, maxy);
	    Matrix4 preApply = matrix.copy();
	    applyValue(matrix, value);

	    if (target == this) {
	    	if (toggle) {
	    		return;
	    	}
	        float closestValue = value;
	        double closestDelta = 999999;

	        for (float checkValue = 0; checkValue <= 1; checkValue += 0.01) {
	            Matrix4 temp = preApply.copy();
	            if (tlx != 0 || tly != 0) {
	                temp.translate(tlx * checkValue, tly * checkValue, 0);
	            }
	            if (rotdeg != 0) {
	                temp.translate(rotx, roty, 0);
	                temp.rotate(Math.toRadians(rotdeg * checkValue + rotoff), 0, 0, 1);
	                temp.translate(-rotx, -roty, 0);
	            }
	            if (scalex != null || scaley != null) {
	                temp.scale(scalex != null ? scalex * checkValue : 1, scaley != null ? scaley * checkValue : 1, 1);
	            }
	            Vec3d checkMiddle = temp.apply(new Vec3d(width / 2f, height / 2f, 0));
	            double delta = checkMiddle.distanceTo(new Vec3d(x, y, 0));
	            if (delta < closestDelta) {
	                closestDelta = delta;
	                closestValue = checkValue;
	            }
	        }

	        if (closestValue != value) {
	            float val = invert ? 1 - closestValue : closestValue;
	            sendRemoteControlChange(val);
	        }
	    } else {
	        for (RemoteOverlay element : elements) {
	            element.onMouseMove(data, matrix, target, maxx, maxy, x, y);
	        }
	    }
	}
	
	public boolean onMouseScroll(double scroll, RemoteControlData data, int maxx, int maxy, int x, int y) {
        if (!MinecraftClient.isReady()) {
            return true;
        }
        int ticks = MinecraftClient.getPlayer().getTickCount();

        RemoteOverlay target = find(data, new Matrix4(), maxx, maxy, x, y);
        if (target == null && lastScrolled + 20 > ticks) {
            target = scrollTarget;
        }

        if (target != null && !target.toggle) {
            float value = target.getValue(data);            
            value += scroll / -50 * ConfigGraphics.ScrollSpeed;

            target.sendRemoteControlChange(value);

            scrollTarget = target;
            lastScrolled = ticks;

            return false;
        }
        return true;
    }

    public void onMouseRelease(RemoteControlData data) {
        float value = getValue(data);

        if (toggle) {
            value = 1 - value;
            if (invert) {
                value = 1 - value;
            }
        }

        sendRemoteControlChange(value);
    }

    public boolean click(ClientEvents.MouseGuiEvent event, RemoteControlData data) {
        switch (event.action) {
            case CLICK:
                if (target != null) {
                    target.onMouseRelease(data);
                }
                target = find(data, new Matrix4(), GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), event.x, event.y);
                return target == null;
            case RELEASE:
                if (target != null) {
                    target.onMouseRelease(data);
                    target = null;
                    return false;
                }
                break;
            case MOVE:
                if (target != null) {
                    this.onMouseMove(data, new Matrix4(), target, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), event.x, event.y);
                    return false;
                }
                break;
            case SCROLL:
                return this.onMouseScroll(event.scroll, data, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), event.x, event.y);
        }
        return true;
    }
	
	private void sendRemoteControlChange(float value) {
	    UUID activeLoco = WirelessRemotecontrolClient.getLoco();
	    if (activeLoco == null || readout == null) {
	        return;
	    }
	    new RemoteControlClientPacket(activeLoco, readout, value).sendToServer();
	    WirelessRemotecontrolClient.applyLocalReadoutUpdate(readout, value);
	}

}
