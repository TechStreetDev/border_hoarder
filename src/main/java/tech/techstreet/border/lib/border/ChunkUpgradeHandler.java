/*
 * Copyright (C) 2026 TechStreetDev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techstreet.border.lib.border;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.gui.item.Colours;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Regenerates chunks in regions defined ahead of time in {@link #DEFINITIONS}, so
 * newly added biomes/terrain features apply to already-generated chunks. Each
 * definition has its own ID and is only ever applied once (tracked in a small
 * completion file) - marked complete only once every one of its regions has
 * actually been regenerated. Because regeneration destroys anything already built
 * in the target chunks, every run backs up the affected region files first and
 * skips (rather than proceeding) if a player is currently standing inside
 * the requested area (they may be inside the border) - the definition is left
 * incomplete so it is retried on next startup.
 */
public class ChunkUpgradeHandler {
    /**
     * Add an entry here for every chunk region that needs regenerating, e.g. after a
     * Minecraft version update introduces new biomes. Each ID must be unique and,
     * once applied, is never re-applied automatically - see {@code world/data/border/completed-upgrades.dat}.
     */
    private static final List<UpgradeDefinition> DEFINITIONS = List.of(
            new UpgradeDefinition("chaos-cubed", List.of(
                    new RegionDefinition("world", 24, -7, 33, -16)
            ), regions -> {
                int count = 3;
                int seeded = 0;

                for (ChunkRegion region : regions) {
                    World world = region.world();

                    for (int x = region.minChunkX(); x <= region.maxChunkX() && seeded < count; x++) {
                        for (int z = region.minChunkZ(); z <= region.maxChunkZ() && seeded < count; z++) {
                            boolean wasLoaded = world.isChunkLoaded(x, z);
                            Chunk chunk = world.getChunkAt(x, z);

                            for (Entity entity : chunk.getEntities()) {
                                if (seeded >= count) break;
                                if (entity instanceof StorageMinecart minecart && minecart.getLocation().getBlock().getBiome().equals(Biome.SULFUR_CAVES)) {
                                    minecart.getInventory().addItem(new ItemStack(Material.MUSIC_DISC_BOUNCE));
                                    seeded++;
                                }
                            }

                            if (!wasLoaded) {
                                world.unloadChunk(x, z, true);
                            }
                        }
                    }
                }
            })
    );

    private static final File completedFile = new File("world/data/border/completed-upgrades.dat");

    private final BorderHoarderPlugin instance;
    private volatile boolean running = false;
    private volatile int upgraded = 0;
    private volatile int total = 0;

    public ChunkUpgradeHandler(BorderHoarderPlugin instance) {
        this.instance = instance;
    }

    /**
     * A rectangular region of chunks, in chunk coordinates (inclusive).
     */
    public record ChunkRegion(World world, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        public ChunkRegion {
            if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
                throw new IllegalArgumentException("Region minimum coordinates must not exceed maximum coordinates.");
            }
        }

