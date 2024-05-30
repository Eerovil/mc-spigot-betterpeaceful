package me.eerovil.betterpeaceful;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
// import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
// import net.minecraft.world.entity.EntityInsentient;
// import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
// import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.loot.LootContext;

import java.util.HashMap;
import java.util.Map;
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

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
    }

    // private fun LivingEntity.randomLoot(player: ServerPlayer): ItemStack? {
    //     val lootContext = LootContext.Builder(player.getLevel())
    //         .withRandom(java.util.Random())
    //         .withParameter(LootContextParams.THIS_ENTITY, this)
    //         .withParameter(LootContextParams.ORIGIN, position())
    //         .withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.GENERIC)
    //         .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
    //         .withOptionalParameter(LootContextParams.KILLER_ENTITY, player)
    //         .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, player)
    //         .create(LootContextParamSets.ENTITY)

    //     return player.server.lootTables[lootTable].getRandomItems(lootContext).randomOrNull()
    // }

    // private void randomLoot(LivingEntity livingEntity) {
    //     livingEntity.getLooyt
    // }


    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof LivingEntity)) return;

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
        event.getPlayer().sendMessage("You have interacted with " + livingEntity.getName());

        // Drop the items the entity would drop on death
        // for (ItemStack drop : livingEntity.getLootTable().populateLoot(new org.bukkit.loot.LootContext.Builder(livingEntity.getLocation()).build())) {
        //     livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), drop);
        // }

        interactCooldown.put(entityId, currentTime);
    }
}
