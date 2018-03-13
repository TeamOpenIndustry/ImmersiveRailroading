package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemTabs;
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
				skipLine();
				addSectionHeader("Tools");
				pageEntries.add("blueprint-book");
				pageEntries.add("track-blueprint");
				skipLine();
				addSectionHeader("Machines");
				pageEntries.add("track-roller");
				pageEntries.add("casting-basin");
				pageEntries.add("steam-hammer");
				pageEntries.add("plate-rolling-machine");
				pageEntries.add("boiler-roller");
				skipLine();
				addSectionHeader("Parts");
				skipLine();
				addSectionHeader("Locomotives");
				skipLine();
				addSectionHeader("Rolling Stock");

			}

			@Override
			public String getName() {
				return ImmersiveRailroading.MODID;
			}

			@Override
			public ItemStack renderTabIcon(GuiWiki gui) {
				NonNullList<ItemStack> items = NonNullList.create();
				ImmersiveRailroading.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.LOCOMOTIVE_TAB, items);
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
