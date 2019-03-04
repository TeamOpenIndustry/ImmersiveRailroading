package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import igwmod.api.WikiRegistry;
import igwmod.gui.GuiWiki;
import igwmod.gui.tabs.BaseWikiTab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class IGWMod {
	public static void init() {
		WikiRegistry.registerWikiTab(new BaseWikiTab() {
			{
				pageEntries.add("home");
				skipLine();
				addSectionHeader("Guides");
				pageEntries.add("getting-started");
				pageEntries.add("open-computers");
				skipLine();
				addSectionHeader("Tools");
				pageEntries.add("blueprint-book");
				pageEntries.add("track-blueprint");
				pageEntries.add("large-wrench");
				pageEntries.add("coupling-hook");
				pageEntries.add("conductor-whistle");
				skipLine();
				addSectionHeader("Machines");
				pageEntries.add("track-roller");
				pageEntries.add("casting-basin");
				pageEntries.add("steam-hammer");
				pageEntries.add("plate-rolling-machine");
				pageEntries.add("boiler-roller");
				skipLine();
				addSectionHeader("Augments");
				pageEntries.add("augment-detector");
				pageEntries.add("augment-control");
				pageEntries.add("augment-item");
				pageEntries.add("augment-fluid");
				pageEntries.add("augment-speed-retarder");
				skipLine();
				addSectionHeader("Info");
				pageEntries.add("rolling-stock-performance");
				pageEntries.add("resource-packs-outside-sources");

			}

			@Override
			public String getName() {
				return ImmersiveRailroading.MODID;
			}

			@Override
			public ItemStack renderTabIcon(GuiWiki gui) {
				NonNullList<ItemStack> items = NonNullList.create();
				items.add(new ItemStack(IRItems.ITEM_TRACK_BLUEPRINT));
				return items.get(0);
			}

			@Override
			protected String getPageName(String pageEntry) {
				if (pageEntry.toCharArray()[0] == '#') {
					pageEntry = pageEntry.replace("#", "");
				}
				String newstr = "";
				boolean cap = true;
				for (char c : pageEntry.toCharArray()) {
					if (c == '-') {
						cap = true;
						newstr += " ";
						continue;
					}
					if (cap) {
						c = Character.toUpperCase(c);
						cap = false;
					}
					newstr += c;
				}
				
				return newstr;
			}

			@Override
			protected String getPageLocation(String pageEntry) {
				return ImmersiveRailroading.MODID + ":" + pageEntry;
			}
			
		});
	}
}
