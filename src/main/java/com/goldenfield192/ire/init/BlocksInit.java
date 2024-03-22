package com.goldenfield192.ire.init;

import com.goldenfield192.ire.IRE;
import com.goldenfield192.ire.blocks.BatteryBlock;
import com.goldenfield192.ire.blocks.ConnectorBlock;

public class BlocksInit {

    public static final ConnectorBlock CONNECTOR_BLOCK = new ConnectorBlock(IRE.MODID,"connector_block");
    public static final BatteryBlock BATTERY_BLOCK = new BatteryBlock(IRE.MODID,"battery_block");

    public static void register(){
    }
}
