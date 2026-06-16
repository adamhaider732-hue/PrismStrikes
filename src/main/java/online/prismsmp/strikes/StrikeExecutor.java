package online.prismsmp.strikes;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StrikeExecutor {

    private final PrismStrikes plugin;
    private final Set<String> activeStrikes = new HashSet<>();

    public StrikeExecutor(PrismStrikes plugin) {
        this.plugin = plugin;
    }

    public void cancelAll() {
        activeStrikes.clear();
    }

    public void execute(Player player, Location target, int tier) {
        String strikeId = player.getName() + "_" + System.currentTimeMillis();
        activeStrikes.add(strikeId);

        String tierPath = "tiers." + tier + ".";
        int radius = plugin.getConfig().getInt(tierPath + "radius", 20);
        int fuseTicks = plugin.getConfig().getInt(tierPath + "fuse-ticks", 80);
        int spawnHeight = plugin.getConfig().getInt(tierPath + "spawn-height", 40);
        double spacing = plugin.getConfig().getDouble(tierPath + "tnt-spacing", 3.0);

        World world = target.getWorld();
        if (world == null) return;

        double spawnY = target.getY() + spawnHeight;

        // Local message within 2x radius
        int msgRange = radius * 2;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(target) <= (double) msgRange * msgRange) {
                if (!p.equals(player)) {
                    p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.RED + "PRISM" + ChatColor.DARK_GRAY + "] "
                            + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " has launched an Orbital Strike!");
                }
            }
        }

        // Phase 1: Beam (3 seconds)
        int beamTicks = 60;
        startBeam(world, target, spawnY, beamTicks, strikeId);

        // Phase 2: TNT bombardment after beam
        int bombardDelay = beamTicks + 5;

        // Load chunks
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeStrikes.contains(strikeId)) return;
                int chunkRadius = (radius / 16) + 2;
                for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
                    for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                        world.getChunkAt(target.getBlockX() / 16 + cx,
                                target.getBlockZ() / 16 + cz).load();
                    }
                }
                world.playSound(target, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10.0f, 0.5f);
            }
        }.runTaskLater(plugin, bombardDelay - 5);

        // Generate dense circular pattern
        List<double[]> positions = generateCircle(target, spawnY, radius, spacing);

        // Spawn in performance-safe waves
        int maxPerTick = plugin.getConfig().getInt("performance.max-tnt-per-tick", 100);
        int waveDelay = plugin.getConfig().getInt("performance.wave-delay-ticks", 1);

        List<List<double[]>> waves = new ArrayList<>();
        for (int i = 0; i < positions.size(); i += maxPerTick) {
            waves.add(positions.subList(i, Math.min(i + maxPerTick, positions.size())));
        }

        for (int wave = 0; wave < waves.size(); wave++) {
            List<double[]> batch = waves.get(wave);
            int waveTick = bombardDelay + (wave * waveDelay);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!activeStrikes.contains(strikeId)) return;
                    for (double[] pos : batch) {
                        try {
                            Location loc = new Location(world, pos[0], pos[1], pos[2]);
                            TNTPrimed tnt = world.spawn(loc, TNTPrimed.class);
                            tnt.setFuseTicks(fuseTicks);
                            tnt.setVelocity(new org.bukkit.util.Vector(
                                    (Math.random() - 0.5) * 0.05,
                                    -0.6,
                                    (Math.random() - 0.5) * 0.05));
                        } catch (Exception e) { /* skip */ }
                    }
                }
            }.runTaskLater(plugin, waveTick);
        }

        // Cleanup
        int totalDuration = bombardDelay + (waves.size() * waveDelay) + fuseTicks + 60;
        new BukkitRunnable() {
            @Override
            public void run() { activeStrikes.remove(strikeId); }
        }.runTaskLater(plugin, totalDuration);
    }

    // ================================================================
    // Dense concentric rings filling the entire circle
    // ================================================================
    private List<double[]> generateCircle(Location target, double spawnY, int radius, double spacing) {
        List<double[]> positions = new ArrayList<>();

        int ringCount = (int) (radius / spacing);

        for (int ring = 0; ring <= ringCount; ring++) {
            double ringRadius = ring * spacing;

            if (ring == 0) {
                // Center cluster — a few TNT
                for (int i = 0; i < 3; i++) {
                    double x = target.getX() + (Math.random() - 0.5) * 2;
                    double z = target.getZ() + (Math.random() - 0.5) * 2;
                    double y = spawnY + (Math.random() * 8);
                    positions.add(new double[]{x, y, z});
                }
                continue;
            }

            // TNT count scales with circumference
            int count = (int) (2 * Math.PI * ringRadius / spacing);
            count = Math.max(count, 6);

            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;

                // Small scatter ±1 block so it doesn't look artificially perfect
                double scatterR = (Math.random() - 0.5) * 2.0;
                double scatterA = (Math.random() - 0.5) * 0.3;

                double r = ringRadius + scatterR;
                double a = angle + scatterA;
                double x = target.getX() + Math.cos(a) * r;
                double z = target.getZ() + Math.sin(a) * r;
                double y = spawnY + (Math.random() * 10);

                positions.add(new double[]{x, y, z});
            }
        }

        return positions;
    }

    // ================================================================
    // Beam Effect
    // ================================================================
    private void startBeam(World world, Location target, double topY, int durationTicks, String strikeId) {
        double targetY = target.getY();
        double totalDist = topY - targetY;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!activeStrikes.contains(strikeId) || tick >= durationTicks) {
                    cancel();
                    return;
                }

                double progress = (double) tick / durationTicks;
                double beamTipY = topY - (totalDist * progress);

                for (int i = 0; i < 20; i++) {
                    double y = beamTipY + i;
                    if (y > topY) break;
                    world.spawnParticle(Particle.END_ROD,
                            target.getX(), y, target.getZ(),
                            1, 0.05, 0, 0.05, 0);
                }

                if (tick % 5 == 0) {
                    spawnGroundRing(world, target, 3.0);
                }

                if (tick % 15 == 0) {
                    float pitch = 0.5f + (1.5f * (float) progress);
                    world.playSound(target, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.HOSTILE, 5.0f, pitch);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnGroundRing(World world, Location center, double radius) {
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            world.spawnParticle(Particle.DUST,
                    new Location(world, x, center.getY() + 0.5, z),
                    1, new Particle.DustOptions(Color.RED, 1.5f));
        }
    }
}
