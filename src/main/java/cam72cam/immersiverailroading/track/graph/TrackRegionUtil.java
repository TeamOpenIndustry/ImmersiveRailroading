package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.ModCore;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class TrackRegionUtil {
    // Copied from UMC
    static void writeBuffer(File file, ByteBuffer buffer) throws IOException {
        buffer.position(0);
        try (FileOutputStream os = new FileOutputStream(file)) {
            try (FileChannel channel = os.getChannel()) {
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4Compressor compressor = factory.highCompressor(2);

                // Could be faster
                byte[] input = buffer.array();
                byte[] output = compressor.compress(input);

                // Write number of input bytes
                ByteBuffer prefix = ByteBuffer.allocate(Integer.BYTES);
                prefix.asIntBuffer().put(input.length);
                channel.write(prefix);

                // Write the compressed data
                channel.write(ByteBuffer.wrap(output));
            } catch (NullPointerException e) {
                ModCore.error("Hit an exception while compressing cache data!  If you are using Java OpenJ9, please use a different JVM as there are known memory corruption bugs.");
                throw e;
            }
        }
    }
    static ByteBuffer readBuffer(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4FastDecompressor decompressor = factory.fastDecompressor();

            // 1) 读 decompressed 长度
            byte[] header = new byte[Integer.BYTES];
            readFully(is, header);
            int decompressedBytes = ByteBuffer.wrap(header).getInt();

            // 2) 读全部压缩数据
            int compressedLen = (int) file.length() - Integer.BYTES;
            if (compressedLen < 0) {
                throw new IOException("Truncated region file: " + file);
            }
            byte[] compressed = new byte[compressedLen];
            readFully(is, compressed);

            // 3) 解压
            ByteBuffer decompressed = ByteBuffer.allocate(decompressedBytes);
            decompressor.decompress(ByteBuffer.wrap(compressed), decompressed);
            decompressed.position(0);
            return decompressed;
        }
    }

    private static void readFully(InputStream is, byte[] dst) throws IOException {
        int off = 0;
        while (off < dst.length) {
            int r = is.read(dst, off, dst.length - off);
            if (r < 0) {
                throw new IOException("Unexpected EOF");
            }
            off += r;
        }
    }

    static int sizeString(String str) {
        // TODO This is wasteful
        return Integer.BYTES + str.getBytes(StandardCharsets.UTF_8).length;
    }

    static void writeString(String str, ByteBuffer buffer) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    static String readString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // region is 512 * worldHeight * 512, only x/z are limited
    public static Vec3i toRegionBlockPos(Vec3i blockPos) {
        return new Vec3i(blockPos.x & 0x1FF, blockPos.y, blockPos.z & 0x1FF);
    }

    public static Vec3i toBlockPos(long regionPos, Vec3i regionBlockPos) {
        int regionX = (int) (regionPos >> 32);
        int regionZ = (int) regionPos;
        return new Vec3i(
                (regionX << 9) + regionBlockPos.x,
                regionBlockPos.y,
                (regionZ << 9) + regionBlockPos.z
        );
    }

    public static long vecToRegion(Vec3i pos) {
        int factor = 9; // 512 blocks per region (2^9 = 512)
        long x = pos.x >> factor;
        long z = pos.z >> factor;
        return (x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int regionX(long region) {
        return (int) (region >> 32);
    }

    public static int regionZ(long region) {
        return (int) region;
    }
}