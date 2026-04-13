package ai.solar.kirill.main.menu;

import java.util.*;
import java.util.stream.Collectors;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.database.ViolationDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

public class MenuManager implements Listener {
    private final SolarAI plugin;
    private final Map<UUID, Integer> viewerPages = new HashMap<>();
    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();
    private static final int HEADS_PER_PAGE = 15;
    private static final int[] HEAD_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28, 29, 30, 31, 32};

    public MenuManager(SolarAI plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String getMenuTitle() {
        return translateHex(plugin.getLocaleManager().getMessage("gui.title"));
    }

    public void openMenu(Player player, int page) {
        viewerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, getMenuTitle());
        fillBorder(inv);
        fillHeads(inv, page);
        fillNavigationButtons(inv, page);

        player.openInventory(inv);
        if (updateTasks.containsKey(player.getUniqueId())) {
            updateTasks.get(player.getUniqueId()).cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().equals(inv)) {
                updateMenu(player, inv, page);
            } else {
                stopUpdating(player);
            }
        }, 40L, 40L);

        updateTasks.put(player.getUniqueId(), task);
    }

    private void updateMenu(Player player, Inventory inv, int page) {
        fillHeads(inv, page);
        player.updateInventory();
    }

    private void stopUpdating(Player player) {
        if (updateTasks.containsKey(player.getUniqueId())) {
            updateTasks.get(player.getUniqueId()).cancel();
            updateTasks.remove(player.getUniqueId());
        }
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = createGlass(Material.BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
            inv.setItem(i + 45, glass);
        }

        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, glass);
            inv.setItem(i * 9 + 8, glass);
        }
    }

    private void fillHeads(Inventory inv, int page) {
        Map<UUID, ViolationDatabase.PlayerViolationData> topViolators = 
            plugin.getViolationDatabase().getTopViolators(1000);
        List<Map.Entry<UUID, ViolationDatabase.PlayerViolationData>> sortedViolators = 
            topViolators.entrySet().stream()
                .filter(entry -> Bukkit.getPlayer(entry.getKey()) != null && Bukkit.getPlayer(entry.getKey()).isOnline())
                .sorted((e1, e2) -> Double.compare(e2.getValue().getAverageProbability(), e1.getValue().getAverageProbability()))
                .collect(Collectors.toList());

        int startIndex = page * HEADS_PER_PAGE;
        int endIndex = Math.min(startIndex + HEADS_PER_PAGE, sortedViolators.size());

        for (int i = 0; i < HEAD_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < endIndex) {
                Map.Entry<UUID, ViolationDatabase.PlayerViolationData> entry = sortedViolators.get(dataIndex);
                ItemStack head = createHead(entry.getKey(), entry.getValue());
                inv.setItem(HEAD_SLOTS[i], head);
            } else {
                inv.setItem(HEAD_SLOTS[i], null);
            }
        }
    }

    private void fillNavigationButtons(Inventory inv, int page) {
        Map<UUID, ViolationDatabase.PlayerViolationData> topViolators = 
            plugin.getViolationDatabase().getTopViolators(1000);

        long onlineCount = topViolators.keySet().stream()
            .filter(uuid -> Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline())
            .count();
        
        int totalPages = (int) Math.ceil((double) onlineCount / HEADS_PER_PAGE);

        if (page > 0) {
            ItemStack prevButton = createButton(
                Material.ARROW,
                translateHex(plugin.getLocaleManager().getMessage("gui.previous-page")),
                ""
            );
            inv.setItem(48, prevButton);
        }

        String pageInfo = translateHex(plugin.getLocaleManager().getMessage("gui.page-info")
            .replace("%page%", String.valueOf(page + 1))
            .replace("%total%", String.valueOf(Math.max(1, totalPages))));
        ItemStack infoButton = createButton(Material.PAPER, pageInfo, "");
        inv.setItem(49, infoButton);

        if (page < totalPages - 1) {
            ItemStack nextButton = createButton(
                Material.ARROW,
                translateHex(plugin.getLocaleManager().getMessage("gui.next-page")),
                ""
            );
            inv.setItem(50, nextButton);
        }
    }

    private boolean isHeadSlot(int slot) {
        for (int headSlot : HEAD_SLOTS) {
            if (headSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createGlass(Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private ItemStack createButton(Material material, String name, String lore) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(Collections.singletonList(lore));
            }
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createHead(UUID playerUUID, ViolationDatabase.PlayerViolationData data) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            Player targetPlayer = Bukkit.getPlayer(playerUUID);
            if (targetPlayer != null) {
                meta.setOwningPlayer(targetPlayer);
            }

            String playerName = data.getPlayerName();
            String headName = translateHex(plugin.getLocaleManager().getMessage("gui.head.name")
                .replace("%player%", playerName));
            meta.setDisplayName(headName);

            List<String> lore = new ArrayList<>();

            lore.add(translateHex(plugin.getLocaleManager().getMessage("gui.head.last-checks")));
            List<ViolationDatabase.ViolationRecord> recentViolations = 
                plugin.getViolationDatabase().getRecentViolations(playerUUID, 15);

            if (recentViolations.isEmpty()) {
                lore.add(translateHex(plugin.getLocaleManager().getMessage("gui.head.time-none")));
            } else {

                StringBuilder line = new StringBuilder();
                for (int i = 0; i < recentViolations.size(); i++) {
                    ViolationDatabase.ViolationRecord record = recentViolations.get(i);
                    double prob = record.getProbability() * 100;

                    String coloredProb = getColoredProbability(prob);
                    line.append(coloredProb);

                    if ((i + 1) % 5 == 0 || i == recentViolations.size() - 1) {
                        lore.add(line.toString());
                        line = new StringBuilder();
                    } else {
                        line.append(" ");
                    }
                }
            }

            lore.add("");

            double avgRisk = data.getAverageProbability() * 100;
            String avgPrefix = translateHex(plugin.getLocaleManager().getMessage("gui.head.avg-prefix"));
            String avgRiskLine = translateHex(plugin.getLocaleManager().getMessage("gui.head.average-risk")) + 
                " " + avgPrefix + getColoredProbability(avgRisk);
            lore.add(avgRiskLine);

            long lastTime = data.getLastViolationTime();
            String lastHitLine = translateHex(plugin.getLocaleManager().getMessage("gui.head.last-hit")) + " ";
            if (lastTime == 0) {
                lastHitLine += translateHex(plugin.getLocaleManager().getMessage("gui.head.time-none"));
            } else {
                long diff = System.currentTimeMillis() - lastTime;
                long seconds = diff / 1000;
                long minutes = seconds / 60;

                if (minutes > 0) {
                    lastHitLine += translateHex(plugin.getLocaleManager().getMessage("gui.head.time-min")
                        .replace("%time%", String.valueOf(minutes)));
                } else {
                    lastHitLine += translateHex(plugin.getLocaleManager().getMessage("gui.head.time-sec")
                        .replace("%time%", String.valueOf(seconds)));
                }
            }
            lore.add(lastHitLine);

            lore.add("");
            lore.add(translateHex(plugin.getLocaleManager().getMessage("gui.head.spectate")));

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    private String getColoredProbability(double probability) {
        String color;
        if (probability < 30) {
            color = "&#00FF00";
        } else if (probability < 60) {
            color = "&#FFD700";
        } else if (probability < 80) {
            color = "&#FF8C00";
        } else {
            color = "&#FF0000";
        }

        return translateHex(color + String.format("%.1f%%", probability));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(getMenuTitle())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();
        int currentPage = viewerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 48) {
            if (currentPage > 0) {
                openMenu(player, currentPage - 1);
            }
            return;
        }

        if (slot == 50) {
            Map<UUID, ViolationDatabase.PlayerViolationData> topViolators = 
                plugin.getViolationDatabase().getTopViolators(1000);

            long onlineCount = topViolators.keySet().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline())
                .count();
            
            int totalPages = (int) Math.ceil((double) onlineCount / HEADS_PER_PAGE);

            if (currentPage < totalPages - 1) {
                openMenu(player, currentPage + 1);
            }
            return;
        }

        if (isHeadSlot(slot) && clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());

                if (target != null && target.isOnline()) {
                    player.closeInventory();
                    stopUpdating(player);

                    player.teleport(target.getLocation());
                    player.sendMessage(translateHex(plugin.getLocaleManager().getMessage("gui.teleport")
                        .replace("%player%", target.getName())));

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spec go " + target.getName() + " " + player.getName());
                } else {
                    player.sendMessage(translateHex(plugin.getLocaleManager().getMessage("gui.offline")));
                }
            }
        }
    }

    private String translateHex(String message) {
        if (message == null) return "";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = pattern.matcher(message);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public void closeAllMenus() {
        for (UUID uuid : new HashSet<>(updateTasks.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }
            stopUpdating(player);
        }
    }
}
