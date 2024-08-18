package me.eerovil.betterpeaceful;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Frog.Variant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.material.Colorable;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.MobGoals;

public class BetterPeaceful extends JavaPlugin implements Listener {

    private class TeleportTarget {
        public Location location;
        public String name;

        public TeleportTarget(Location location, String name) {
            this.location = location;
            this.name = name;
        }
    }

    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private final Map<UUID, Long> teleportCooldown = new HashMap<>();
    private final Map<UUID, Integer> teleportList = new HashMap<>();

    // Add handler when compass is used
    @EventHandler
    public void onPlayerUse(org.bukkit.event.player.PlayerInteractEvent event) {
        // If compass is used
        if (!event.getAction().isRightClick()) {
            return;
        }
        Block block = event.getClickedBlock();
        // If target is a chest or other interactable block, skip
        if (block != null && block.getType().isInteractable()) {
            return;
        }
        Player p = event.getPlayer();

        // If player is holding a blaze rod, delete the entity they are looking at
        if (p.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
            Entity target = p.getTargetEntity(10);
            if (target != null) {
                target.remove();
            } else {
                // Find nearest water or lava source block in line of sight and remove it
                List<Block> blocks = p.getLineOfSight((Set<Material>)null, 10);
                for (Block b : blocks) {
                    if (b.getType() == Material.WATER || b.getType() == Material.LAVA) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }

        // Check cooldown
        if (teleportCooldown.containsKey(p.getUniqueId())) {
            long lastTeleportTime = teleportCooldown.get(p.getUniqueId());
            if (System.currentTimeMillis() - lastTeleportTime < 1000) {
                return;
            }
            if (System.currentTimeMillis() - lastTeleportTime > 10000) {
                // After 10 seconds, reset the teleportList index
                teleportList.put(p.getUniqueId(), 0);
            }
        }
        teleportCooldown.put(p.getUniqueId(), System.currentTimeMillis());
        if(p.getInventory() != null && p.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            // Tp to current teleportList index
            Integer index = teleportList.getOrDefault(p.getUniqueId(), 0);
            Map<Integer, TeleportTarget> list = getTeleportList(p);
            if (index >= list.size()) {
                index = 0;
            }
            TeleportTarget target = list.get(index);
            Location loc = target.location;
            loc.setPitch(p.getLocation().getPitch());
            loc.setYaw(p.getLocation().getYaw());
            p.teleport(loc);
            p.sendMessage("Teleporting to " + target.name);
            // Increment index
            index++;
            if (index >= list.size()) {
                index = 0;
            }
            teleportList.put(p.getUniqueId(), index);
        } else if (p.getInventory() != null && p.getInventory().getItemInMainHand().getType() == Material.CLOCK) {
            // If clock is used, toggle player between creative and survival
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.sendMessage("Switched to survival mode");
            } else {
                p.setGameMode(org.bukkit.GameMode.CREATIVE);
                p.sendMessage("Switched to creative mode");
            }
        }
    }

    private void deletePossibleWater(Block block, BlockFace face) {
        Location loc = block.getLocation();
        // Find the block that is next to the block "block" when going towards face "face"
        loc.add(face.getModX(), face.getModY(), face.getModZ());
        Block targetBlock = loc.getBlock();
        // If target block is water or lava, remove it
        if (targetBlock.getType() == Material.WATER || targetBlock.getType() == Material.LAVA) {
            targetBlock.setType(Material.AIR);
        }
    }

    private boolean isSafeLocation(Location location) {
        if (location.getBlock().isEmpty()) {
            return true;
        }
        // Water is also ok
        if (location.getBlock().getType() == Material.WATER) {
            return true;
        }
        return false;
    }

    public Map<Integer, TeleportTarget> getTeleportList(Player player) {
        // Return a collection where 0 is home, 1 is player 1 location, 2 is player 2 location, etc.
        // Skip player that is given as arg
        Map<Integer, TeleportTarget> list = new HashMap<>();

        Location home = player.getRespawnLocation();
        if (home == null) {
            home = player.getWorld().getSpawnLocation();
        }
        list.put(0, new TeleportTarget(home, "Home"));
        Integer index = 1;

        // Loop all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == player) {
                continue;
            }
            Location playerLocation = p.getLocation();
            // playerLocation.setX(200.5);
            // playerLocation.setY(200.5);
            // playerLocation.setZ(200.5);
            Collection<Location> availableLocations = List.of(
                playerLocation.clone().add(1, 0, 0),
                playerLocation.clone().add(1, 0, 1),
                playerLocation.clone().add(0, 0, 1),
                playerLocation.clone().add(-1, 0, 1),
                playerLocation.clone().add(-1, 0, 0),
                playerLocation.clone().add(-1, 0, -1),
                playerLocation.clone().add(0, 0, -1),
                playerLocation.clone().add(1, 0, -1),
                playerLocation
            );

            // Loop all available locations until we find a good one
            Location goodLocation = null;
            for (Location loc : availableLocations) {
                // If player is in the air, reduce y until there is a block
                Integer counter = 0;
                while (loc.getBlock().isEmpty()) {
                    loc.setY(loc.getY() - 1);
                    counter++;
                    if (counter > 500) {
                        break;
                    }
                }
                if (loc.getBlock().isEmpty()) {
                    continue;
                }
                // If the block is lava, skip
                if (loc.getBlock().getType() == Material.LAVA) {
                    continue;
                }
                // If the block above is not empty, skip
                if (!isSafeLocation(loc.clone().add(0, 1, 0))) {
                    continue;
                }
                // also block above that
                if (!isSafeLocation(loc.clone().add(0, 2, 0))) {
                    continue;
                }
                goodLocation = loc;
                break;
            }
            if (goodLocation == null) {
                continue;
            }

            // All good. Add 1 to y and store the location
            goodLocation.setY(goodLocation.getY() + 1);
            list.put(index, new TeleportTarget(goodLocation, p.getName()));
            index++;
        }

