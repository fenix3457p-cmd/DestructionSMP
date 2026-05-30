package org.fenix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class destructionSMP extends JavaPlugin implements Listener, CommandExecutor {

    private NamespacedKey maceKey, shardKey, excaliburKey, witherBoneKey, spearKey;
    private NamespacedKey godBootsKey, godLegs1Key, godLegs2Key, godChest1Key, godChest2Key, godHelmetKey, wardenHeartKey;

    @Override
    public void onEnable() {
        // Keys
        maceKey = new NamespacedKey(this, "op_mace");
        shardKey = new NamespacedKey(this, "empyrium_shard");
        excaliburKey = new NamespacedKey(this, "excalibur");
        witherBoneKey = new NamespacedKey(this, "wither_bone");
        spearKey = new NamespacedKey(this, "spear");
        godBootsKey = new NamespacedKey(this, "god_boots");
        godLegs1Key = new NamespacedKey(this, "god_legs_1");
        godLegs2Key = new NamespacedKey(this, "god_legs_2");
        godChest1Key = new NamespacedKey(this, "god_chest_1");
        godChest2Key = new NamespacedKey(this, "god_chest_2");
        godHelmetKey = new NamespacedKey(this, "god_helmet");
        wardenHeartKey = new NamespacedKey(this, "warden_heart");

        // Config Limits
        this.getConfig().addDefault("mace_crafted", 0);
        this.getConfig().addDefault("excalibur_crafted", 0);
        this.getConfig().addDefault("boots_crafted", 0);
        this.getConfig().addDefault("leggings_crafted", 0);
        this.getConfig().addDefault("chestplate_crafted", 0);
        this.getConfig().addDefault("helmet_crafted", 0);
        this.getConfig().options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);

        // Commands
        if (this.getCommand("givemace") != null) this.getCommand("givemace").setExecutor(this);
        if (this.getCommand("giveexcalibur") != null) this.getCommand("giveexcalibur").setExecutor(this);
        if (this.getCommand("givespear") != null) this.getCommand("givespear").setExecutor(this);
        if (this.getCommand("givegodarmor") != null) this.getCommand("givegodarmor").setExecutor(this);

        registerRecipes();
        startArmorTask();

        getLogger().info("DestructionSMP Plugin enabled!");
    }

    // --- REPEATING TASK FOR PASSIVE ARMOR EFFECTS ---
    private void startArmorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check Leggings (Speed)
                    ItemStack legs = player.getInventory().getLeggings();
                    if (isCustomItem(legs, godLegs1Key)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
                    } else if (isCustomItem(legs, godLegs2Key)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false, false));
                    }

                    // Check Chestplate (Strength)
                    ItemStack chest = player.getInventory().getChestplate();
                    if (isCustomItem(chest, godChest1Key)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false, false));
                    } else if (isCustomItem(chest, godChest2Key)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 1, true, false, false));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Runs every 1 second (20 ticks)
    }

    // --- CRAFTING LIMIT LOGIC ---
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack item = event.getRecipe().getResult();
        Player player = (Player) event.getWhoClicked();

        if (isCustomItem(item, maceKey)) {
            if (checkLimit("mace_crafted", 1)) {
                cancelCraft(event, "Only one Mace of the Heavens can ever be crafted!");
                return;
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "THE MACE OF THE HEAVENS HAS BEEN FORGED BY " + player.getName() + "!");
        }
        else if (isCustomItem(item, excaliburKey)) {
            if (checkLimit("excalibur_crafted", 5)) { cancelCraft(event, "All 5 Excaliburs have already been forged!"); return; }
        }
        else if (isCustomItem(item, godBootsKey)) {
            if (checkLimit("boots_crafted", 5)) { cancelCraft(event, "All 5 God Boots have already been forged!"); return; }
        }
        else if (isCustomItem(item, godLegs1Key)) {
            if (checkLimit("leggings_crafted", 5)) { cancelCraft(event, "All 5 God Leggings have already been forged!"); return; }
        }
        else if (isCustomItem(item, godChest1Key)) {
            if (checkLimit("chestplate_crafted", 5)) { cancelCraft(event, "All 5 God Chestplates have already been forged!"); return; }
        }
        else if (isCustomItem(item, godHelmetKey)) {
            if (checkLimit("helmet_crafted", 5)) { cancelCraft(event, "All 5 God Helmets have already been forged!"); return; }
        }
    }

    private boolean checkLimit(String configKey, int max) {
        int current = getConfig().getInt(configKey);
        if (current >= max) return true;
        getConfig().set(configKey, current + 1);
        saveConfig();
        return false;
    }

    private void cancelCraft(CraftItemEvent event, String message) {
        event.setCancelled(true);
        event.getWhoClicked().sendMessage(ChatColor.RED + message);
    }

    private void registerRecipes() {
        RecipeChoice.ExactChoice shard = new RecipeChoice.ExactChoice(getEmpyriumShard());
        RecipeChoice.ExactChoice wBone = new RecipeChoice.ExactChoice(getWitherBone());
        RecipeChoice.ExactChoice wHeart = new RecipeChoice.ExactChoice(getWardenHeart());

        // Core Items
        ShapedRecipe shardR = new ShapedRecipe(new NamespacedKey(this, "recipe_shard"), getEmpyriumShard());
        shardR.shape("DDD", "DPD", "DDD");
        shardR.setIngredient('D', Material.DIAMOND);
        shardR.setIngredient('P', Material.PLAYER_HEAD);
        getServer().addRecipe(shardR);

        ShapedRecipe maceR = new ShapedRecipe(new NamespacedKey(this, "recipe_mace"), getOpMace());
        maceR.shape("NNN", "NCN", "SBS");
        maceR.setIngredient('N', Material.NETHERITE_BLOCK);
        maceR.setIngredient('C', Material.HEAVY_CORE);
        maceR.setIngredient('B', Material.BREEZE_ROD);
        maceR.setIngredient('S', shard);
        getServer().addRecipe(maceR);

        ShapedRecipe exR = new ShapedRecipe(new NamespacedKey(this, "recipe_excalibur"), getExcalibur());
        exR.shape("NEN", "NEN", "NWN");
        exR.setIngredient('N', Material.NETHERITE_INGOT);
        exR.setIngredient('E', shard);
        exR.setIngredient('W', wBone);
        getServer().addRecipe(exR);

        ShapedRecipe spearR = new ShapedRecipe(new NamespacedKey(this, "recipe_spear"), getSpear());
        spearR.shape("DED", "DWD", "DWD");
        spearR.setIngredient('D', Material.DIAMOND);
        spearR.setIngredient('E', shard);
        spearR.setIngredient('W', wBone);
        getServer().addRecipe(spearR);

        // --- GOD ARMOR RECIPES ---
        ShapedRecipe bootsR = new ShapedRecipe(new NamespacedKey(this, "recipe_god_boots"), getGodBoots());
        bootsR.shape("S S", "S S");
        bootsR.setIngredient('S', shard);
        getServer().addRecipe(bootsR);

        ShapedRecipe legs1R = new ShapedRecipe(new NamespacedKey(this, "recipe_god_legs1"), getGodLeggings(1));
        legs1R.shape("SSS", "S S", "S S");
        legs1R.setIngredient('S', shard);
        getServer().addRecipe(legs1R);

        ShapelessRecipe legs2R = new ShapelessRecipe(new NamespacedKey(this, "recipe_god_legs2"), getGodLeggings(2));
        legs2R.addIngredient(new RecipeChoice.ExactChoice(getGodLeggings(1)));
        legs2R.addIngredient(new RecipeChoice.ExactChoice(getGodLeggings(1)));
        getServer().addRecipe(legs2R);

        ShapedRecipe chest1R = new ShapedRecipe(new NamespacedKey(this, "recipe_god_chest1"), getGodChestplate(1));
        chest1R.shape("S S", "SSS", "SSS");
        chest1R.setIngredient('S', shard);
        getServer().addRecipe(chest1R);

        ShapelessRecipe chest2R = new ShapelessRecipe(new NamespacedKey(this, "recipe_god_chest2"), getGodChestplate(2));
        chest2R.addIngredient(new RecipeChoice.ExactChoice(getGodChestplate(1)));
        chest2R.addIngredient(new RecipeChoice.ExactChoice(getGodChestplate(1)));
        getServer().addRecipe(chest2R);

        ShapedRecipe helmR = new ShapedRecipe(new NamespacedKey(this, "recipe_god_helm"), getGodHelmet());
        helmR.shape("SSS", "SHS");
        helmR.setIngredient('S', shard);
        helmR.setIngredient('H', wHeart);
        getServer().addRecipe(helmR);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!player.isOp()) return true;

        if (command.getName().equalsIgnoreCase("givemace")) player.getInventory().addItem(getOpMace());
        if (command.getName().equalsIgnoreCase("giveexcalibur")) player.getInventory().addItem(getExcalibur());
        if (command.getName().equalsIgnoreCase("givespear")) player.getInventory().addItem(getSpear());
        if (command.getName().equalsIgnoreCase("givegodarmor")) {
            player.getInventory().addItem(getGodHelmet(), getGodChestplate(2), getGodLeggings(2), getGodBoots(), getWardenHeart());
        }
        return true;
    }

    // --- ITEM BUILDERS ---

    // Core Components
    private ItemStack getEmpyriumShard() {
        return buildItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Empyrium Shard", shardKey);
    }
    private ItemStack getWitherBone() {
        return buildItem(Material.BONE, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Wither Bone", witherBoneKey);
    }
    private ItemStack getWardenHeart() {
        return buildItem(Material.ECHO_SHARD, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden Heart", wardenHeartKey);
    }

    // Weapons
    private ItemStack getSpear() {
        ItemStack item = buildItem(Material.DIAMOND_SPEAR, ChatColor.WHITE + "" + ChatColor.BOLD + "Spear", spearKey);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Grants Saturation when held");
        meta.setLore(lore);
        AttributeModifier speedModifier = new AttributeModifier(new NamespacedKey(this, "spear_attack_speed"), 100.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, speedModifier);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.LUNGE, 3);
        return item;
    }

    private ItemStack getOpMace() {
        ItemStack item = buildItem(Material.MACE, ChatColor.GOLD + "" + ChatColor.BOLD + "Mace of the Heavens", maceKey);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Soulbound - Kept on death");
        lore.add(ChatColor.AQUA + "Right-Click to cast Wind Charge");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.DENSITY, 5);
        item.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
        return item;
    }

    private ItemStack getExcalibur() {
        ItemStack item = buildItem(Material.NETHERITE_SWORD, ChatColor.AQUA + "" + ChatColor.BOLD + "Excalibur", excaliburKey);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.BLUE + "Right-Click: Freeze Entity (30s CD)");
        lore.add(ChatColor.DARK_GREEN + "30% Chance to inflict Poison on hit");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
        item.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
        item.addUnsafeEnchantment(Enchantment.LOOTING, 3);
        return item;
    }

    // God Armor
    private ItemStack getGodBoots() {
        ItemStack item = buildItem(Material.NETHERITE_BOOTS, ChatColor.GOLD + "" + ChatColor.BOLD + "God Boots", godBootsKey);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Negates all fall damage.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        return item;
    }

    private ItemStack getGodLeggings(int level) {
        NamespacedKey key = level == 1 ? godLegs1Key : godLegs2Key;
        ItemStack item = buildItem(Material.NETHERITE_LEGGINGS, ChatColor.GOLD + "" + ChatColor.BOLD + "God Leggings " + (level == 1 ? "I" : "II"), key);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Grants Speed " + level + " permanently.");
        if (level == 1) lore.add(ChatColor.YELLOW + "Combine two in a crafting table to upgrade!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        item.addUnsafeEnchantment(Enchantment.SWIFT_SNEAK, 3);
        return item;
    }

    private ItemStack getGodChestplate(int level) {
        NamespacedKey key = level == 1 ? godChest1Key : godChest2Key;
        ItemStack item = buildItem(Material.NETHERITE_CHESTPLATE, ChatColor.GOLD + "" + ChatColor.BOLD + "God Chestplate " + (level == 1 ? "I" : "II"), key);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Grants Strength " + level + " permanently.");
        if (level == 1) lore.add(ChatColor.YELLOW + "Combine two in a crafting table to upgrade!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        return item;
    }

    private ItemStack getGodHelmet() {
        ItemStack item = buildItem(Material.NETHERITE_HELMET, ChatColor.GOLD + "" + ChatColor.BOLD + "God Helmet", godHelmetKey);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_AQUA + "Shift/Crouch to unleash a Sonic Boom! (60s CD)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        item.addUnsafeEnchantment(Enchantment.RESPIRATION, 3);
        item.addUnsafeEnchantment(Enchantment.AQUA_AFFINITY, 1);
        return item;
    }

    // Helper for building items
    private ItemStack buildItem(Material mat, String name, NamespacedKey key) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCustomItem(ItemStack item, NamespacedKey key) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    // --- EVENTS ---

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (isCustomItem(newItem, spearKey)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, true, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.SATURATION);
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (isCustomItem(player.getInventory().getBoots(), godBootsKey)) {
                event.setCancelled(true); // Negates Fall Damage
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && isCustomItem(player.getInventory().getHelmet(), godHelmetKey)) {
            if (!player.hasCooldown(Material.NETHERITE_HELMET)) {

                player.setCooldown(Material.NETHERITE_HELMET, 1200); // 60 seconds (60 * 20 ticks)
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.0f);

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection();

                // Spawn Sonic Boom Particles
                for (int i = 1; i <= 20; i++) {
                    Location particleLoc = eye.clone().add(dir.clone().multiply(i));
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLoc, 1);
                }

                // Raytrace to find target
                RayTraceResult result = player.getWorld().rayTraceEntities(eye, dir, 20.0, e -> e instanceof LivingEntity && e != player);
                if (result != null && result.getHitEntity() != null) {
                    LivingEntity target = (LivingEntity) result.getHitEntity();
                    target.damage(30.0, player); // Deals 15 full hearts of damage
                    target.setVelocity(dir.multiply(1.5)); // Knockback
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        event.getDrops().add(head);

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (isCustomItem(drop, maceKey)) {
                iterator.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wither) {
            event.getDrops().add(getWitherBone());
        } else if (event.getEntity() instanceof Warden) {
            event.getDrops().add(getWardenHeart());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player player = (Player) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();
            if (isCustomItem(player.getInventory().getItemInMainHand(), excaliburKey)) {
                if (Math.random() <= 0.30) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {

            if (isCustomItem(item, maceKey)) {
                event.setCancelled(true);
                if (!player.hasCooldown(Material.MACE)) {
                    player.launchProjectile(WindCharge.class);
                    player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1.0f, 1.0f);
                    player.setCooldown(Material.MACE, 5);
                }
            }
            else if (isCustomItem(item, excaliburKey)) {
                if (!player.hasCooldown(Material.NETHERITE_SWORD)) {
                    Entity target = player.getTargetEntity(10);
                    if (target instanceof LivingEntity) {
                        LivingEntity livingTarget = (LivingEntity) target;
                        livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 255, false, false));
                        livingTarget.setFreezeTicks(200);
                        player.sendMessage(ChatColor.BLUE + "You froze your target!");
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
                        player.setCooldown(Material.NETHERITE_SWORD, 600);
                    } else {
                        player.sendMessage(ChatColor.RED + "No target found in range!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Excalibur is on cooldown!");
                }
            }
        }
    }
}