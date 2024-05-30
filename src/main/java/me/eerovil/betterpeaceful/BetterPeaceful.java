package me.eerovil.betterpeaceful;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.damage.DamageSource;
// import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.world.entity.EntityInsentient;

import org.bukkit.inventory.ItemStack;
// import net.minecraft.world.entity.EntityInsentient;
// import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
// import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.material.Colorable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BetterPeaceful extends JavaPlugin implements Listener {

    private final Map<UUID, Long> interactCooldown = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Better Peaceful Mod Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Better Peaceful Mod Disabled!");
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType entityType = event.getEntityType();
        Entity entity = event.getEntity();

        if (entity instanceof Monster) {
            long mobCount = Bukkit.getWorlds().stream()
                    .flatMap(world -> world.getEntitiesByClass(Monster.class).stream())
                    .filter(monster -> monster.getType() == entityType)
                    .count();

            if (mobCount >= 1) {
                event.setCancelled(true);
                return;
            }

            // Change AI to passive
            // makePassive((Monster) entity);
        }
    }

    // private void makePassive(Monster monster) {
    //     EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) monster).getHandle();
    //     nmsEntity.bP = new PathfinderGoalSelector(nmsEntity.D);
    //     nmsEntity.bQ = new PathfinderGoalSelector(nmsEntity.D);

    //     // Add a new "wander" goal
    //     nmsEntity.bP.a(0, new PathfinderGoalRandomStrollLand(nmsEntity, 1.0));
    // }

    public static void showHearts(Entity entity) {
        Location location = entity.getLocation();
        entity.getWorld().spawnParticle(Particle.HEART, location.getX(), location.getY() + 1, location.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Remove all damage events
        event.setCancelled(true);
        
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)){
            return;
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
                .luck(1)
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


        // Don't use livingEntity.getLootTable() because it's not available in 1.16

        interactCooldown.put(entityId, currentTime);
    }
}
