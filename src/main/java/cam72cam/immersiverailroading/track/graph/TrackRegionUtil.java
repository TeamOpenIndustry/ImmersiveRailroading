package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.ModCore;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        try (FileInputStream is = new FileInputStream(file)){
            try (FileChannel channel = is.getChannel()) {
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4FastDecompressor decompressor = factory.fastDecompressor();

                // Memmap the file (per javadoc, hooks into GC cleanup)
                MappedByteBuffer raw = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

                // Read number of input bytes
                int decompressedBytes = raw.getInt();//todo 中途崩溃文件损坏？
                // Buffer to hold the decompressed data
                ByteBuffer decompressed = ByteBuffer.allocate(decompressedBytes);

                // Perform the decompression and move the head to the beginning of the buffer
                decompressor.decompress(raw, decompressed);
                decompressed.position(0);

                return decompressed;
            }
        }
    }

    static int sizeItemStack(ItemStack stack) {
        if (stack == null) {
            return Integer.BYTES;
        }

        // TODO this is wasteful
        try {
            return Integer.BYTES + stack.toTag().toBytes().length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void writeItemStack(ItemStack stack, ByteBuffer buffer) {
        if (stack == null) {
            buffer.putInt(0);
            ImmersiveRailroading.error("empty ItemStack!");
            return;
        }
        try {
            byte[] bytes = stack.toTag().toBytes();
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static ItemStack readItemStack(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.getInt()];
        if (bytes.length == 0) {
            return null;
        }
        buffer.get(bytes);
        try {
            return new ItemStack(new TagCompound(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
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