package com.psiromouna.smp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

public final class PsiromounaSMP extends JavaPlugin implements Listener, TabCompleter {
    private Connection db;
    private BukkitTask displayTask;
    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private final Map<UUID, Long> lastDisplay = new HashMap<>();
    private final Map<UUID, Long> pvpTaggedUntil = new HashMap<>();
    private final Set<UUID> afkPlayers = new HashSet<>();
    private static final long AFK_COOLDOWN_MILLIS = 30L * 60L * 1000L;
    private static final long PVP_TAG_MILLIS = 15_000L;
    private static final String PREFIX = ChatColor.DARK_AQUA + "[SMP] " + ChatColor.RESET;
    private static final Map<Material, Double> DEFAULT_PRICES = new LinkedHashMap<>();
    private static final List<String> COMMON_BLOCKS = List.of(
        "OAK_LOG",
        "SPRUCE_LOG",
        "BIRCH_LOG",
        "JUNGLE_LOG",
        "ACACIA_LOG",
        "DARK_OAK_LOG",
        "MANGROVE_LOG",
        "CHERRY_LOG",
        "BAMBOO_BLOCK",
        "CRIMSON_STEM",
        "WARPED_STEM",
        "OAK_PLANKS",
        "SPRUCE_PLANKS",
        "BIRCH_PLANKS",
        "JUNGLE_PLANKS",
        "ACACIA_PLANKS",
        "DARK_OAK_PLANKS",
        "MANGROVE_PLANKS",
        "CHERRY_PLANKS",
        "BAMBOO_PLANKS",
        "CRIMSON_PLANKS",
        "WARPED_PLANKS",
        "OAK_STAIRS",
        "SPRUCE_STAIRS",
        "BIRCH_STAIRS",
        "JUNGLE_STAIRS",
        "ACACIA_STAIRS",
        "DARK_OAK_STAIRS",
        "MANGROVE_STAIRS",
        "CHERRY_STAIRS",
        "BAMBOO_STAIRS",
        "CRIMSON_STAIRS",
        "WARPED_STAIRS",
        "OAK_SLAB",
        "SPRUCE_SLAB",
        "BIRCH_SLAB",
        "JUNGLE_SLAB",
        "ACACIA_SLAB",
        "DARK_OAK_SLAB",
        "MANGROVE_SLAB",
        "CHERRY_SLAB",
        "BAMBOO_SLAB",
        "CRIMSON_SLAB",
        "WARPED_SLAB",
        "STONE",
        "COBBLESTONE",
        "MOSSY_COBBLESTONE",
        "STONE_BRICKS",
        "MOSSY_STONE_BRICKS",
        "CRACKED_STONE_BRICKS",
        "ANDESITE",
        "POLISHED_ANDESITE",
        "DIORITE",
        "POLISHED_DIORITE",
        "GRANITE",
        "POLISHED_GRANITE",
        "DEEPSLATE",
        "COBBLED_DEEPSLATE",
        "POLISHED_DEEPSLATE",
        "DEEPSLATE_BRICKS",
        "TUFF",
        "TUFF_BRICKS",
        "CALCITE",
        "DRIPSTONE_BLOCK",
        "DIRT",
        "COARSE_DIRT",
        "GRASS_BLOCK",
        "MUD",
        "PACKED_MUD",
        "MUD_BRICKS",
        "SAND",
        "RED_SAND",
        "GRAVEL",
        "CLAY",
        "BRICKS",
        "SANDSTONE",
        "RED_SANDSTONE",
        "WHITE_WOOL",
        "ORANGE_WOOL",
        "MAGENTA_WOOL",
        "LIGHT_BLUE_WOOL",
        "YELLOW_WOOL",
        "LIME_WOOL",
        "PINK_WOOL",
        "GRAY_WOOL",
        "LIGHT_GRAY_WOOL",
        "CYAN_WOOL",
        "PURPLE_WOOL",
        "BLUE_WOOL",
        "BROWN_WOOL",
        "GREEN_WOOL",
        "RED_WOOL",
        "BLACK_WOOL",
        "WHITE_CONCRETE",
        "ORANGE_CONCRETE",
        "MAGENTA_CONCRETE",
        "LIGHT_BLUE_CONCRETE",
        "YELLOW_CONCRETE",
        "LIME_CONCRETE",
        "PINK_CONCRETE",
        "GRAY_CONCRETE",
        "LIGHT_GRAY_CONCRETE",
        "CYAN_CONCRETE",
        "PURPLE_CONCRETE",
        "BLUE_CONCRETE",
        "BROWN_CONCRETE",
        "GREEN_CONCRETE",
        "RED_CONCRETE",
        "BLACK_CONCRETE",
        "WHITE_TERRACOTTA",
        "ORANGE_TERRACOTTA",
        "MAGENTA_TERRACOTTA",
        "LIGHT_BLUE_TERRACOTTA",
        "YELLOW_TERRACOTTA",
        "LIME_TERRACOTTA",
        "PINK_TERRACOTTA",
        "GRAY_TERRACOTTA",
        "LIGHT_GRAY_TERRACOTTA",
        "CYAN_TERRACOTTA",
        "PURPLE_TERRACOTTA",
        "BLUE_TERRACOTTA",
        "BROWN_TERRACOTTA",
        "GREEN_TERRACOTTA",
        "RED_TERRACOTTA",
        "BLACK_TERRACOTTA",
        "GLASS",
        "GLASS_PANE",
        "TINTED_GLASS",
        "OBSIDIAN",
        "CRYING_OBSIDIAN",
        "NETHERRACK",
        "NETHER_BRICKS",
        "BLACKSTONE",
        "POLISHED_BLACKSTONE",
        "QUARTZ_BLOCK",
        "PRISMARINE",
        "DARK_PRISMARINE",
        "SEA_LANTERN",
        "GLOWSTONE",
        "END_STONE",
        "END_STONE_BRICKS",
        "BOOKSHELF",
        "MOSS_BLOCK",
        "HAY_BLOCK",
        "BONE_BLOCK",
        "ICE",
        "PACKED_ICE",
        "SNOW_BLOCK",
        "PUMPKIN",
        "MELON"
    );
    private static final List<org.bukkit.entity.EntityType> SPAWNER_TYPES = List.of(
        org.bukkit.entity.EntityType.ZOMBIE, org.bukkit.entity.EntityType.SKELETON, org.bukkit.entity.EntityType.CREEPER,
        org.bukkit.entity.EntityType.SPIDER, org.bukkit.entity.EntityType.CAVE_SPIDER, org.bukkit.entity.EntityType.ENDERMAN,
        org.bukkit.entity.EntityType.BLAZE, org.bukkit.entity.EntityType.SLIME, org.bukkit.entity.EntityType.MAGMA_CUBE,
        org.bukkit.entity.EntityType.WITCH, org.bukkit.entity.EntityType.PILLAGER, org.bukkit.entity.EntityType.DROWNED,
        org.bukkit.entity.EntityType.HUSK, org.bukkit.entity.EntityType.STRAY, org.bukkit.entity.EntityType.PIG,
        org.bukkit.entity.EntityType.COW, org.bukkit.entity.EntityType.SHEEP, org.bukkit.entity.EntityType.CHICKEN,
        org.bukkit.entity.EntityType.RABBIT, org.bukkit.entity.EntityType.VILLAGER, org.bukkit.entity.EntityType.IRON_GOLEM,
        org.bukkit.entity.EntityType.GUARDIAN, org.bukkit.entity.EntityType.PHANTOM, org.bukkit.entity.EntityType.SHULKER
    );

