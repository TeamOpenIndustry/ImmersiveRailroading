package cam72cam.immersiverailroading.util;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {

    /**
     * Split a list into N number of parts.
     * Returned number of list parts may be less than nParts, depending of the amount
     * of items in the given list.
     *
     * @param list List to split.
     * @param nParts Number of parts to split the list into.
     * @param <T> Type of the item in the list.
     * @return List of list parts.
     */
    public static <T> List<List<T>> splitListIntoNParts(List<T> list, int nParts) {
        ArrayList<List<T>> resultLists = new ArrayList<>();
        int size = list.size();
        if (size == 0) {
            return resultLists;
        }

        int chunkSize = MathHelper.ceil((float) size / (float) nParts);
        int fromIndex = 0;
        int toIndex = chunkSize;
        while (fromIndex < size) {
            resultLists.add(list.subList(fromIndex, Math.min(toIndex, size)));

            fromIndex = toIndex;
            toIndex += chunkSize;
        }

        return resultLists;
    }

}
