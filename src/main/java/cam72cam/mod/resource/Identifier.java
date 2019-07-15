package cam72cam.mod.resource;

import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Identifier {
    public final ResourceLocation internal;

    public Identifier(ResourceLocation internal) {
        this.internal = internal;
    }

    public Identifier(String ident) {
        this(new ResourceLocation(ident));
    }

    public Identifier(String domain, String path) {
        this(new ResourceLocation(domain, path));
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    public String getDomain() {
        return internal.getResourceDomain();
    }

    public String getPath() {
        return internal.getResourcePath();
    }

    public Identifier getRelative(String path) {
        return new Identifier(getDomain(), FilenameUtils.concat(FilenameUtils.getPath(getPath()), path).replace('\\', '/'));
    }



    public List<InputStream> getResourceStreamAll() throws IOException {
        return Data.proxy.getResourceStreamAll(this);
    }

    public InputStream getResourceStream() throws IOException {
        return Data.proxy.getResourceStream(this);
    }
}
