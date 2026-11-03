package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.EntityPool;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VehiclePersistence {
    private static final Pattern FILE_PATTERN = Pattern.compile("^vehicles_(\\d+)_(\\d+)\\.dat$");

    private static final ExecutorService SAVE_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "IR-Vehicle-Save");
        // 注意：不能再是 Daemon 线程了，否则关窗时会直接被杀掉
        t.setDaemon(false);
        return t;
    });

    // 【核心修复 1】：注册 JVM 关闭钩子，保证强退时数据能写完
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SAVE_EXECUTOR.shutdown();
            try {
                // 最多等 10 秒，防止死锁卡死游戏
                if (!SAVE_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                    SAVE_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                SAVE_EXECUTOR.shutdownNow();
            }
        }, "IR-Vehicle-Save-ShutdownHook"));
    }

    private static File dirFor(File levelDirectory, World world) {
        return new File(levelDirectory, "immersiverailroading" + world.getId() + "/vehicles");
    }

    private static File fileFor(File levelDirectory, World world, int totalBuckets, int bucketId) {
        return new File(dirFor(levelDirectory, world), "vehicles_" + totalBuckets + "_" + bucketId + ".dat");
    }

    private static int bucketFor(UUID uuid, int bucketCount) {
        return Math.floorMod(uuid.hashCode(), bucketCount);
    }

    public static void save(World world, File levelDirectory) {
        if (!world.isServer) return;
        if (!Config.ConfigDebug.enableEntityPool) return;

        EntityPool pool = EntityPool.get(world);
        File dir = dirFor(levelDirectory, world);

        int bucketCount = Math.max(1, Config.ConfigDebug.vehicleBucketCount);
        Map<Integer, List<ModdedEntity>> buckets = new HashMap<>();

        for (ModdedEntity e : pool.getAllEntities()) {
            int b = bucketFor(e.getPersistentID(), bucketCount);
            buckets.computeIfAbsent(b, k -> new ArrayList<>()).add(e);
        }

        SAVE_EXECUTOR.submit(() -> {
            try {
                if (!dir.exists() && !dir.mkdirs()) {
                    ModCore.warn("Unable to create vehicle data directory %s", dir);
                    return;
                }

                Set<String> currentValidFiles = new HashSet<>();
                int totalSaved = 0;

                for (Map.Entry<Integer, List<ModdedEntity>> entry : buckets.entrySet()) {
                    int bucketId = entry.getKey();
                    List<ModdedEntity> entities = entry.getValue();

                    TagCompound root = new TagCompound();
                    Map<UUID, TagCompound> map = new HashMap<>();
                    for (ModdedEntity e : entities) {
                        map.put(e.getPersistentID(), e.serializeToTagCompound());
                    }
                    root.setMap("vehicles", map, UUID::toString, tc -> tc);

                    File file = fileFor(levelDirectory, world, bucketCount, bucketId);
                    Path targetPath = file.toPath();
                    Path tmpPath = targetPath.resolveSibling(file.getName() + ".tmp");

                    // 【核心修复 2】：原子写入。先写临时文件
                    Files.write(tmpPath, root.toBytes());

                    // 瞬间移动替换，保证就算在写入途中崩溃，原文件也不会损坏
                    try {
                        Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    currentValidFiles.add(file.getName());
                    totalSaved += map.size();
                }

                // 清理旧的分卷
                File[] existingFiles = dir.listFiles();
                if (existingFiles != null) {
                    for (File f : existingFiles) {
                        if (FILE_PATTERN.matcher(f.getName()).matches() && !currentValidFiles.contains(f.getName())) {
                            if (f.delete()) {
                                ImmersiveRailroading.info("[POOL] Deleted outdated bucket file %s", f.getName());
                            }
                        }
                    }
                }

                ImmersiveRailroading.info("[POOL] async saved %d vehicles into %d buckets", totalSaved, buckets.size());
            } catch (Exception e) {
                ModCore.catching(e, "Failed to async save vehicles for world %d", world.getId());
            }
        });
    }

    public static void load(World world, File levelDirectory) {
        if (!world.isServer) return;

        File dir = dirFor(levelDirectory, world);
        if (!dir.exists()) return;

        EntityPool pool = EntityPool.get(world);
        int restored = 0, skipped = 0;
        int loadedFiles = 0;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            Matcher matcher = FILE_PATTERN.matcher(file.getName());
            if (!matcher.matches()) continue;

            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                TagCompound root = new TagCompound(bytes);
                if (!root.hasKey("vehicles")) continue;

                Map<UUID, TagCompound> vehicles = root.getMap("vehicles", UUID::fromString, tc -> tc);
                for (Map.Entry<UUID, TagCompound> entry : vehicles.entrySet()) {
                    if (pool.getEntity(entry.getKey()) != null) {
                        skipped++;
                        continue;
                    }
                    try {
                        ModdedEntity e = pool.createFromTagCompound(entry.getValue());
                        restored++;

                        // we will get duplicated wrong entity cant be interacted?
//                        if (!Config.ConfigDebug.enableEntityPool) {
//                            pool.spawn(e);
//                        }
                        System.out.println(restored);
                    } catch (Exception e) {
                        ModCore.catching(e, "Failed to restore vehicle %s", entry.getKey());
                    }
                }
                loadedFiles++;

                // 【备份处理】：只有池子关闭时，才将文件重命名，防止离线数据丢失
                // 生成时间戳：20231027_153022
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                if (!Config.ConfigDebug.enableEntityPool) {
                    File backupFile = new File(file.getParentFile(), file.getName() + "." + timestamp + ".backup");
                    if (file.renameTo(backupFile)) {
                        ImmersiveRailroading.info("[POOL] backed up %s -> %s", file.getName(), backupFile.getName());
                    } else {
                        ModCore.warn("[POOL] Failed to backup %s", file.getName());
                    }
                }
            } catch (Exception e) {
                // 【核心修复 3】：损坏隔离。防止坏档导致游戏彻底无法启动
                ModCore.error("Vehicle file %s is corrupted! Renaming to .corrupted", file.getName());
                ModCore.catching(e);
                File badFile = new File(file.getParentFile(), file.getName() + ".corrupted");
                if (badFile.exists()) badFile.delete();
                file.renameTo(badFile);
            }
        }

        ImmersiveRailroading.info("[POOL] restored %d vehicles (%d skipped) from %d files", restored, skipped, loadedFiles);
    }
}