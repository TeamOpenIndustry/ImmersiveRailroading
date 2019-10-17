package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.items.ItemGoldenSpike;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.TagCompound;

public class ItemGradeControl
{
    public static final float GRADE_CHANGE_DELTA = 0.25f;
    public static final String GRADE_NAME = "grade";

    public static void incrementGrade(Player player, ItemStack stack) {
        //I don't know if this null protection is necessary
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new TagCompound());
        }

        TagCompound mainTag = stack.getTagCompound();
        float newGrade = ItemGoldenSpike.gradeChangeDelta;

        if(mainTag.hasKey(GRADE_NAME)) {
            newGrade = mainTag.getFloat(GRADE_NAME) + GRADE_CHANGE_DELTA;
        }

        mainTag.setFloat(GRADE_NAME, newGrade);
        displayGrade(player, newGrade);
    }

    public static void decrementGrade(Player player, ItemStack stack) {
        //I don't know if this null protection is necessary
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new TagCompound());
        }

        TagCompound mainTag = stack.getTagCompound();
        float newGrade = -ItemGoldenSpike.gradeChangeDelta;

        if(mainTag.hasKey(GRADE_NAME)) {
            newGrade = mainTag.getFloat(GRADE_NAME) - GRADE_CHANGE_DELTA;
        }

        mainTag.setFloat(GRADE_NAME, newGrade);
        displayGrade(player, newGrade);
    }

    public static float getGrade(ItemStack stack) {
        //I don't know if this null protection is necessary
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new TagCompound());
        }

        TagCompound mainTag = stack.getTagCompound();
        if(mainTag.hasKey(GRADE_NAME)) {
            return mainTag.getFloat(GRADE_NAME);
        }
        else {
            return 0;
        }
    }

    public static void displayGrade(Player player, float grade) {
        player.sendMessage(PlayerMessage.direct("Grade now: " + grade));
    }
}
