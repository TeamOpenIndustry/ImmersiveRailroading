package cam72cam.mod.gui;

public interface IScreen {
    void init(IScreenBuilder screen);

    void onEnterKey();

    void onClose();

    void draw(IScreenBuilder builder);
}