        public int chunkCount() {
            return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        }
    }

    /**
     * A region to regenerate, in chunk coordinates, tied to a world by name.
     */
    public record RegionDefinition(String world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
    }

    /**
     * A single upgrade definition: a unique ID, the regions it regenerates, and an
     * optional script to run once immediately after every region has successfully
     * regenerated (e.g. to seed newly added loot into freshly generated structures).
     * The script receives the resolved regions and is free to load chunks and
     * inspect/modify their entities and blocks directly via the Bukkit API.
     */
    public record UpgradeDefinition(String id, List<RegionDefinition> regions,
                                    @Nullable Consumer<List<ChunkRegion>> postUpgrade) {

    }

    /**
     * An upgrade definition with its region definitions already resolved to actual
     * worlds, so its total chunk count can be known upfront.
     */
    private record ResolvedUpgrade(UpgradeDefinition definition, List<ChunkRegion> regions) {
    }

    /**
     * Builds a chunk region from two chunk coordinate corners.
     */
    private ChunkRegion regionFromChunkCoords(World world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
        int minChunkX = Math.min(chunkX1, chunkX2);
        int maxChunkX = Math.max(chunkX1, chunkX2);
        int minChunkZ = Math.min(chunkZ1, chunkZ2);
        int maxChunkZ = Math.max(chunkZ1, chunkZ2);

        return new ChunkRegion(world, minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }

    /**
     * Finds the online players currently standing inside the given chunk region.
     *
     * @param region the region to check.
     * @return the list of players inside the region.
     */
    public List<Player> getPlayersInRegion(ChunkRegion region) {
        List<Player> players = new ArrayList<>();
        for (Player player : region.world().getPlayers()) {
            int chunkX = player.getLocation().getBlockX() >> 4;
            int chunkZ = player.getLocation().getBlockZ() >> 4;

            if (chunkX >= region.minChunkX() && chunkX <= region.maxChunkX()
                    && chunkZ >= region.minChunkZ() && chunkZ <= region.maxChunkZ()) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Applies every upgrade definition in {@link #DEFINITIONS} that has not yet
     * been marked complete. {@link #getUpgraded()}/{@link #getTotal()} report
     * cumulative progress across every region of every pending definition, so
     * progress keeps climbing steadily instead of resetting partway through.
     */
    public void checkForPendingUpgrades() {
        if (running) {
            log("A chunk upgrade is already in progress.");
            return;
        }

        List<UpgradeDefinition> definitions = loadDefinitions();
        Set<String> completed = readCompletedIds();

        List<ResolvedUpgrade> pending = new ArrayList<>();
        int totalChunks = 0;

        for (UpgradeDefinition definition : definitions) {
            if (completed.contains(definition.id())) continue;

            List<ChunkRegion> regions = resolveRegions(definition);
            pending.add(new ResolvedUpgrade(definition, regions));

            for (ChunkRegion region : regions) totalChunks += region.chunkCount();
        }

        if (pending.isEmpty()) return;

        running = true;
        upgraded = 0;
        total = totalChunks;

        log("Found " + pending.size() + " pending chunk upgrade(s) to apply (" + totalChunks + " chunk(s) total).");
        applyPending(pending, 0);
    }

    private List<ChunkRegion> resolveRegions(UpgradeDefinition definition) {
        List<ChunkRegion> regions = new ArrayList<>();

        for (RegionDefinition rd : definition.regions()) {
            World world = Bukkit.getWorld(rd.world());
            if (world == null) {
                log("Skipping a region in unknown world '" + rd.world() + "' for upgrade '" + definition.id() + "'.");
                continue;
            }

            regions.add(regionFromChunkCoords(world, rd.chunkX1(), rd.chunkZ1(), rd.chunkX2(), rd.chunkZ2()));
        }

        return regions;
    }

    private void applyPending(List<ResolvedUpgrade> pending, int index) {
        if (index >= pending.size()) {
            running = false;
            log("Finished applying pending chunk upgrades.");
            return;
        }

        ResolvedUpgrade next = pending.get(index);
        applyDefinition(next.definition(), next.regions(), () -> applyPending(pending, index + 1));
    }

    private void applyDefinition(UpgradeDefinition definition, List<ChunkRegion> regions, Runnable onComplete) {
        AtomicBoolean anySkipped = new AtomicBoolean(false);
        processRegionsSequentially(regions, 0, definition.id(), anySkipped, () -> {
            if (anySkipped.get()) {
                log("Upgrade '" + definition.id() + "' was not fully applied, it will be retried next startup.");
            } else {
                if (definition.postUpgrade() != null) {
                    try {
                        definition.postUpgrade().accept(regions);
                    } catch (Exception e) {
                        log("Post-upgrade script for '" + definition.id() + "' failed: " + e.getMessage());
                    }
                }

                markCompleted(definition.id());
                log("Upgrade '" + definition.id() + "' complete.");
            }

            onComplete.run();
        });
    }

    private void processRegionsSequentially(List<ChunkRegion> regions, int index, String id, AtomicBoolean anySkipped, Runnable onComplete) {
        if (index >= regions.size()) {
            onComplete.run();
            return;
        }

        ChunkRegion region = regions.get(index);
        List<Player> playersInside = getPlayersInRegion(region);

        if (!playersInside.isEmpty()) {
            String names = playersInside.stream().map(Player::getName).collect(Collectors.joining(", "));
            log("Skipping a region in '" + region.world().getName() + "' for upgrade '" + id + "' because players are inside it: " + names + ".");
            anySkipped.set(true);
            processRegionsSequentially(regions, index + 1, id, anySkipped, onComplete);
            return;
        }

        upgradeRegion(region, () -> processRegionsSequentially(regions, index + 1, id, anySkipped, onComplete));
    }

    /**
     * The upgrade definitions to check, as declared in {@link #DEFINITIONS}.
     */
    private List<UpgradeDefinition> loadDefinitions() {
        return DEFINITIONS;
    }

    private Set<String> readCompletedIds() {
        try {
            if (!completedFile.exists()) return new HashSet<>();
            return new HashSet<>(Files.readAllLines(completedFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void markCompleted(String id) {
        try {
            if (!completedFile.getParentFile().exists()) completedFile.getParentFile().mkdirs();
            Set<String> completed = readCompletedIds();
            completed.add(id);
            Files.write(completedFile.toPath(), completed);
        } catch (IOException ignored) {
        }
    }

    private void upgradeRegion(ChunkRegion region, Runnable onComplete) {
        log("Backing up affected region files before upgrading " + region.chunkCount() + " chunk(s)...");

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            try {
                backupRegion(region);
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(instance, () -> {
                    log("Failed to back up region files, aborting upgrade: " + e.getMessage());
                    onComplete.run();
                });
                return;
            }

            Bukkit.getScheduler().runTask(instance, () -> {
                log("Backup complete, upgrading " + region.chunkCount() + " chunk(s) in '" + region.world().getName() + "'...");
                processBatches(region, onComplete);
            });
        });
    }

    private void processBatches(ChunkRegion region, Runnable onComplete) {
        List<int[]> chunks = new ArrayList<>();
        for (int x = region.minChunkX(); x <= region.maxChunkX(); x++) {
            for (int z = region.minChunkZ(); z <= region.maxChunkZ(); z++) {
                chunks.add(new int[]{x, z});
            }
        }

        // Every target chunk's header entry must be cleared BEFORE the server's first
        // getChunkAt() touches that chunk's region file. Paper caches each region
        // file's header table in memory the moment it opens it (region-file-cache-size,
        // default 256) and never re-reads it from disk - so editing the header on disk
        // AFTER the server has already opened that file (e.g. while working through
        // other chunks in the same 32x32 region) is invisible to it, and it will keep
        // returning the old, un-regenerated chunk. Clearing every entry for a region
        // file up front, before any of its chunks are loaded, avoids that entirely.
        try {
            clearChunkEntries(region.world(), chunks);
        } catch (IOException e) {
            log("Failed to clear chunk data ahead of regeneration: " + e.getMessage());
        }

        int perTick = Math.max(1, instance.getConfig().getInt("chunk-upgrade.chunks-per-tick", 10));
        Iterator<int[]> iterator = chunks.iterator();
        World world = region.world();

        Bukkit.getScheduler().runTaskTimer(instance, task -> {
            int processedThisTick = 0;
            while (processedThisTick < perTick && iterator.hasNext()) {
                int[] chunk = iterator.next();
                try {
                    forceRegenerateChunk(world, chunk[0], chunk[1]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                upgraded++;
                processedThisTick++;
            }

            if (!iterator.hasNext()) {
                task.cancel();
                log("Finished upgrading this region (" + upgraded + "/" + total + " chunk(s) overall) in '" + world.getName() + "'.");
                onComplete.run();
            }
        }, 1L, 1L);
    }

    /**
     * Regenerates a single chunk that has already had its region file entry cleared
     * by {@link #clearChunkEntries(World, List)}. {@code World#regenerateChunk} is no
     * longer supported by the server since noise-based world generation replaced the
     * old regeneration path, so instead the chunk is unloaded and reloaded, which -
     * since the server now sees it as never generated - triggers a full fresh
     * generation pass with the current world generator.
     */
    private void forceRegenerateChunk(World world, int chunkX, int chunkZ) {
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            world.unloadChunk(chunkX, chunkZ, false);
        }

        world.getChunkAt(chunkX, chunkZ);
        world.unloadChunk(chunkX, chunkZ, true);
    }

    /**
     * Zeroes out the location and timestamp table entries for every given chunk, so
     * the server no longer sees them as generated. Entries are grouped by region file
     * and each file is opened once, so every chunk in a region file is cleared before
     * any of them can possibly be loaded (see the caching caveat above).
     */
    private void clearChunkEntries(World world, List<int[]> chunks) throws IOException {
        File regionDir = getRegionDirectory(world);
        if (!regionDir.isDirectory()) return;

        Map<Long, List<int[]>> byRegionFile = new HashMap<>();
        for (int[] chunk : chunks) {
            int regionX = Math.floorDiv(chunk[0], 32);
            int regionZ = Math.floorDiv(chunk[1], 32);
            long key = (((long) regionX) << 32) | (regionZ & 0xFFFFFFFFL);
            byRegionFile.computeIfAbsent(key, k -> new ArrayList<>()).add(chunk);
        }

        for (Map.Entry<Long, List<int[]>> entry : byRegionFile.entrySet()) {
            int regionX = (int) (entry.getKey() >> 32);
            int regionZ = (int) (long) entry.getKey();
            File regionFile = new File(regionDir, "r." + regionX + "." + regionZ + ".mca");
            if (!regionFile.exists()) continue;

            try (RandomAccessFile raf = new RandomAccessFile(regionFile, "rw")) {
                if (raf.length() < 8192) continue;

                byte[] zero = new byte[4];
                for (int[] chunk : entry.getValue()) {
                    int localX = Math.floorMod(chunk[0], 32);
                    int localZ = Math.floorMod(chunk[1], 32);
                    int index = localX + localZ * 32;

                    raf.seek((long) index * 4);
                    raf.write(zero);
                    raf.seek(4096L + (long) index * 4);
                    raf.write(zero);
                }
            }
        }
    }

    /**
     * Zips the region (.mca) files that overlap the given chunk region into the
     * backups folder, so the original terrain can be restored if needed.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void backupRegion(ChunkRegion region) throws IOException {
        File regionDir = getRegionDirectory(region.world());
        if (!regionDir.isDirectory()) return;

        List<File> affectedFiles = getFiles(region, regionDir);
        if (affectedFiles.isEmpty()) return;

        File backupsDir = new File(".backups");
        if (!backupsDir.exists()) backupsDir.mkdirs();

        String zipName = "chunk-upgrade-" + region.world().getName() + "-" + Instant.now().getEpochSecond() + ".zip";
        File zipFile = new File(backupsDir, zipName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File file : affectedFiles) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

    private static @NonNull List<File> getFiles(ChunkRegion region, File regionDir) {
        int minRegionX = Math.floorDiv(region.minChunkX(), 32);
        int maxRegionX = Math.floorDiv(region.maxChunkX(), 32);
        int minRegionZ = Math.floorDiv(region.minChunkZ(), 32);
        int maxRegionZ = Math.floorDiv(region.maxChunkZ(), 32);

        List<File> affectedFiles = new ArrayList<>();
        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                File regionFile = new File(regionDir, "r." + rx + "." + rz + ".mca");
                if (regionFile.exists()) affectedFiles.add(regionFile);
            }
        }
        return affectedFiles;
    }

    /**
     * Resolves the region folder for a given world.
     */
    private File getRegionDirectory(World world) {
        // Paper 26.1+ stores every dimension under world/dimensions/minecraft/<dimension>/,
        // each with its own region/ folder, and World#getWorldFolder() already resolves to
        // that per-dimension path - there is no more world_nether/DIM-1 style layout to guess.
        return new File(world.getWorldFolder(), "region");
    }

    private void log(String message) {
        Component component = Component.text("[" + BorderHoarderPlugin.getPluginName() + "] " + message, Colours.GREEN);
        Bukkit.getConsoleSender().sendMessage(component);
    }

    /**
     * Whether an upgrade pass is currently in progress.
     *
     * @return true if an upgrade is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * The number of chunks upgraded so far in the current (or most recent) pass.
     *
     * @return the number of chunks upgraded.
     */
    public int getUpgraded() {
        return upgraded;
    }

    /**
     * The total number of chunks queued for the current (or most recent) pass.
     *
     * @return the total number of chunks queued.
     */
    public int getTotal() {
        return total;
    }
}