    static {
        DEFAULT_PRICES.put(Material.DIAMOND, 500.0);
        DEFAULT_PRICES.put(Material.EMERALD, 250.0);
        DEFAULT_PRICES.put(Material.GOLD_INGOT, 80.0);
        DEFAULT_PRICES.put(Material.IRON_INGOT, 35.0);
        DEFAULT_PRICES.put(Material.COPPER_INGOT, 12.0);
        DEFAULT_PRICES.put(Material.COAL, 8.0);
        DEFAULT_PRICES.put(Material.REDSTONE, 10.0);
        DEFAULT_PRICES.put(Material.LAPIS_LAZULI, 15.0);
        DEFAULT_PRICES.put(Material.AMETHYST_SHARD, 25.0);
        DEFAULT_PRICES.put(Material.BLAZE_ROD, 45.0);
        DEFAULT_PRICES.put(Material.ENDER_PEARL, 30.0);
        DEFAULT_PRICES.put(Material.STRING, 8.0);
        DEFAULT_PRICES.put(Material.BONE, 6.0);
        DEFAULT_PRICES.put(Material.GUNPOWDER, 18.0);
        DEFAULT_PRICES.put(Material.ROTTEN_FLESH, 3.0);
        DEFAULT_PRICES.put(Material.SPIDER_EYE, 8.0);
        DEFAULT_PRICES.put(Material.LEATHER, 12.0);
        DEFAULT_PRICES.put(Material.WHEAT, 5.0);
        DEFAULT_PRICES.put(Material.CARROT, 4.0);
        DEFAULT_PRICES.put(Material.POTATO, 4.0);
        DEFAULT_PRICES.put(Material.SUGAR_CANE, 5.0);
        DEFAULT_PRICES.put(Material.PUMPKIN, 12.0);
        DEFAULT_PRICES.put(Material.APPLE, 5.0);
        DEFAULT_PRICES.put(Material.BREAD, 10.0);
        DEFAULT_PRICES.put(Material.COOKED_BEEF, 18.0);
        DEFAULT_PRICES.put(Material.COBBLESTONE, 1.0);
        DEFAULT_PRICES.put(Material.OAK_LOG, 4.0);
        DEFAULT_PRICES.put(Material.OBSIDIAN, 20.0);
        DEFAULT_PRICES.put(Material.QUARTZ, 12.0);
        DEFAULT_PRICES.put(Material.SHULKER_SHELL, 150.0);
        DEFAULT_PRICES.put(Material.ELYTRA, 2500.0);
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        openDatabase();
        loadLoadedBannerTitles();
        Bukkit.getScheduler().runTaskTimer(this, this::updateBannerTitleVisibility, 20L, 10L);
        getServer().getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new EconomyExpansion(this).register();
        for (String name : List.of("worth", "sell", "shop", "buy", "ah", "balance", "currency", "cmds", "banner", "afk")) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) cmd.setTabCompleter(this);
        }
        displayTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) updateDisplays(p);
        }, 20L, 40L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                changeBalance(p.getUniqueId(), "shards", 3);
                p.sendMessage(ChatColor.AQUA + "+3 Shards " + ChatColor.GRAY + "(5 minutes online)");
            }
        }, 6000L, 6000L);
        getLogger().info("Psiromouna SMP economy enabled.");
    }

    @Override public void onDisable() {
        if (displayTask != null) displayTask.cancel();
        if (db != null) try { db.close(); } catch (SQLException ignored) {}
    }

    private void openDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            db = DriverManager.getConnection("jdbc:sqlite:" + new File(getDataFolder(), "economy.db").getAbsolutePath());
            try (Statement s = db.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS balances (uuid TEXT PRIMARY KEY, lefta REAL NOT NULL DEFAULT 0, shards REAL NOT NULL DEFAULT 0)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS auctions (id INTEGER PRIMARY KEY AUTOINCREMENT, seller TEXT NOT NULL, seller_name TEXT NOT NULL, price REAL NOT NULL, item TEXT NOT NULL)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_banners (owner TEXT PRIMARY KEY, world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, title TEXT NOT NULL, display_uuid TEXT)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS afk_cooldowns (uuid TEXT PRIMARY KEY, until_ms INTEGER NOT NULL)");
            }
        } catch (Exception e) {
            getLogger().severe("Could not open economy database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void ensureAccount(UUID id) {
        try (PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO balances(uuid,lefta,shards) VALUES(?,0,0)")) {
            ps.setString(1, id.toString()); ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning(e.getMessage()); }
    }
    public double placeholderBalance(UUID id, String currency) { return getBalance(id, currency); }
    private double getBalance(UUID id, String currency) {
        ensureAccount(id);
        try (PreparedStatement ps = db.prepareStatement("SELECT " + (currency.equals("shards") ? "shards" : "lefta") + " FROM balances WHERE uuid=?")) {
            ps.setString(1, id.toString()); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (SQLException e) { return 0; }
    }
    private void changeBalance(UUID id, String currency, double amount) {
        ensureAccount(id);
        String column = currency.equals("shards") ? "shards" : "lefta";
        try (PreparedStatement ps = db.prepareStatement("UPDATE balances SET " + column + " = " + column + " + ? WHERE uuid=?")) {
            ps.setDouble(1, amount); ps.setString(2, id.toString()); ps.executeUpdate();
        } catch (SQLException e) { getLogger().warning(e.getMessage()); }
    }
    private String money(double amount) { return String.format(Locale.US, "%,.2f", amount).replaceAll("\\.00$", ""); }
    private String nice(Material m) {
        String[] bits = m.name().toLowerCase(Locale.ROOT).split("_");
        return Arrays.stream(bits).map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1)).collect(Collectors.joining(" "));
    }
    private double worth(Material m) {
        if (m == null || m.isAir() || !m.isItem()) return 0;
        if (getConfig().contains("prices." + m.name())) {
            double configured = getConfig().getDouble("prices." + m.name());
            if (Double.isFinite(configured) && configured > 0) return configured;
        }
        if (DEFAULT_PRICES.containsKey(m)) return DEFAULT_PRICES.get(m);
        String n = m.name();
        if (n.contains("ELYTRA")) return 2500;
        if (n.contains("NETHERITE")) return 1200;
        if (n.contains("TOTEM")) return 1800;
        if (n.contains("SHULKER")) return 500;
        if (n.contains("DIAMOND")) return 500;
        if (n.contains("TRIDENT") || n.contains("HEART_OF_THE_SEA")) return 1600;
        if (n.contains("SPAWN_EGG") || n.contains("SPAWNER")) return 0;
        if (n.contains("ANCIENT_DEBRIS")) return 900;
        if (n.contains("KELP")) return 125;
        if (n.contains("PICKLE") || n.contains("PETAL")) return 125;
        if (n.contains("LOG") || n.contains("PLANKS")) return 4;
        if (n.contains("SEEDS") || n.contains("WHEAT") || n.contains("CARROT") || n.contains("POTATO")) return 2;
        if (n.contains("STONE") || n.contains("DIRT") || n.contains("COBBLE")) return 1;
        if (n.contains("COOKED_") || n.contains("BREAD")) return 8;
        if (n.contains("WOOL") || n.contains("GLASS")) return 3;
        if (n.contains("BONE") || n.contains("STRING") || n.contains("ROTTEN_FLESH")) return 2;
        if (n.contains("NETHERITE") || n.contains("ANCIENT_DEBRIS")) return 900;
        if (n.contains("TOTEM") || n.contains("ELYTRA") || n.contains("TRIDENT")) return 1800;
        if (n.contains("SHULKER") || n.contains("END_CRYSTAL")) return 300;
        if (n.contains("SPAWN_EGG") || n.contains("SPAWNER")) return 0;
        if (n.contains("DIAMOND") || n.contains("EMERALD")) return 250;
        if (n.contains("GOLD")) return 45;
        if (n.contains("IRON")) return 20;
        if (n.contains("COPPER")) return 9;
        if (n.contains("QUARTZ") || n.contains("PRISMARINE") || n.contains("SEA_LANTERN")) return 24;
        if (n.contains("OBSIDIAN") || n.contains("ANVIL")) return 35;
        if (n.contains("CONCRETE") || n.contains("TERRACOTTA") || n.contains("GLASS")) return 8;
        if (n.contains("WOOL") || n.contains("CARPET")) return 6;
        if (n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_PLANKS")) return 4;
        if (n.endsWith("_STAIRS") || n.endsWith("_SLAB") || n.endsWith("_WALL")) return 5;
        if (n.contains("STONE") || n.contains("DIRT") || n.contains("SAND") || n.contains("GRAVEL")) return 2;
        if (n.contains("COOKED_") || n.contains("GOLDEN_")) return 14;
        if (n.contains("ENCHANTED") || n.contains("NETHER_STAR")) return 1200;
        return 3;
    }

    private double shopPrice(Material material) {
        // Keep shop prices clean and predictable; do not add ordinal-based fractions.
        return Math.round(Math.max(1.0, worth(material) * 2.0) * 100.0) / 100.0;
    }
    private String shopMoney(double amount) {
        return String.format(Locale.US, "%,.2f", amount).replaceAll("\\.00$", "").replaceAll("(\\.[0-9])0$", "$1");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("currency")) return currencyCommand(sender, args);
        if (name.equals("banner")) { if (!(sender instanceof Player bp)) { sender.sendMessage("Players only."); return true; } return bannerCommand(bp, args); }
        if (name.equals("afk")) { if (!(sender instanceof Player ap)) { sender.sendMessage("Players only."); return true; } return afkCommand(ap); }
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }

        switch (name) {
            case "cmds" -> { openCommands(p); return true; }
            case "worth" -> {
                if (args.length == 0) { openWorth(p); return true; }
                if (args[0].equalsIgnoreCase("search")) {
                    if (args.length < 2) { p.sendMessage(PREFIX + "Usage: /worth search <item>"); return true; }
                    String q = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
                    List<Material> matches = Arrays.stream(Material.values()).filter(m -> !m.isAir() && worth(m) > 0 &&
                            (m.name().toLowerCase(Locale.ROOT).contains(q.replace(' ', '_')) || nice(m).toLowerCase(Locale.ROOT).contains(q))).limit(20).toList();
                    if (matches.isEmpty()) p.sendMessage(PREFIX + ChatColor.YELLOW + "No priced items found.");
                    else { p.sendMessage(ChatColor.GOLD + "Worth search: " + q); for (Material m : matches) p.sendMessage(ChatColor.YELLOW + nice(m) + ChatColor.GRAY + " — " + ChatColor.GOLD + "€" + money(worth(m)) + " each"); }
                    return true;
                }
                Material m = args[0].equalsIgnoreCase("hand") ? p.getInventory().getItemInMainHand().getType()
                        : Material.matchMaterial(String.join("_", args).toUpperCase(Locale.ROOT));
                if (m == null || m.isAir()) p.sendMessage(PREFIX + ChatColor.RED + "Unknown item. Try /worth search <text>.");
                else p.sendMessage(PREFIX + ChatColor.YELLOW + nice(m) + ChatColor.GRAY + " is worth " + ChatColor.GOLD + "€" + money(worth(m)) + " each.");
                return true;
            }
            case "sell" -> {
                if (args.length == 0) { openSell(p); return true; }
                if (args[0].equalsIgnoreCase("hand")) {
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (item.getType().isAir()) { p.sendMessage(PREFIX + "Hold an item first."); return true; }
                    sellStack(p, item, item.getAmount()); return true;
                }
                if (args[0].equalsIgnoreCase("all")) {
                    int sold = 0; double total = 0;
                    for (int i = 0; i < p.getInventory().getStorageContents().length; i++) {
                        ItemStack item = p.getInventory().getItem(i);
                        if (item == null || item.getType().isAir()) continue;
                        double price = worth(item.getType()); if (price <= 0) continue;
                        sold += item.getAmount(); total += price * item.getAmount(); p.getInventory().setItem(i, null);
                    }
                    if (sold == 0) p.sendMessage(PREFIX + "You have no items with a sell value.");
                    else { changeBalance(p.getUniqueId(), "lefta", total); p.sendMessage(PREFIX + ChatColor.GREEN + "Sold " + sold + " items for €" + money(total) + "."); }
                    return true;
                }
                p.sendMessage(PREFIX + "Usage: /sell [hand|all]"); return true;
            }
            case "shop", "buy" -> { openShop(p); return true; }
            case "balance" -> {
                p.sendMessage(PREFIX + ChatColor.GREEN + "Lefta: " + ChatColor.GOLD + money(getBalance(p.getUniqueId(), "lefta"))
                        + ChatColor.GRAY + " | Shards: " + ChatColor.AQUA + money(getBalance(p.getUniqueId(), "shards")));
                return true;
            }
            case "ah" -> { if (args.length == 0) { openAuction(p); return true; } return auctionCommand(p, args); }
        }
        return false;
    }

    private void loadLoadedBannerTitles() {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT owner FROM player_banners")) {
            List<UUID> owners = new ArrayList<>();
            while (rs.next()) owners.add(UUID.fromString(rs.getString(1)));
            for (UUID owner : owners) spawnPlayerBanner(owner);
        } catch (SQLException ex) { getLogger().warning("Could not restore banner titles: " + ex.getMessage()); }
    }

    private boolean bannerCommand(Player p, String[] args) {
        if (args.length == 0) {
            p.sendMessage(ChatColor.LIGHT_PURPLE + "Banner commands:");
            p.sendMessage(ChatColor.WHITE + "/banner <title> " + ChatColor.GRAY + "- register the banner you're looking at");
            p.sendMessage(ChatColor.WHITE + "/banner rename <title> " + ChatColor.GRAY + "- change your title");
            p.sendMessage(ChatColor.WHITE + "/banner move " + ChatColor.GRAY + "- move your title to another banner");
            p.sendMessage(ChatColor.WHITE + "/banner remove " + ChatColor.GRAY + "- remove your banner and title");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("remove")) {
            if (!removePlayerBanner(p.getUniqueId(), true)) { p.sendMessage(PREFIX + ChatColor.RED + "You don't have a registered banner."); return true; }
            p.sendMessage(PREFIX + ChatColor.GREEN + "Your banner and floating title were removed.");
            return true;
        }
        if (action.equals("rename")) {
            if (args.length < 2) { p.sendMessage(PREFIX + "Usage: /banner rename <title>"); return true; }
            String title = cleanBannerTitle(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            if (title.isBlank()) { p.sendMessage(PREFIX + ChatColor.RED + "Enter a title."); return true; }
            try (PreparedStatement ps = db.prepareStatement("UPDATE player_banners SET title=? WHERE owner=?")) {
                ps.setString(1, title); ps.setString(2, p.getUniqueId().toString());
                if (ps.executeUpdate() == 0) { p.sendMessage(PREFIX + ChatColor.RED + "Register a banner first with /banner <title>."); return true; }
                refreshPlayerBanner(p.getUniqueId());
                p.sendMessage(PREFIX + ChatColor.GREEN + "Banner title changed to " + ChatColor.LIGHT_PURPLE + title);
            } catch (SQLException ex) { p.sendMessage(PREFIX + ChatColor.RED + "Could not update your banner."); }
            return true;
        }
        boolean moving = action.equals("move");
        String title;
        if (moving) {
            title = getPlayerBannerTitle(p.getUniqueId());
            if (title == null) { p.sendMessage(PREFIX + ChatColor.RED + "You don't have a banner yet. Use /banner <title> first."); return true; }
        } else {
            title = cleanBannerTitle(String.join(" ", args));
            if (title.isBlank()) { p.sendMessage(PREFIX + ChatColor.RED + "Enter a title."); return true; }
        }
        org.bukkit.block.Block target = p.getTargetBlockExact(6);
        if (target == null || !(target.getState() instanceof org.bukkit.block.Banner)) {
            p.sendMessage(PREFIX + ChatColor.RED + "Look directly at a placed banner within 6 blocks.");
            return true;
        }
        if (!moving && hasPlayerBanner(p.getUniqueId())) {
            p.sendMessage(PREFIX + ChatColor.RED + "You already have a banner. Use /banner rename <title> or /banner move.");
            return true;
        }
        if (isBannerOwnedByOther(target, p.getUniqueId())) {
            p.sendMessage(PREFIX + ChatColor.RED + "That banner is already registered to another player.");
            return true;
        }
        if (moving) removePlayerBanner(p.getUniqueId(), false);
        try (PreparedStatement ps = db.prepareStatement("INSERT OR REPLACE INTO player_banners(owner,world,x,y,z,title,display_uuid) VALUES(?,?,?,?,?,?,NULL)")) {
            ps.setString(1, p.getUniqueId().toString()); ps.setString(2, target.getWorld().getName());
            ps.setInt(3, target.getX()); ps.setInt(4, target.getY()); ps.setInt(5, target.getZ()); ps.setString(6, title);
            ps.executeUpdate();
            spawnPlayerBanner(p.getUniqueId());
            p.sendMessage(PREFIX + ChatColor.GREEN + (moving ? "Banner moved." : "Banner registered!") + " Floating title: " + ChatColor.LIGHT_PURPLE + title);
        } catch (SQLException ex) { p.sendMessage(PREFIX + ChatColor.RED + "Could not save your banner."); }
        return true;
    }

    private String cleanBannerTitle(String raw) {
        String cleaned = ChatColor.stripColor(raw).replaceAll("[\\r\\n\\t]", " ").trim();
        return cleaned.length() > 48 ? cleaned.substring(0, 48) : cleaned;
    }
    private boolean hasPlayerBanner(UUID owner) { return getPlayerBannerTitle(owner) != null; }
    private String getPlayerBannerTitle(UUID owner) {
        try (PreparedStatement ps = db.prepareStatement("SELECT title FROM player_banners WHERE owner=?")) {
            ps.setString(1, owner.toString()); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        } catch (SQLException ex) { return null; }
    }
    private boolean isBannerOwnedByOther(org.bukkit.block.Block block, UUID owner) {
        try (PreparedStatement ps = db.prepareStatement("SELECT owner FROM player_banners WHERE world=? AND x=? AND y=? AND z=?")) {
            ps.setString(1, block.getWorld().getName()); ps.setInt(2, block.getX()); ps.setInt(3, block.getY()); ps.setInt(4, block.getZ());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && !rs.getString(1).equals(owner.toString()); }
        } catch (SQLException ex) { return true; }
    }
    private boolean removePlayerBanner(UUID owner, boolean removeBlock) {
        try (PreparedStatement ps = db.prepareStatement("SELECT world,x,y,z,display_uuid FROM player_banners WHERE owner=?")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                org.bukkit.World world = Bukkit.getWorld(rs.getString("world"));
                if (removeBlock && world != null) { org.bukkit.block.Block bannerBlock = world.getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")); if (bannerBlock.getState() instanceof org.bukkit.block.Banner) bannerBlock.setType(Material.AIR, false); }
                if (world != null && rs.getString("display_uuid") != null) {
                    try { org.bukkit.entity.Entity entity = world.getEntity(UUID.fromString(rs.getString("display_uuid"))); if (entity != null) entity.remove(); } catch (IllegalArgumentException ignored) {}
                }
            }
            try (PreparedStatement del = db.prepareStatement("DELETE FROM player_banners WHERE owner=?")) { del.setString(1, owner.toString()); del.executeUpdate(); }
            return true;
        } catch (SQLException ex) { return false; }
    }
    private void refreshPlayerBanner(UUID owner) {
        try (PreparedStatement ps = db.prepareStatement("SELECT world,x,y,z,title,display_uuid FROM player_banners WHERE owner=?")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                org.bukkit.World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return;
                UUID displayId = null;
                try { if (rs.getString("display_uuid") != null) displayId = UUID.fromString(rs.getString("display_uuid")); } catch (IllegalArgumentException ignored) {}
                org.bukkit.entity.Entity entity = displayId == null ? null : world.getEntity(displayId);
                if (entity instanceof TextDisplay display) display.setText(bannerTitleColor(rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) + ChatColor.BOLD.toString() + rs.getString("title"));
                else spawnPlayerBanner(owner);
            }
        } catch (SQLException ex) { getLogger().warning("Could not refresh banner: " + ex.getMessage()); }
    }
    private void updateBannerTitleVisibility() {
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT world,x,y,z,title,display_uuid FROM player_banners WHERE display_uuid IS NOT NULL")) {
            while (rs.next()) {
                org.bukkit.World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                org.bukkit.entity.Entity entity;
                try { entity = world.getEntity(UUID.fromString(rs.getString("display_uuid"))); }
                catch (IllegalArgumentException ex) { continue; }
                if (!(entity instanceof TextDisplay display)) continue;
                display.setText(bannerTitleColor(world.getName(), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"))
                        + ChatColor.BOLD.toString() + rs.getString("title"));
                org.bukkit.Location bannerLocation = new org.bukkit.Location(world,
                    rs.getInt("x") + 0.5, rs.getInt("y") + 0.5, rs.getInt("z") + 0.5);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (!viewer.getWorld().equals(world)) continue;
                    if (viewer.getLocation().distanceSquared(bannerLocation) <= 100.0) {
                        viewer.hideEntity(this, display);
                    } else {
                        viewer.showEntity(this, display);
                    }
                }
            }
        } catch (SQLException ex) {
            getLogger().warning("Could not update banner title visibility: " + ex.getMessage());
        }
    }

    private ChatColor bannerTitleColor(String worldName, int x, int y, int z) {
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return ChatColor.LIGHT_PURPLE;
        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        if (!(block.getState() instanceof org.bukkit.block.Banner banner)) return ChatColor.LIGHT_PURPLE;
        return switch (banner.getBaseColor()) {
            case WHITE -> ChatColor.WHITE;
            case ORANGE, BROWN -> ChatColor.GOLD;
            case MAGENTA, PINK -> ChatColor.LIGHT_PURPLE;
            case LIGHT_BLUE, CYAN -> ChatColor.AQUA;
            case YELLOW -> ChatColor.YELLOW;
            case LIME, GREEN -> ChatColor.GREEN;
            case GRAY, LIGHT_GRAY -> ChatColor.GRAY;
            case PURPLE -> ChatColor.DARK_PURPLE;
            case BLUE -> ChatColor.BLUE;
            case RED -> ChatColor.RED;
            case BLACK -> ChatColor.DARK_GRAY;
        };
    }

    private void spawnPlayerBanner(UUID owner) {
        try (PreparedStatement ps = db.prepareStatement("SELECT world,x,y,z,title,display_uuid FROM player_banners WHERE owner=?")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                org.bukkit.World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return;
                int x = rs.getInt("x"), y = rs.getInt("y"), z = rs.getInt("z");
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return;
                UUID oldId = null;
                try { if (rs.getString("display_uuid") != null) oldId = UUID.fromString(rs.getString("display_uuid")); } catch (IllegalArgumentException ignored) {}
                org.bukkit.entity.Entity old = oldId == null ? null : world.getEntity(oldId);
                if (old instanceof TextDisplay existing) {
                    existing.teleport(new org.bukkit.Location(world, x + 0.5, y + 20.0, z + 0.5));
                    existing.setVisibleByDefault(false);
                    existing.setViewRange(128.0f);
                    existing.setLineWidth(240);
                    existing.setText(bannerTitleColor(world.getName(), x, y, z) + ChatColor.BOLD.toString() + rs.getString("title"));
                    return;
                }
                String title = rs.getString("title");
                TextDisplay display = world.spawn(new org.bukkit.Location(world, x + 0.5, y + 20.0, z + 0.5), TextDisplay.class, entity -> {
                    entity.setText(bannerTitleColor(world.getName(), x, y, z) + ChatColor.BOLD.toString() + title);
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                    entity.setVisibleByDefault(false);
                    entity.setViewRange(128.0f);
                    entity.setLineWidth(240);
                    entity.setShadowed(true);
                    entity.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(0, 0, 0), new org.joml.AxisAngle4f(), new org.joml.Vector3f(2.5f, 2.5f, 2.5f), new org.joml.AxisAngle4f()));
                    entity.setSeeThrough(false);
                    entity.setDefaultBackground(false);
                    entity.setPersistent(true);
                    entity.setInvulnerable(true);
                });
                try (PreparedStatement update = db.prepareStatement("UPDATE player_banners SET display_uuid=? WHERE owner=?")) {
                    update.setString(1, display.getUniqueId().toString()); update.setString(2, owner.toString()); update.executeUpdate();
                }
            }
        } catch (Exception ex) { getLogger().warning("Could not spawn banner title: " + ex.getMessage()); }
    }
    @EventHandler public void onBannerChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        try (PreparedStatement ps = db.prepareStatement("SELECT owner FROM player_banners WHERE world=? AND (x >> 4)=? AND (z >> 4)=?")) {
            ps.setString(1, e.getWorld().getName()); ps.setInt(2, e.getChunk().getX()); ps.setInt(3, e.getChunk().getZ());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) spawnPlayerBanner(UUID.fromString(rs.getString(1))); }
        } catch (SQLException ex) { getLogger().warning("Could not load banner titles: " + ex.getMessage()); }
    }
    @EventHandler public void onBannerBreak(org.bukkit.event.block.BlockBreakEvent e) {
        org.bukkit.block.Block block = e.getBlock();
        try (PreparedStatement ps = db.prepareStatement("SELECT owner FROM player_banners WHERE world=? AND x=? AND y=? AND z=?")) {
            ps.setString(1, block.getWorld().getName()); ps.setInt(2, block.getX()); ps.setInt(3, block.getY()); ps.setInt(4, block.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                UUID owner = UUID.fromString(rs.getString("owner"));
                if (!owner.equals(e.getPlayer().getUniqueId())) { e.setCancelled(true); e.getPlayer().sendMessage(PREFIX + ChatColor.RED + "That is someone else's registered banner."); return; }
                removePlayerBanner(owner, false);
            }
        } catch (SQLException ex) { getLogger().warning("Could not clean up broken banner: " + ex.getMessage()); }
    }

    private void sellStack(Player p, ItemStack item, int amount) {
        double price = worth(item.getType());
        if (price <= 0) { p.sendMessage(PREFIX + ChatColor.RED + "That item has no sell price."); return; }
        double total = price * amount;
        item.setAmount(item.getAmount() - amount);
        changeBalance(p.getUniqueId(), "lefta", total);
        p.sendMessage(PREFIX + ChatColor.GREEN + "Sold " + amount + "x " + nice(item.getType()) + " for €" + money(total) + ".");
    }

    private boolean currencyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("psiromouna.admin")) { sender.sendMessage(PREFIX + ChatColor.RED + "No permission."); return true; }
        if (args.length != 4 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Usage: /currency give <player> <lefta|shards> <amount>"); return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("That player must be online."); return true; }
        String cur = args[2].toLowerCase(Locale.ROOT);
        if (!cur.equals("lefta") && !cur.equals("shards")) { sender.sendMessage("Currency must be lefta or shards."); return true; }
        try {
            double amount = Double.parseDouble(args[3]);
            if (!Double.isFinite(amount) || amount <= 0) throw new NumberFormatException();
            changeBalance(target.getUniqueId(), cur, amount);
            sender.sendMessage("Gave " + money(amount) + " " + cur + " to " + target.getName() + ".");
            target.sendMessage(PREFIX + ChatColor.GREEN + "You received " + money(amount) + " " + cur + ".");
        } catch (NumberFormatException ex) { sender.sendMessage("Enter a positive number."); }
        return true;
    }

    private void openWorth(Player p) {
        MenuHolder h = new MenuHolder("worth"); Inventory inv = Bukkit.createInventory(h, 27, ChatColor.DARK_GREEN + "Worth Guide"); h.inv = inv;
        Material[] icons = {Material.DIAMOND, Material.BONE, Material.WHEAT, Material.BREAD, Material.OAK_LOG, Material.NETHERRACK, Material.END_STONE, Material.CHEST};
        String[] labels = {"Ores", "Mob Drops", "Farming", "Food", "Blocks", "Nether", "End", "All Items"};
        for (int i=0;i<labels.length;i++) inv.setItem(10+i, button(icons[i], ChatColor.GREEN + labels[i], "Browse prices"));
        p.openInventory(inv);
    }
    private void openWorthCategory(Player p, String category) { openWorthCategory(p, category, 0); }
    private void openWorthCategory(Player p, String category, int page) {
        List<Material> list = Arrays.stream(Material.values())
                .filter(m -> m.isItem() && !m.isAir() && worth(m) > 0 && categoryMatches(m, category))
                .sorted(Comparator.comparing(Material::name)).toList();
        int pages = Math.max(1, (list.size() + 44) / 45);
        int current = Math.max(0, Math.min(page, pages - 1));
        MenuHolder h = new MenuHolder("worth-items", category, current);
        Inventory inv = Bukkit.createInventory(h, 54, ChatColor.DARK_GREEN + "Worth: " + category + " (" + (current + 1) + "/" + pages + ")");
        h.inv = inv;
        int start = current * 45;
        for (int i = start; i < Math.min(start + 45, list.size()); i++) {
            Material m = list.get(i);
            inv.setItem(i - start, button(m, ChatColor.YELLOW + nice(m), ChatColor.GRAY + "€" + money(worth(m)) + " each"));
        }
        inv.setItem(45, button(Material.ARROW, ChatColor.RED + "Back", "Worth categories"));
        if (current > 0) inv.setItem(48, button(Material.SPECTRAL_ARROW, ChatColor.YELLOW + "Previous page"));
        inv.setItem(49, button(Material.PAPER, ChatColor.GOLD + "Page " + (current + 1) + "/" + pages, "Items: " + list.size()));
        if (current + 1 < pages) inv.setItem(50, button(Material.SPECTRAL_ARROW, ChatColor.GREEN + "Next page"));
        p.openInventory(inv);
    }
    private boolean categoryMatches(Material m,String c) {
        String n=m.name();
        return switch(c) {
            case "Ores" -> n.contains("DIAMOND")||n.contains("EMERALD")||n.contains("INGOT")||n.contains("COAL")||n.contains("REDSTONE")||n.contains("LAPIS")||n.contains("AMETHYST");
            case "Mob Drops" -> n.contains("BONE")||n.contains("ROTTEN")||n.contains("STRING")||n.contains("PEARL")||n.contains("BLAZE")||n.contains("GUNPOWDER")||n.contains("LEATHER")||n.contains("SPIDER_EYE");
            case "Farming" -> n.contains("WHEAT")||n.contains("CARROT")||n.contains("POTATO")||n.contains("BEETROOT")||n.contains("SUGAR_CANE")||n.contains("PUMPKIN")||n.contains("MELON")||n.contains("SEEDS");
            case "Food" -> n.contains("BREAD")||n.contains("COOKED_")||n.contains("APPLE")||n.contains("CARROT")||n.contains("POTATO");
            case "Blocks" -> n.endsWith("_LOG")||n.endsWith("_PLANKS")||n.contains("STONE")||n.contains("OBSIDIAN");
            case "Nether" -> n.contains("NETHER")||n.contains("QUARTZ")||n.contains("BLAZE");
            case "End" -> n.contains("END_STONE")||n.contains("SHULKER")||n.contains("ELYTRA")||n.contains("ENDER");
            default -> true;
        };
    }
    private void openShop(Player p) {
        MenuHolder h=new MenuHolder("shop-categories"); Inventory inv=Bukkit.createInventory(h,27,ChatColor.DARK_BLUE+"PSIROMOUNA STORE"); h.inv=inv;
        Material[] icons={Material.IRON_SWORD,Material.BRICKS,Material.WHEAT,Material.SPAWNER,Material.COOKED_BEEF,Material.REDSTONE};
        String[] cats={"PvP","Blocks","Farming","Spawners","Food","Redstone"};
        for(int i=0;i<cats.length;i++) inv.setItem(10+i,button(icons[i],ChatColor.AQUA+cats[i],ChatColor.GRAY+"Browse "+cats[i]));
        p.openInventory(inv);
    }

    private void openCommands(Player p) {
        MenuHolder h=new MenuHolder("commands"); Inventory inv=Bukkit.createInventory(h,27,ChatColor.DARK_AQUA+"Psiromouna Commands"); h.inv=inv;
        inv.setItem(10,button(Material.EMERALD,ChatColor.GREEN+"/balance","View Lefta and Shards"));
        inv.setItem(11,button(Material.GOLD_INGOT,ChatColor.YELLOW+"/buy","Browse the shop"));
        inv.setItem(12,button(Material.BOOK,ChatColor.AQUA+"/worth","Search item values"));
        inv.setItem(13,button(Material.HOPPER,ChatColor.GREEN+"/sell","Open sell chest"));
        inv.setItem(14,button(Material.CHEST,ChatColor.LIGHT_PURPLE+"/ah","Auction house"));
        inv.setItem(15,button(Material.WHITE_BANNER,ChatColor.LIGHT_PURPLE+"/banner","Create and manage your floating banner title"));
        p.openInventory(inv);
    }
    private void openSell(Player p) {
        MenuHolder h=new MenuHolder("sell"); Inventory inv=Bukkit.createInventory(h,54,ChatColor.DARK_GREEN+"Sell Chest"); h.inv=inv;
        inv.setItem(49,button(Material.EMERALD_BLOCK,ChatColor.GREEN+"CONFIRM SELL","Sell all priced items above"));
        p.openInventory(inv);
    }
    private void openAuction(Player p) {
        MenuHolder h=new MenuHolder("auction"); Inventory inv=Bukkit.createInventory(h,54,ChatColor.DARK_PURPLE+"Auction House"); h.inv=inv;
        try(Statement st=db.createStatement(); ResultSet rs=st.executeQuery("SELECT id,seller_name,price,item FROM auctions ORDER BY id DESC LIMIT 45")) {
            int slot=0;
            while(rs.next()) {
                ItemStack it=decodeItem(rs.getString("item"));
                ItemMeta meta=it.getItemMeta();
                if(meta!=null) {
                    List<String> lore=new ArrayList<>();
                    lore.add(ChatColor.GOLD+""+ChatColor.BOLD+"PRICE");
                    lore.add(ChatColor.YELLOW+"€ "+ChatColor.WHITE+ChatColor.BOLD+money(rs.getDouble("price")));
                    lore.add(ChatColor.DARK_GRAY+"----------------");
                    lore.add(ChatColor.GRAY+"Seller: "+ChatColor.LIGHT_PURPLE+rs.getString("seller_name"));
                    lore.add(ChatColor.GRAY+"Amount: "+ChatColor.WHITE+"x"+it.getAmount());
                    lore.add(ChatColor.DARK_GRAY+"Listing #"+rs.getInt("id"));
                    lore.add(ChatColor.GREEN+"Click to purchase");
                    meta.setLore(lore);
                    it.setItemMeta(meta);
                }
                inv.setItem(slot++,it);
            }
        } catch(Exception ex) { p.sendMessage(PREFIX+"Could not load auctions."); }
        p.openInventory(inv);
    }
    private void openShopCategory(Player p,String category) { openShopCategory(p, category, 0); }
    private void openShopCategory(Player p, String category, int page) {
        List<Material> items = Arrays.stream(Material.values())
                .filter(m -> m.isItem() && !m.isAir() && worth(m) > 0 && shopMatches(m, category))
                .sorted(Comparator.comparing(Material::name)).toList();
        int pages = Math.max(1, (items.size() + 44) / 45);
        int current = Math.max(0, Math.min(page, pages - 1));
        MenuHolder h = new MenuHolder("shop-items", category, current);
        Inventory inv = Bukkit.createInventory(h, 54, ChatColor.DARK_BLUE + "Shop: " + category + " (" + (current + 1) + "/" + pages + ")");
        h.inv = inv;
        int start = current * 45;
        for (int i = start; i < Math.min(start + 45, items.size()); i++) {
            Material m = items.get(i);
            double price = shopPrice(m);
            inv.setItem(i - start, button(m, ChatColor.YELLOW + nice(m),
                    ChatColor.GRAY + "Worth: €" + money(worth(m)),
                    ChatColor.GOLD + "Price: €" + shopMoney(price),
                    ChatColor.GREEN + "Click to choose quantity"));
        }
        inv.setItem(45, button(Material.ARROW, ChatColor.RED + "Back", "Categories"));
        if (current > 0) inv.setItem(48, button(Material.SPECTRAL_ARROW, ChatColor.YELLOW + "Previous page"));
        inv.setItem(49, button(Material.PAPER, ChatColor.GOLD + "Page " + (current + 1) + "/" + pages, "Items: " + items.size()));
        if (current + 1 < pages) inv.setItem(50, button(Material.SPECTRAL_ARROW, ChatColor.GREEN + "Next page"));
        p.openInventory(inv);
    }
    private boolean shopMatches(Material m, String c) {
        if (m == null || !m.isItem() || m.isAir()) return false;
        String n = m.name();
        return switch (c) {
            case "Blocks" -> COMMON_BLOCKS.contains(n);
            case "PvP" -> Set.of("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS").contains(n);
            case "Farming" -> Set.of("WHEAT_SEEDS", "BEETROOT_SEEDS", "MELON_SEEDS", "PUMPKIN_SEEDS",
                    "WHEAT", "CARROT", "POTATO", "BEETROOT", "SUGAR_CANE", "PUMPKIN",
                    "MELON_SLICE", "KELP", "SEA_PICKLE", "PINK_PETALS").contains(n);
            case "Food" -> m.isEdible();
            case "Redstone" -> Set.of("REDSTONE", "REDSTONE_BLOCK", "REDSTONE_TORCH", "REDSTONE_LAMP",
                    "PISTON", "STICKY_PISTON", "HOPPER", "REPEATER", "COMPARATOR", "OBSERVER",
                    "DISPENSER", "DROPPER", "LEVER", "STONE_BUTTON", "OAK_BUTTON",
                    "RAIL", "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL", "TARGET").contains(n);
            default -> false;
        };
    }
    private boolean isBuildingBlock(Material m, String n) {
        return m != null && m.isBlock() && COMMON_BLOCKS.contains(n);
    }

    private void openSpawnerShop(Player p) { openSpawnerShop(p, 0); }
    private void openSpawnerShop(Player p, int page) {
        int pages = (SPAWNER_TYPES.size() + 44) / 45;
        int current = Math.max(0, Math.min(page, pages - 1));
        MenuHolder h = new MenuHolder("spawner-types", "", current);
        Inventory inv = Bukkit.createInventory(h, 54, ChatColor.DARK_AQUA + "Mob Spawners (" + (current + 1) + "/" + pages + ")");
        h.inv = inv;
        int start = current * 45;
        for (int i = start; i < Math.min(start + 45, SPAWNER_TYPES.size()); i++) {
            org.bukkit.entity.EntityType type = SPAWNER_TYPES.get(i);
            Material icon = Material.matchMaterial(type.name() + "_SPAWN_EGG");
            if (icon == null) icon = Material.SPAWNER;
            inv.setItem(i - start, button(icon, ChatColor.AQUA + niceEntity(type),
                    ChatColor.GRAY + "Cost: " + ChatColor.AQUA + spawnerPrice(type) + " Shards",
                    ChatColor.GREEN + "Click to choose quantity"));
        }
        inv.setItem(45, button(Material.ARROW, ChatColor.RED + "Back", "Shop categories"));
        if (current > 0) inv.setItem(48, button(Material.SPECTRAL_ARROW, ChatColor.YELLOW + "Previous page"));
        inv.setItem(49, button(Material.PAPER, ChatColor.GOLD + "Page " + (current + 1) + "/" + pages));
        if (current + 1 < pages) inv.setItem(50, button(Material.SPECTRAL_ARROW, ChatColor.GREEN + "Next page"));
        p.openInventory(inv);
    }
    private String niceEntity(org.bukkit.entity.EntityType type) {
        return Arrays.stream(type.name().toLowerCase(Locale.ROOT).split("_"))
                .map(v -> Character.toUpperCase(v.charAt(0)) + v.substring(1)).collect(Collectors.joining(" "));
    }
    private int spawnerPrice(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case PIG, COW, SHEEP, CHICKEN, RABBIT -> 20;
            case ZOMBIE, SKELETON, SPIDER, DROWNED, HUSK, STRAY -> 30;
            case CREEPER, CAVE_SPIDER, SLIME, WITCH, PILLAGER -> 45;
            case ENDERMAN, BLAZE, MAGMA_CUBE, PHANTOM, GUARDIAN -> 65;
            case VILLAGER, IRON_GOLEM -> 90;
            case SHULKER -> 120;
            default -> 50;
        };
    }
    private void openQuantityMenu(Player p, Material material, org.bukkit.entity.EntityType spawnerType,
                                  double unitPrice, String currency, String category) {
        MenuHolder h = new MenuHolder("buy-quantity", category, 0, material, spawnerType, unitPrice, currency);
        Inventory inv = Bukkit.createInventory(h, 27, ChatColor.DARK_GREEN + "Choose quantity");
        h.inv = inv;
        int[] amounts = {1, 16, 32, 64};
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            ItemStack icon = new ItemStack(material, amount);
            if (spawnerType != null) applySpawnerType(icon, spawnerType);
            inv.setItem(10 + i, buttonWithAmount(icon, ChatColor.GREEN + "Buy x" + amount,
                    ChatColor.GRAY + "Unit price: " + currencyLabel(currency) + shopMoney(unitPrice),
                    ChatColor.YELLOW + "Total: " + currencyLabel(currency) + shopMoney(unitPrice * amount),
                    ChatColor.GREEN + "Click to confirm"));
        }
        inv.setItem(18, button(Material.ARROW, ChatColor.RED + "Back"));
        p.openInventory(inv);
    }
    private String currencyLabel(String currency) { return currency.equals("shards") ? "" : "€"; }
    private ItemStack buttonWithAmount(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); }
        return item;
    }
    private boolean applySpawnerType(ItemStack item, org.bukkit.entity.EntityType type) {
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return false;
        if (!(meta.getBlockState() instanceof CreatureSpawner state)) return false;
        state.setSpawnedType(type);
        meta.setBlockState(state);
        item.setItemMeta(meta);
        return true;
    }
    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) remaining -= item.getMaxStackSize();
            else if (slot.isSimilar(item)) remaining -= Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }
    private void finishShopPurchase(Player p, MenuHolder h, int amount) {
        double total = h.unitPrice * amount;
        if (getBalance(p.getUniqueId(), h.currency) + 0.000001 < total) {
            p.sendMessage(PREFIX + ChatColor.RED + "Not enough " + (h.currency.equals("shards") ? "Shards" : "Lefta")
                    + ". Total required: " + shopMoney(total) + ".");
            return;
        }
        ItemStack bought = new ItemStack(h.itemMaterial, amount);
        if (h.spawnerType != null && !applySpawnerType(bought, h.spawnerType)) {
            p.sendMessage(PREFIX + ChatColor.RED + "Could not create that mob spawner.");
            return;
        }
        if (!canFit(p, bought)) { p.sendMessage(PREFIX + ChatColor.RED + "Not enough inventory space for that quantity."); return; }
        p.getInventory().addItem(bought);
        changeBalance(p.getUniqueId(), h.currency, -total);
        p.sendMessage(PREFIX + ChatColor.GREEN + "Bought x" + amount + " " +
                (h.spawnerType == null ? nice(h.itemMaterial) : niceEntity(h.spawnerType) + " Spawner") +
                " for " + (h.currency.equals("shards") ? shopMoney(total) + " Shards" : "€" + shopMoney(total)) + ".");
        p.closeInventory();
    }
    private ItemStack button(Material m,String name,String... lore) {
        ItemStack it=new ItemStack(m); ItemMeta meta=it.getItemMeta(); if(meta!=null){meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));it.setItemMeta(meta);} return it;
    }
    private ItemStack addToSellChest(Inventory inventory, ItemStack source) {
        ItemStack remaining = source.clone();
        // First merge into matching stacks.
        for (int slot = 0; slot < 45 && remaining.getAmount() > 0; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir() || !existing.isSimilar(remaining)) continue;
            int room = existing.getMaxStackSize() - existing.getAmount();
            if (room <= 0) continue;
            int moved = Math.min(room, remaining.getAmount());
            existing.setAmount(existing.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
        }
        // Then use empty storage slots only; never touch the control row.
        for (int slot = 0; slot < 45 && remaining.getAmount() > 0; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing != null && !existing.getType().isAir()) continue;
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
            ItemStack placed = remaining.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            remaining.setAmount(remaining.getAmount() - moved);
        }
        return remaining.getAmount() <= 0 ? null : remaining;
    }

    @EventHandler public void onSellDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder h) || !h.type.equals("sell")) return;
        // Only allow drags that touch storage slots; the bottom control row stays locked.
        for (int raw : e.getRawSlots()) {
            if (raw >= 45 && raw < e.getInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler public void onMenuClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p) || !(e.getInventory().getHolder() instanceof MenuHolder h)) return;
        if (h.type.equals("sell")) {
            int raw = e.getRawSlot();
            if (raw >= e.getInventory().getSize()) {
                // Allow normal clicks in the player's inventory so items can be picked up.
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                    ItemStack moving = e.getCurrentItem();
                    if (moving != null && !moving.getType().isAir()) {
                        ItemStack remainder = addToSellChest(h.inv, moving);
                        if (remainder == null || remainder.getType().isAir()) {
                            e.setCurrentItem(null);
                        } else {
                            e.setCurrentItem(remainder);
                        }
                    }
                } else {
                    e.setCancelled(false);
                }
                return;
            }
            if (raw >= 0 && raw < 45) {
                e.setCancelled(false);
                return;
            }
            e.setCancelled(true);
            if (raw == 49) {
                double total = 0; int count = 0;
                for (int i = 0; i < 45; i++) {
                    ItemStack it = h.inv.getItem(i);
                    if (it == null || it.getType().isAir()) continue;
                    double v = worth(it.getType());
                    if (v > 0) { total += v * it.getAmount(); count += it.getAmount(); }
                }
                if (count == 0) { p.sendMessage(PREFIX + "No sellable items."); return; }
                for (int i = 0; i < 45; i++) {
                    ItemStack it = h.inv.getItem(i);
                    if (it != null && !it.getType().isAir() && worth(it.getType()) > 0) h.inv.setItem(i, null);
                }
                changeBalance(p.getUniqueId(), "lefta", total);
                p.sendMessage(PREFIX + ChatColor.GREEN + "Sold " + count + " items for €" + money(total) + ".");
                p.closeInventory();
            }
            return;
        }

        e.setCancelled(true);
        if(e.getRawSlot()<0||e.getRawSlot()>=e.getInventory().getSize()) return;
        ItemStack clicked=e.getCurrentItem(); if(clicked==null||clicked.getType().isAir()) return;

        if(h.type.equals("commands")) { int slot=e.getRawSlot(); String[] cmds={"/balance","/buy","/worth","/sell","/ah","/banner"}; if(slot>=10&&slot<=15)p.performCommand(cmds[slot-10].substring(1)); return; }
        if(h.type.equals("shop-categories")) { String[] cats={"PvP","Blocks","Farming","Spawners","Food","Redstone"};int ix=e.getRawSlot()-10;if(ix>=0&&ix<6){if(ix==3)openSpawnerShop(p);else openShopCategory(p,cats[ix]);}return; }
        if(h.type.equals("shop-items")) {
            int slot = e.getRawSlot();
            if (slot == 45) { openShop(p); return; }
            if (slot == 48 && h.page > 0) { openShopCategory(p, h.category, h.page - 1); return; }
            if (slot == 50) { openShopCategory(p, h.category, h.page + 1); return; }
            if (slot < 0 || slot >= 45) return;
            Material m = clicked.getType();
            double price = shopPrice(m);
            openQuantityMenu(p, m, null, price, "lefta", h.category);
            return;
        }
        if(h.type.equals("auction")) { if(clicked.hasItemMeta()&&clicked.getItemMeta().hasLore()){String line=clicked.getItemMeta().getLore().get(0);if(line.startsWith("Listing #")){auctionCommand(p,new String[]{"buy",line.substring(9)});openAuction(p);}}return; }
        if(h.type.equals("worth")) {
            String[] cats={"Ores","Mob Drops","Farming","Food","Blocks","Nether","End","All Items"};
            int index=e.getRawSlot()-10; if(index>=0&&index<cats.length) openWorthCategory(p,cats[index]); return;
        }
        if (h.type.equals("worth-items")) {
            int slot = e.getRawSlot();
            if (slot == 45) { openWorth(p); return; }
            if (slot == 48 && h.page > 0) { openWorthCategory(p, h.category, h.page - 1); return; }
            if (slot == 50) { openWorthCategory(p, h.category, h.page + 1); return; }
            return;
        }
        if(h.type.equals("shop")) {
            if(e.getRawSlot()==49){openSpawnerShop(p);return;}
            Material m=clicked.getType(); double price=Math.max(1,worth(m)*2);
            if(getBalance(p.getUniqueId(),"lefta")<price){p.sendMessage(PREFIX+ChatColor.RED+"Not enough Lefta.");return;}
            if(!p.getInventory().addItem(new ItemStack(m,1)).isEmpty()){p.sendMessage(PREFIX+"Your inventory is full.");return;}
            changeBalance(p.getUniqueId(),"lefta",-price); p.sendMessage(PREFIX+ChatColor.GREEN+"Bought "+nice(m)+" for €"+money(price)+"."); return;
        }
        if (h.type.equals("spawner-types")) {
            int slot = e.getRawSlot();
            if (slot == 45) { openShop(p); return; }
            if (slot == 48 && h.page > 0) { openSpawnerShop(p, h.page - 1); return; }
            if (slot == 50) { openSpawnerShop(p, h.page + 1); return; }
            if (slot < 0 || slot >= 45) return;
            int index = h.page * 45 + slot;
            if (index >= SPAWNER_TYPES.size()) return;
            org.bukkit.entity.EntityType type = SPAWNER_TYPES.get(index);
            openQuantityMenu(p, Material.SPAWNER, type, spawnerPrice(type), "shards", "spawners");
            return;
        }
        if (h.type.equals("buy-quantity")) {
            int slot = e.getRawSlot();
            if (slot == 18) {
                if (h.spawnerType != null) openSpawnerShop(p);
                else openShopCategory(p, h.category);
                return;
            }
            int[] amounts = {1, 16, 32, 64};
            if (slot >= 10 && slot <= 13) finishShopPurchase(p, h, amounts[slot - 10]);
            return;
        }
    }

    private boolean auctionCommand(Player p,String[] args) {
        if(args.length==0||args[0].equalsIgnoreCase("help")) {
            p.sendMessage(ChatColor.GOLD+"/ah - list auctions | /ah sell <price> | /ah buy <id> | /ah cancel <id>"); return true;
        }
        if(args[0].equalsIgnoreCase("sell")) {
            if(args.length!=2){p.sendMessage(PREFIX+"Usage: /ah sell <price>");return true;}
            ItemStack hand=p.getInventory().getItemInMainHand();
            if(hand.getType().isAir()){p.sendMessage(PREFIX+"Hold the item you want to list.");return true;}
            try {
                double price=Double.parseDouble(args[1]); if(!Double.isFinite(price)||price<=0) throw new NumberFormatException();
                String encoded=encodeItem(hand); try(PreparedStatement ps=db.prepareStatement("INSERT INTO auctions(seller,seller_name,price,item) VALUES(?,?,?,?)")) {
                    ps.setString(1,p.getUniqueId().toString());ps.setString(2,p.getName());ps.setDouble(3,price);ps.setString(4,encoded);ps.executeUpdate();
                }
                p.getInventory().setItemInMainHand(null);p.sendMessage(PREFIX+ChatColor.GREEN+"Listed item for €"+money(price)+".");
            } catch(Exception ex){p.sendMessage(PREFIX+ChatColor.RED+"Invalid price or listing error.");}
            return true;
        }
        if(args[0].equalsIgnoreCase("buy")&&args.length==2) {
            try {
                int id=Integer.parseInt(args[1]); Listing l=getListing(id);
                if(l==null){p.sendMessage(PREFIX+"Listing not found.");return true;}
                if(l.seller.equals(p.getUniqueId())){p.sendMessage(PREFIX+"You cannot buy your own listing.");return true;}
                if(getBalance(p.getUniqueId(),"lefta")<l.price){p.sendMessage(PREFIX+"Not enough Lefta.");return true;}
                ItemStack item=decodeItem(l.item); if(!p.getInventory().addItem(item).isEmpty()){p.sendMessage(PREFIX+"Your inventory is full.");return true;}
                try(PreparedStatement ps=db.prepareStatement("DELETE FROM auctions WHERE id=?")){ps.setInt(1,id);ps.executeUpdate();}
                changeBalance(p.getUniqueId(),"lefta",-l.price);changeBalance(l.seller,"lefta",l.price);
                p.sendMessage(PREFIX+ChatColor.GREEN+"Purchased listing #"+id+".");
            }catch(Exception ex){p.sendMessage(PREFIX+ChatColor.RED+"Could not complete purchase.");}
            return true;
        }
        if(args[0].equalsIgnoreCase("cancel")&&args.length==2) {
            try {int id=Integer.parseInt(args[1]); Listing l=getListing(id); if(l==null||!l.seller.equals(p.getUniqueId())){p.sendMessage(PREFIX+"That is not your listing.");return true;}
                ItemStack item=decodeItem(l.item); if(!p.getInventory().addItem(item).isEmpty()){p.sendMessage(PREFIX+"Inventory full.");return true;}
                try(PreparedStatement ps=db.prepareStatement("DELETE FROM auctions WHERE id=?")){ps.setInt(1,id);ps.executeUpdate();}
                p.sendMessage(PREFIX+ChatColor.GREEN+"Listing cancelled and item returned.");
            }catch(Exception ex){p.sendMessage(PREFIX+"Could not cancel listing.");} return true;
        }
        p.sendMessage(ChatColor.GOLD+"Active auctions:");
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT id,seller_name,price,item FROM auctions ORDER BY id DESC LIMIT 20")){
            while(rs.next()){ItemStack item=decodeItem(rs.getString("item"));p.sendMessage(ChatColor.YELLOW+"#"+rs.getInt("id")+" "+nice(item.getType())+" x"+item.getAmount()+ChatColor.GRAY+" by "+rs.getString("seller_name")+" — "+ChatColor.GOLD+money(rs.getDouble("price"))+" Lefta");}
        }catch(Exception ex){p.sendMessage(PREFIX+"Could not load auctions.");}
        return true;
    }
    private Listing getListing(int id)throws SQLException {
        try(PreparedStatement ps=db.prepareStatement("SELECT seller,price,item FROM auctions WHERE id=?")){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;return new Listing(UUID.fromString(rs.getString(1)),rs.getDouble(2),rs.getString(3));}}
    }
    private String encodeItem(ItemStack item)throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();try(BukkitObjectOutputStream out=new BukkitObjectOutputStream(b)){out.writeObject(item);}return Base64.getEncoder().encodeToString(b.toByteArray());
    }
    private ItemStack decodeItem(String data)throws IOException,ClassNotFoundException {
        byte[] b=Base64.getDecoder().decode(data);try(BukkitObjectInputStream in=new BukkitObjectInputStream(new ByteArrayInputStream(b))){return (ItemStack)in.readObject();}
    }
    private record Listing(UUID seller,double price,String item) {}
    private static final class MenuHolder implements InventoryHolder {
        final String type;
        final String category;
        final int page;
        final Material itemMaterial;
        final org.bukkit.entity.EntityType spawnerType;
        final double unitPrice;
        final String currency;
        Inventory inv;
        MenuHolder(String type) { this(type, "", 0); }
        MenuHolder(String type, String category, int page) {
            this(type, category, page, null, null, 0, "lefta");
        }
        MenuHolder(String type, String category, int page, Material itemMaterial,
                   org.bukkit.entity.EntityType spawnerType, double unitPrice, String currency) {
            this.type = type;
            this.category = category;
            this.page = page;
            this.itemMaterial = itemMaterial;
            this.spawnerType = spawnerType;
            this.unitPrice = unitPrice;
            this.currency = currency;
        }
        @Override public Inventory getInventory() { return inv; }
    }


    @EventHandler public void onSellClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p=(Player)e.getPlayer();
        if (!(e.getInventory().getHolder() instanceof MenuHolder)) return;
        MenuHolder h=(MenuHolder)e.getInventory().getHolder();
        if (!h.type.equals("sell")) return;
        for(int i=0;i<45;i++) {
            ItemStack it=e.getInventory().getItem(i);
            if(it!=null&&!it.getType().isAir()) {
                Map<Integer,ItemStack> left=p.getInventory().addItem(it);
                for(ItemStack drop:left.values()) p.getWorld().dropItemNaturally(p.getLocation(),drop);
            }
        }
    }

    private long afkCooldownUntil(UUID id) {
        try (PreparedStatement ps = db.prepareStatement("SELECT until_ms FROM afk_cooldowns WHERE uuid=?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0L; }
        } catch (SQLException ex) { return 0L; }
    }
    private void setAfkCooldown(UUID id, long until) {
        try (PreparedStatement ps = db.prepareStatement("INSERT OR REPLACE INTO afk_cooldowns(uuid,until_ms) VALUES(?,?)")) {
            ps.setString(1, id.toString()); ps.setLong(2, until); ps.executeUpdate();
        } catch (SQLException ex) { getLogger().warning("Could not save AFK cooldown: " + ex.getMessage()); }
    }
    private boolean afkCommand(Player p) {
        UUID id = p.getUniqueId();
        if (afkPlayers.contains(id)) {
            afkPlayers.remove(id);
            p.setInvulnerable(false);
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
            p.setPlayerListName(p.getName());
            p.sendMessage(PREFIX + ChatColor.GREEN + "You are no longer AFK. You can use /afk again after the 30-minute cooldown.");
            return true;
        }
        long now = System.currentTimeMillis();
        long until = afkCooldownUntil(id);
        if (until > now) {
            long minutes = Math.max(1L, (until - now + 59_999L) / 60_000L);
            p.sendMessage(PREFIX + ChatColor.RED + "AFK is on cooldown. You can use it again in " + minutes + " minute(s).");
            return true;
        }
        setAfkCooldown(id, now + AFK_COOLDOWN_MILLIS);
        afkPlayers.add(id);
        p.setInvulnerable(true);
        p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        p.setPlayerListName(ChatColor.YELLOW + "[AFK] " + ChatColor.RESET + p.getName());
        p.sendMessage(PREFIX + ChatColor.YELLOW + "You are now AFK. You are protected and frozen until you use /afk again.");
        return true;
    }

    private boolean isPvpTagged(Player p) {
        Long until = pvpTaggedUntil.get(p.getUniqueId());
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) { pvpTaggedUntil.remove(p.getUniqueId()); return false; }
        return true;
    }
    private void tagForPvp(Player p) {
        if (afkPlayers.contains(p.getUniqueId())) return;
        pvpTaggedUntil.put(p.getUniqueId(), System.currentTimeMillis() + PVP_TAG_MILLIS);
        p.sendActionBar(ChatColor.RED + "⚔ PVP TAG " + ChatColor.GRAY + "— 15 seconds");
    }
    private boolean isAllowedShopCommand(String message) {
        String cmd = message.toLowerCase(Locale.ROOT).split("\\s+", 2)[0];
        return cmd.equals("/shop") || cmd.equals("/buy") || cmd.equals("/psiromounasmp:shop") || cmd.equals("/psiromounasmp:buy");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTagDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player direct) attacker = direct;
        else if (e.getDamager() instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player shooter) attacker = shooter;
        if (attacker == null || attacker.equals(victim)) return;
        if (afkPlayers.contains(victim.getUniqueId()) || afkPlayers.contains(attacker.getUniqueId())) { e.setCancelled(true); return; }
        tagForPvp(victim);
        tagForPvp(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTaggedCommands(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (afkPlayers.contains(p.getUniqueId())) {
            if (!e.getMessage().equalsIgnoreCase("/afk")) { e.setCancelled(true); p.sendMessage(PREFIX + ChatColor.RED + "You are AFK. Use /afk to return."); }
            return;
        }
        if (isPvpTagged(p) && !isAllowedShopCommand(e.getMessage())) {
            e.setCancelled(true);
            p.sendMessage(PREFIX + ChatColor.RED + "You are combat-tagged. Only /shop and /buy are available for 15 seconds after PvP.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && afkPlayers.contains(p.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkMove(org.bukkit.event.player.PlayerMoveEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId()) && e.getTo() != null &&
                (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getY() != e.getTo().getY() || e.getFrom().getZ() != e.getTo().getZ())) {
            e.setTo(e.getFrom());
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkVelocity(org.bukkit.event.player.PlayerVelocityEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkInteract(org.bukkit.event.player.PlayerInteractEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkBreak(org.bukkit.event.block.BlockBreakEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (afkPlayers.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && afkPlayers.contains(p.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && afkPlayers.contains(p.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && afkPlayers.contains(p.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedInteract(org.bukkit.event.player.PlayerInteractEvent e) {
        if (isPvpTagged(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedBreak(org.bukkit.event.block.BlockBreakEvent e) {
        if (isPvpTagged(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (isPvpTagged(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
        if (isPvpTagged(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedSwap(org.bukkit.event.player.PlayerSwapHandItemsEvent e) {
        if (isPvpTagged(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isPvpTagged(p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder holder) ||
                !(holder.type.equals("shop-categories") || holder.type.equals("shop-items") || holder.type.equals("spawner-types") || holder.type.equals("buy-quantity"))) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvpTaggedInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isPvpTagged(p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder holder) ||
                !(holder.type.equals("shop-categories") || holder.type.equals("shop-items") || holder.type.equals("spawner-types") || holder.type.equals("buy-quantity"))) e.setCancelled(true);
    }

    private void onQuitCleanup(Player p) {
        afkPlayers.remove(p.getUniqueId());
        pvpTaggedUntil.remove(p.getUniqueId());
        p.setInvulnerable(false);
    }

    @EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p=e.getPlayer();
        p.setInvulnerable(false);
        e.setJoinMessage(ChatColor.DARK_AQUA+"✦ "+ChatColor.AQUA+p.getName()+ChatColor.GRAY+" joined Psiromouna SMP");
        p.sendTitle(ChatColor.AQUA+""+ChatColor.BOLD+"WELCOME TO",ChatColor.WHITE+"PSIROMOUNA SMP",10,80,20);
        updateDisplays(p);
    }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) { onQuitCleanup(e.getPlayer()); }

    private void updateDisplays(Player p) {
        double lefta = getBalance(p.getUniqueId(), "lefta");
        double shards = getBalance(p.getUniqueId(), "shards");
        p.setPlayerListName(afkPlayers.contains(p.getUniqueId())
                ? ChatColor.YELLOW + "[AFK] " + ChatColor.RESET + p.getName() : p.getName());
        p.setPlayerListHeaderFooter(ChatColor.DARK_AQUA + "✦ PSIROMOUNA SMP ✦\n" + ChatColor.GRAY + "Survive • Trade • Build",
                ChatColor.GOLD + "Lefta: " + money(lefta) + "   " + ChatColor.AQUA + "Shards: " + money(shards) + "\n"
                        + ChatColor.GRAY + "Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        ChatColor pink = ChatColor.LIGHT_PURPLE;
        Objective obj = board.registerNewObjective("smpinfo", Criteria.DUMMY, pink + "" + ChatColor.BOLD + "✦ PSIROMOUNA ✦");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        String[] lines = {
                ChatColor.GOLD + "€ " + money(lefta) + " Lefta",
                ChatColor.AQUA + "✦ " + money(shards) + " Shards"
        };
        int score = lines.length;
        for (String line : lines) {
            Score sidebarLine = obj.getScore(line);
            sidebarLine.setScore(score--);
            sidebarLine.setNumberFormat(NumberFormat.blank());
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamId = "cash" + target.getUniqueId().toString().replace("-", "").substring(0, 8);
            Team nameTag = board.registerNewTeam(teamId);
            nameTag.addEntry(target.getName());
            if (afkPlayers.contains(target.getUniqueId())) nameTag.setPrefix(ChatColor.YELLOW + "[AFK] " + ChatColor.RESET);
            nameTag.setSuffix(ChatColor.GRAY + " | " + ChatColor.GOLD + "€"
                    + money(getBalance(target.getUniqueId(), "lefta")));
        }
        p.setScoreboard(board);
    }

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        String c=command.getName().toLowerCase(Locale.ROOT);
        if(args.length==1) {
            List<String> opts=switch(c) {
                case "worth" -> List.of("search","hand");
                case "cmds" -> List.of();
                case "sell" -> List.of("hand","all");
                case "ah" -> List.of("sell","buy","cancel","help");
                case "currency" -> List.of("give");
                case "banner" -> List.of("rename", "move", "remove");
                case "afk" -> List.of();
                default -> List.of();
            };
            return opts.stream().filter(s->s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if(c.equals("currency")&&args.length==2&&sender.hasPermission("psiromouna.admin"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s->s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        if(c.equals("currency")&&args.length==3)return List.of("lefta","shards");
        return List.of();
    }
}