        return list;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (player == null) {
            sender.sendMessage("Command can only be used by a player!");
            return true;
        }
        sender.sendMessage(label + " command executed!");
        // do stuff
        // Despawn all entities
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof Creature) {
                sender.sendMessage("Despawning " + entity.getName());
                entity.remove();
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Better Peaceful Mod Enabled!");
    }

    @Override
    public void onLoad() {
        getLogger().info("Better Peaceful Mod Loadeddd!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Better Peaceful Mod Disabled!");
    }

    public void killEntityUsingDamage(LivingEntity entity) {
        // Kill the entity using damage
        getLogger().info("Killing " + entity.getName());
        if (entity.isDead()) {
            getLogger().info("Entity is already dead");
            return;
        }
        entity.setHealth(0);
        // Loop this until the entity is dead
        if (!entity.isDead()) {
            Bukkit.getScheduler().runTaskLater(this, () -> killEntityUsingDamage(entity), 10);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType entityType = event.getEntityType();
        Entity entity = event.getEntity();

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && entity instanceof Frog) {
            // If the entity has variants, set a random variant
            try {
                Frog frog = (Frog) entity;
                Variant newVariant = Frog.Variant.values()[new Random().nextInt(Frog.Variant.values().length)];
                frog.setVariant(newVariant);
            } catch (Exception e) {
                getLogger().log(Level.INFO, "Failed to set variant for entity: {0}", entity.getName());
            }
        }

        // // If this is ender dragon, kill it after spawning
        // if (entityType == EntityType.ENDER_DRAGON) {
        //     // Wait for 10 tick before killing the dragon
        //     Bukkit.getScheduler().runTaskLater(this, () -> killEntityUsingDamage((LivingEntity) entity), 10);
        // }

        makePassive((LivingEntity) entity);
    }

    public void makePassive(LivingEntity monster) {
        Boolean isMonster = monster instanceof Monster;
        isMonster = isMonster || monster instanceof Shulker || monster instanceof Warden;
        if (!isMonster) {
            return;
        }
        MobGoals goals = Bukkit.getMobGoals();
        Mob mob = (Mob) monster;
        mob.setSilent(true);
        Collection<Goal<Mob>> monsterGoals = goals.getAllGoals(mob);

        monsterGoals.forEach(wrappedGoal -> {
            GoalKey<Mob> key = wrappedGoal.getKey();
            // Only remove keys that have the string attack in key
            List<String> badKeys = List.of("attack", "leap_at");
            // Remove all creeper-specific goals
            List<String> badClasses = List.of("creeper");

            Boolean isBad = badClasses.stream().anyMatch(key.getNamespacedKey().getClass().toString()::contains);
            isBad = isBad || badKeys.stream().anyMatch(key.getNamespacedKey().getKey()::contains);

            if (isBad) {
                goals.removeGoal(mob, key);
            } else {
                // getLogger().info("Keeping goal: " + key.getNamespacedKey().getKey());
            }
        });
    }

    @EventHandler
    public void EntityTargetEvent(org.bukkit.event.entity.EntityTargetEvent event) {
        Entity entity = event.getEntity();
        Entity target = event.getTarget();
        if (entity instanceof LivingEntity || target instanceof LivingEntity) {
            event.setCancelled(true);
            // Mob mob = (Mob) entity;
            // Disable mob ai for 1 seconds
            // mob.setAI(false);
            // Bukkit.getScheduler().runTaskLater(this, () -> mob.setAI(true), 20);
        }
    }

    public static void showHearts(Entity entity) {
        Location location = entity.getLocation();
        entity.getWorld().spawnParticle(Particle.HEART, location.getX(), location.getY() + 1, location.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent baseEvent) {
        Collection<DamageCause> badCauses = List.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.SUFFOCATION);
        if (baseEvent.getCause() == DamageCause.FIRE_TICK) {
            // If the taget is a monster and it's daytime, we despawn it
            if (baseEvent.getEntity() instanceof Monster) {
                Monster monster = (Monster) baseEvent.getEntity();
                if (monster.getWorld().getTime() < 12300 || monster.getWorld().getTime() > 23850) {
                    monster.remove();
                }
            }
        }
        if (badCauses.contains(baseEvent.getCause())) {
            baseEvent.setCancelled(true);
            return;
        }
        if (!(baseEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }

        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) baseEvent;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)){
            return;
        } else {
            // Remove all damage events
            event.setCancelled(true);
        }

        Entity damagingEntity = event.getDamager();
        // If damager is not a player, cancel the event
        if (damagingEntity.getType() != EntityType.PLAYER) {
            // Print log
            return;
        }

        // Cast damaging entity to a player
        Player player = (Player) damagingEntity;

        LivingEntity livingEntity = (LivingEntity) entity;

        makePassive(livingEntity);

        UUID entityId = livingEntity.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (interactCooldown.containsKey(entityId)) {
            long lastInteractTime = interactCooldown.get(entityId);
            if (currentTime - lastInteractTime < 12000) { // 12000 ms = 5 clicks per minute
                return;
            }
        }

        // Print a message to the player
        // player.sendMessage("You have interacted with " + livingEntity.getName());

        NamespacedKey key = new NamespacedKey("minecraft", "entities/" + livingEntity.getType().getKey().getKey());

        LootTable lootTable = Bukkit.getLootTable(key);
        if (lootTable == null) {
            getLogger().info("Loot table not found: " + key);
            return;
        }

        LootContext lootContext = new LootContext.Builder(livingEntity.getLocation())
                .lootedEntity(livingEntity)
                .killer(player)
                .lootingModifier(2)
                .luck(100)
                .build();

        Random random = new Random();
        // Try 5 times to get loot
        for (int i = 0; i < 5; i++) {
            Collection<ItemStack> loot = lootTable.populateLoot(random, lootContext);
            if (loot.size() > 0) {
                for (ItemStack item : loot) {
                    // Drop the item to the ground
                    livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), item);
                }
                break;
            }
        }
        // If the entity was a sheep, drop wool according to its color
        if (livingEntity instanceof Sheep) {
            Colorable colorable = (Colorable) livingEntity;
            Material wool = Material.valueOf(colorable.getColor().name() + "_WOOL");
            ItemStack woolStack = new ItemStack(wool, 1);
            livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), woolStack);
        }

        // Show hearts on the entity like when breeding
        showHearts(livingEntity);

        // Look at player
        ((Mob) livingEntity).lookAt(player);

        // Don't use livingEntity.getLootTable() because it's not available in 1.16

        interactCooldown.put(entityId, currentTime);

        // If player is holding a blaze rod, delete the entity
        if (player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
            livingEntity.remove();
        }
    }

    // Don't allow breaking blocks when holding a blaze rod
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
            event.setCancelled(true);
            Block block = event.getBlock();
            BlockFace face = BlockFace.UP;  // Default face: cannot get a face from event
            deletePossibleWater(block, face);
        }
    }
}
