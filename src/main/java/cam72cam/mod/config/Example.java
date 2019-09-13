package cam72cam.mod.config;

import java.io.IOException;

import static cam72cam.mod.config.ConfigFile.*;

@Comment("Magic!")
public class Example {
    private int x;
    public int y;
    public static int z = 10;
    @Name("Myname")
    @Comment("Super duper...")
    public static int q;
    public static Integer bar;

    public static String foobar;

    @Name("inner")
    @Comment("Some other data")
    public static class Example2 {
        public static int foo;
    }


    public static void main(String[] args) throws IOException {
        sync(Example.class);
    }
}
