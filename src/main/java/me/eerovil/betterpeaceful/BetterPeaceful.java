package me.eerovil.betterpeaceful;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
// import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.MobGoals;

import org.bukkit.inventory.ItemStack;

import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.material.Colorable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class BetterPeaceful extends JavaPlugin implements Listener {

    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private final Map<UUID, Integer> teleportList = new HashMap<>();

    // Add handler when compass is used
    @EventHandler
    public void onPlayerUse(org.bukkit.event.player.PlayerInteractEvent event) {
        // If compass is used
        Player p = event.getPlayer();
        getLogger().info("Player used item: " + p.getInventory().getItemInMainHand().getType());
        if(p.getInventory() != null && p.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            // Tp to current teleportList index
            Integer index = teleportList.getOrDefault(p.getUniqueId(), 0);
            Map<Integer, Location> list = getTeleportList(p);
            if (index >= list.size()) {
                index = 0;
            }
            Location loc = list.get(index);
            loc.setPitch(p.getLocation().getPitch());
            loc.setYaw(p.getLocation().getYaw());
            p.teleport(loc);
            getLogger().info("Teleporting player to " + loc);
            // Increment index
            index++;
            if (index >= list.size()) {
                index = 0;
            }
            teleportList.put(p.getUniqueId(), index);
        }
    }

    public Map<Integer, Location> getTeleportList(Player player) {
        // Return a collection where 0 is home, 1 is player 1 location, 2 is player 2 location, etc.
        // Skip player that is given as arg
        Map<Integer, Location> list = new HashMap<>();

        Location home = player.getRespawnLocation();
        if (home == null) {
            home = player.getWorld().getSpawnLocation();
        }
        list.put(0, home);
        Integer index = 1;

        // Loop all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == player) {
                continue;
            }
            list.put(index, p.getLocation());
            index++;
        }

        // Loop all mobs
        // for (Entity entity : player.getWorld().getEntities()) {
        //     if (entity instanceof Player) {
        //         continue;
        //     }
        //     list.put(index, entity.getLocation());
        //     index++;
        // }

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

        // If this is ender dragon, kill it after spawning
        if (entityType == EntityType.ENDER_DRAGON) {
            // Wait for 10 tick before killing the dragon
            Bukkit.getScheduler().runTaskLater(this, () -> killEntityUsingDamage((LivingEntity) entity), 10);
        }

        makePassive((LivingEntity) entity);
    }

    public void makePassive(LivingEntity monster) {
        Boolean isMonster = monster instanceof Monster;
        isMonster = isMonster || monster instanceof Shulker;
        if (!isMonster) {
            return;
        }
        getLogger().info("Making " + monster.getName() + " id " + monster.getUniqueId() + " passive");
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
        if (entity instanceof Monster) {
            getLogger().info("EntityTargetEvent: " + event.getEntity().getName() + " targetting " + event.getTarget());
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
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        
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
                .lootingModifier(1)
                .luck(100)
                .build();

        Random random = new Random();
        // print the table
        Collection<ItemStack> loot = lootTable.populateLoot(random, lootContext);
        for (ItemStack item : loot) {
            player.sendMessage("You got " + item.getAmount() + " " + item.getType());
            // Drop the item to the ground
            livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), item);
        }
        // If the entity was a sheep, drop wool according to its color
        if (livingEntity instanceof Sheep) {
            Colorable colorable = (Colorable) livingEntity;
            Material wool = Material.valueOf(colorable.getColor().name() + "_WOOL");
            ItemStack woolStack = new ItemStack(wool, 1);
            player.sendMessage("You got " + woolStack.getAmount() + " " + woolStack.getType());
            livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), woolStack);
        }

        // Show hearts on the entity like when breeding
        showHearts(livingEntity);

        // Look at player
        ((Mob) livingEntity).lookAt(player);

        // Don't use livingEntity.getLootTable() because it's not available in 1.16

        interactCooldown.put(entityId, currentTime);
    }
}
