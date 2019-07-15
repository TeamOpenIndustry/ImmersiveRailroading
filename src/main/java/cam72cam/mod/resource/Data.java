package cam72cam.mod.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class Data {
    @SidedProxy(clientSide="cam72cam.mod.resource.Data$ClientProxy", serverSide="cam72cam.mod.resource.Data$ServerProxy")
    public static DataProxy proxy;

    public static abstract class DataProxy {
        private String configDir;

        public abstract List<InputStream> getResourceStreamAll(Identifier identifier) throws IOException;

        public InputStream getResourceStream(Identifier location) throws IOException {
            InputStream chosen = null;
            for (InputStream strm : getResourceStreamAll(location)) {
                if (chosen == null) {
                    chosen = strm;
                } else {
                    strm.close();
                }
            }
            if (chosen == null) {
                throw new java.io.FileNotFoundException(location.toString());
            }
            return chosen;
        }

        String pathString(Identifier location, boolean startingSlash) {
            return (startingSlash ? "/" : "") + "assets/" + location.getDomain() + "/" + location.getPath();
        }

        List<InputStream> getFileResourceStreams(Identifier location) throws IOException {
            List<InputStream> streams = new ArrayList<>();

            if (configDir == null) {
                configDir = Loader.instance().getConfigDir().toString();
                new File(configDir).mkdirs();
            }

            File folder = new File(this.configDir + File.separator + location.getDomain());
            if (folder.exists()) {
                if (folder.isDirectory()) {
                    File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
                    for (File file : files) {
                        ZipFile resourcePack = new ZipFile(file);
                        ZipEntry entry = resourcePack.getEntry(pathString(location, false));
                        if (entry != null) {
                            // Copy the input stream so we can close the resource pack
                            InputStream stream = resourcePack.getInputStream(entry);
                            streams.add(new ByteArrayInputStream(IOUtils.toByteArray(stream)));
                        }
                        resourcePack.close();
                    }
                }
            } else {
                folder.mkdirs();
            }
            return streams;
        }

    }

    public static class ClientProxy extends DataProxy {
        @Override
        public List<InputStream> getResourceStreamAll(Identifier identifier) throws IOException {
            List<InputStream> res = new ArrayList<>();
            try {
                for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(identifier.internal)) {
                    res.add(resource.getInputStream());
                }
            } catch (java.io.FileNotFoundException ex) {
                // Ignore
            }
            res.addAll(getFileResourceStreams(identifier));
            return res;
        }
    }

    public static class ServerProxy extends DataProxy {
        private InputStream getEmbeddedResourceStream(Identifier location) throws IOException {
            URL url = this.getClass().getResource(pathString(location, true));
            return url != null ? this.getClass().getResourceAsStream(pathString(location, true)) : null;
        }

        @Override
        public List<InputStream> getResourceStreamAll(Identifier location) throws IOException {
            List<InputStream> res = new ArrayList<>();
            InputStream stream = getEmbeddedResourceStream(location);
            if (stream != null) {
                res.add(stream);
            }

            res.addAll(getFileResourceStreams(location));

            return res;
        }
    }
}
