package cam72cam.mod.gui;

import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

import java.util.Iterator;

public class Progress {
    public static class Bar {
        private final ProgressBar bar;

        public Bar(ProgressBar bar) {
            this.bar = bar;
        }

        public void step(String name) {
            bar.step(name);
        }
    }

    public static Bar push(String name, int steps) {
        return new Bar(ProgressManager.push(name, steps));
    }

    public static void pop(Bar bar) {
        ProgressManager.pop(bar.bar);
    }

    public static void pop() {
        ProgressBar origBar = null;
        Iterator<ProgressBar> itr = ProgressManager.barIterator();
        while (itr.hasNext()) {
            origBar = itr.next();
        }

        //This is terrible, I am sorry
        ProgressManager.pop(origBar);
    }
}
