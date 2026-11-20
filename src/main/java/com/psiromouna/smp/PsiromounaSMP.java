package com.psiromouna.smp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
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
    private static final String PREFIX = ChatColor.DARK_AQUA + "[SMP] " + ChatColor.RESET;
    private static final Map<Material, Double> DEFAULT_PRICES = new LinkedHashMap<>();

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
        getServer().getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new EconomyExpansion(this).register();
        for (String name : List.of("worth", "sell", "shop", "buy", "ah", "balance", "currency")) {
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
        if (getConfig().contains("prices." + m.name())) return getConfig().getDouble("prices." + m.name());
        if (DEFAULT_PRICES.containsKey(m)) return DEFAULT_PRICES.get(m);
        if (m.isAir() || !m.isItem()) return 0;
        String n=m.name();
        if (n.contains("ELYTRA")) return 2500;
        if (n.contains("NETHERITE")) return 350;
        if (n.contains("TOTEM")) return 900;
        if (n.contains("SHULKER")) return 150;
        if (n.contains("DIAMOND")) return 500;
        if (n.contains("TRIDENT") || n.contains("HEART_OF_THE_SEA")) return 800;
        if (n.contains("SPAWN_EGG") || n.contains("SPAWNER")) return 0;
        if (n.contains("ANCIENT_DEBRIS")) return 300;
        if (n.contains("KELP")) return 0.25;
        if (n.contains("PICKLE") || n.contains("PETAL")) return 150;
        if (n.contains("LOG") || n.contains("PLANKS")) return 4;
        if (n.contains("SEEDS") || n.contains("WHEAT") || n.contains("CARROT") || n.contains("POTATO")) return 2;
        if (n.contains("STONE") || n.contains("DIRT") || n.contains("COBBLE")) return 0.5;
        if (n.contains("COOKED_") || n.contains("BREAD")) return 8;
        if (n.contains("WOOL") || n.contains("GLASS")) return 3;
        if (n.contains("BONE") || n.contains("STRING") || n.contains("ROTTEN_FLESH")) return 2;
        return 5;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("currency")) return currencyCommand(sender, args);
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }

        switch (name) {
            case "worth" -> {
                if (args.length == 0) { openWorth(p); return true; }
                if (args[0].equalsIgnoreCase("search")) {
                    if (args.length < 2) { p.sendMessage(PREFIX + "Usage: /worth search <item>"); return true; }
                    String q = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
                    List<Material> matches = Arrays.stream(Material.values()).filter(m -> !m.isAir() && worth(m) > 0 &&
                            (m.name().toLowerCase(Locale.ROOT).contains(q.replace(' ', '_')) || nice(m).toLowerCase(Locale.ROOT).contains(q))).limit(20).toList();
                    if (matches.isEmpty()) p.sendMessage(PREFIX + ChatColor.YELLOW + "No priced items found.");
                    else { p.sendMessage(ChatColor.GOLD + "Worth search: " + q); for (Material m : matches) p.sendMessage(ChatColor.YELLOW + nice(m) + ChatColor.GRAY + " — " + ChatColor.GOLD + money(worth(m)) + " Lefta each"); }
                    return true;
                }
                Material m = args[0].equalsIgnoreCase("hand") ? p.getInventory().getItemInMainHand().getType()
                        : Material.matchMaterial(String.join("_", args).toUpperCase(Locale.ROOT));
                if (m == null || m.isAir()) p.sendMessage(PREFIX + ChatColor.RED + "Unknown item. Try /worth search <text>.");
                else p.sendMessage(PREFIX + ChatColor.YELLOW + nice(m) + ChatColor.GRAY + " is worth " + ChatColor.GOLD + money(worth(m)) + " Lefta each.");
                return true;
            }
            case "sell" -> {
                if (args.length == 0 || args[0].equalsIgnoreCase("hand")) {
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
                    else { changeBalance(p.getUniqueId(), "lefta", total); p.sendMessage(PREFIX + ChatColor.GREEN + "Sold " + sold + " items for " + money(total) + " Lefta."); }
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
            case "ah" -> { return auctionCommand(p, args); }
        }
        return false;
    }

    private void sellStack(Player p, ItemStack item, int amount) {
        double price = worth(item.getType());
        if (price <= 0) { p.sendMessage(PREFIX + ChatColor.RED + "That item has no sell price."); return; }
        double total = price * amount;
        item.setAmount(item.getAmount() - amount);
        changeBalance(p.getUniqueId(), "lefta", total);
        p.sendMessage(PREFIX + ChatColor.GREEN + "Sold " + amount + "x " + nice(item.getType()) + " for " + money(total) + " Lefta.");
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
    private void openWorthCategory(Player p, String category) {
        List<Material> list = Arrays.stream(Material.values()).filter(m -> worth(m)>0 && categoryMatches(m,category)).sorted(Comparator.comparing(Material::name)).limit(45).toList();
        MenuHolder h = new MenuHolder("worth-items"); Inventory inv = Bukkit.createInventory(h,54,ChatColor.DARK_GREEN+"Worth: "+category); h.inv=inv;
        int slot=0; for(Material m:list) inv.setItem(slot++, button(m,ChatColor.YELLOW+nice(m),ChatColor.GRAY+money(worth(m))+" Lefta each"));
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
        MenuHolder h = new MenuHolder("shop"); Inventory inv = Bukkit.createInventory(h,54,ChatColor.DARK_BLUE+"Lefta Shop"); h.inv=inv;
        List<Material> items = DEFAULT_PRICES.keySet().stream().filter(m->m!=Material.SPAWNER).toList();
        int slot=0; for(Material m:items) inv.setItem(slot++, button(m,ChatColor.YELLOW+nice(m),ChatColor.GRAY+"Price: "+ChatColor.GOLD+money(Math.max(1,worth(m)*2))+" Lefta",ChatColor.GREEN+"Click to buy 1"));
        inv.setItem(49,button(Material.SPAWNER,ChatColor.AQUA+"Shard Spawner Shop",ChatColor.GRAY+"Buy spawners with Shards"));
        p.openInventory(inv);
    }
    private void openSpawnerShop(Player p) {
        MenuHolder h=new MenuHolder("spawners"); Inventory inv=Bukkit.createInventory(h,27,ChatColor.DARK_AQUA+"Shard Shop"); h.inv=inv;
        inv.setItem(13,button(Material.SPAWNER,ChatColor.AQUA+"Mob Spawner",ChatColor.GRAY+"Cost: 25 Shards",ChatColor.GREEN+"Click to purchase"));
        p.openInventory(inv);
    }
    private ItemStack button(Material m,String name,String... lore) {
        ItemStack it=new ItemStack(m); ItemMeta meta=it.getItemMeta(); if(meta!=null){meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));it.setItemMeta(meta);} return it;
    }
    @EventHandler public void onMenuClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p) || !(e.getInventory().getHolder() instanceof MenuHolder h)) return;
        e.setCancelled(true); if(e.getRawSlot()<0||e.getRawSlot()>=e.getInventory().getSize()) return;
        ItemStack clicked=e.getCurrentItem(); if(clicked==null||clicked.getType().isAir()) return;
        if(h.type.equals("worth")) {
            String[] cats={"Ores","Mob Drops","Farming","Food","Blocks","Nether","End","All Items"};
            int index=e.getRawSlot()-10; if(index>=0&&index<cats.length) openWorthCategory(p,cats[index]); return;
        }
        if(h.type.equals("shop")) {
            if(e.getRawSlot()==49){openSpawnerShop(p);return;}
            Material m=clicked.getType(); double price=Math.max(1,worth(m)*2);
            if(getBalance(p.getUniqueId(),"lefta")<price){p.sendMessage(PREFIX+ChatColor.RED+"Not enough Lefta.");return;}
            if(!p.getInventory().addItem(new ItemStack(m,1)).isEmpty()){p.sendMessage(PREFIX+"Your inventory is full.");return;}
            changeBalance(p.getUniqueId(),"lefta",-price); p.sendMessage(PREFIX+ChatColor.GREEN+"Bought "+nice(m)+" for "+money(price)+" Lefta."); return;
        }
        if(h.type.equals("spawners")&&e.getRawSlot()==13){
            if(getBalance(p.getUniqueId(),"shards")<25){p.sendMessage(PREFIX+ChatColor.RED+"Not enough Shards.");return;}
            if(!p.getInventory().addItem(new ItemStack(Material.SPAWNER)).isEmpty()){p.sendMessage(PREFIX+"Your inventory is full.");return;}
            changeBalance(p.getUniqueId(),"shards",-25);p.sendMessage(PREFIX+ChatColor.AQUA+"Bought a Mob Spawner for 25 Shards.");
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
                p.getInventory().setItemInMainHand(null);p.sendMessage(PREFIX+ChatColor.GREEN+"Listed item for "+money(price)+" Lefta.");
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
        final String type; Inventory inv; MenuHolder(String t){type=t;} @Override public Inventory getInventory(){return inv;}
    }

    @EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p=e.getPlayer();
        e.setJoinMessage(ChatColor.DARK_AQUA+"✦ "+ChatColor.AQUA+p.getName()+ChatColor.GRAY+" joined Psiromouna SMP");
        p.sendTitle(ChatColor.AQUA+""+ChatColor.BOLD+"WELCOME TO",ChatColor.WHITE+"PSIROMOUNA SMP",10,80,20);
        updateDisplays(p);
    }

    private void updateDisplays(Player p) {
        double lefta=getBalance(p.getUniqueId(),"lefta"), shards=getBalance(p.getUniqueId(),"shards");
        p.setPlayerListHeaderFooter(ChatColor.DARK_AQUA+"✦ PSIROMOUNA SMP ✦\n"+ChatColor.GRAY+"Survive • Trade • Build",
                ChatColor.GOLD+"Lefta: "+money(lefta)+"   "+ChatColor.AQUA+"Shards: "+money(shards)+"\n"+ChatColor.GRAY+"Players: "+Bukkit.getOnlinePlayers().size()+"/"+Bukkit.getMaxPlayers());
        Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj=board.registerNewObjective("smpinfo",Criteria.DUMMY,ChatColor.AQUA+""+ChatColor.BOLD+"PSIROMOUNA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore(ChatColor.DARK_GRAY+" ").setScore(7);
        obj.getScore(ChatColor.GRAY+"Players").setScore(6);
        obj.getScore(ChatColor.WHITE+""+Bukkit.getOnlinePlayers().size()+"/"+Bukkit.getMaxPlayers()).setScore(5);
        obj.getScore(ChatColor.DARK_GRAY+"  ").setScore(4);
        obj.getScore(ChatColor.GOLD+"€ "+money(lefta)).setScore(3);
        obj.getScore(ChatColor.AQUA+"✦ "+money(shards)+" Shards").setScore(2);
        obj.getScore(ChatColor.DARK_GRAY+"psiromouna SMP").setScore(1);
        p.setScoreboard(board);
    }

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        String c=command.getName().toLowerCase(Locale.ROOT);
        if(args.length==1) {
            List<String> opts=switch(c) {
                case "worth" -> List.of("search","hand");
                case "sell" -> List.of("hand","all");
                case "ah" -> List.of("sell","buy","cancel","help");
                case "currency" -> List.of("give");
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
