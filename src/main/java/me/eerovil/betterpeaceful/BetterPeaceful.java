package me.eerovil.betterpeaceful;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.inject.Named;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
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
import org.bukkit.event.entity.EntityTeleportEvent;
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

    private final 
    EntityType[] uniqueEntityTypes = {
        EntityType.ALLAY,
        EntityType.AXOLOTL,
        EntityType.BAT,
        EntityType.BEE,
        EntityType.BLAZE,
        EntityType.BOAT,
        EntityType.CAMEL,
        EntityType.CAT,
        EntityType.CHICKEN,
        EntityType.COW,
        EntityType.CREEPER,
        EntityType.DOLPHIN,
        EntityType.DONKEY,
        EntityType.DROWNED,
        EntityType.ELDER_GUARDIAN,
        EntityType.ENDERMAN,
        EntityType.ENDERMITE,
        EntityType.EVOKER,
        EntityType.FOX,
        EntityType.FROG,
        EntityType.GOAT,
        EntityType.HORSE,
        EntityType.HUSK,
        EntityType.ILLUSIONER,
        EntityType.IRON_GOLEM,
        EntityType.LLAMA,
        EntityType.MAGMA_CUBE,
        EntityType.MINECART,
        EntityType.MUSHROOM_COW,
        EntityType.OCELOT,
        EntityType.PANDA,
        EntityType.PARROT,
        EntityType.PHANTOM,
        EntityType.PIG,
        EntityType.PILLAGER,
        EntityType.PUFFERFISH,
        EntityType.RABBIT,
        EntityType.RAVAGER,
        EntityType.SALMON,
        EntityType.SHEEP,
        EntityType.SHULKER,
        EntityType.SKELETON,
        EntityType.SLIME,
        EntityType.SNIFFER,
        EntityType.SNOWMAN,
        EntityType.SPIDER,
        EntityType.SQUID,
        EntityType.STRAY,
        EntityType.STRIDER,
        EntityType.TURTLE,
        EntityType.VILLAGER,
        EntityType.WANDERING_TRADER,
        EntityType.WITCH,
        EntityType.WITHER_SKELETON,
        EntityType.WOLF,
        EntityType.ZOGLIN,
        EntityType.ZOMBIE,
        EntityType.ENDER_DRAGON,
        EntityType.GHAST,
    };

    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private final Map<UUID, Long> teleportCooldown = new HashMap<>();
    private final Map<UUID, Integer> teleportList = new HashMap<>();

    private final ArrayList<Location> randomTeleportTargetList = new ArrayList<>();
    private Integer randomTeleportListIndex = 0;

    // Prevent other entities from using teleport

    // Log message when entity despawn


    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        // Example: Only allow players to teleport
        if (event.getEntityType() != EntityType.PLAYER) {
            event.setCancelled(true);
        }
        // Also don't allow villager
        if (event.getEntityType() == EntityType.VILLAGER) {
            event.setCancelled(true);
        }
    }

    // Add handler when player teleports using a nether portal
    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        // If the teleport is not a nether portal, skip
        Player p = event.getPlayer();
        // Log the teleport event
        String toName = event.getTo().getWorld().getName();
        getLogger().info("Player " + p.getName() + " teleported to " + event.getTo() + " " + toName);
        if (!toName.equals("flat_nether")) {
            // If the player is not going to the nether, skip
            getLogger().info("Player " + p.getName() + " is not going to the nether, he is going to " + toName);
            return;
        }
        // Cancel the event and instead teleport the player to a random location 2 blocks away
        // from the original location
        // Find random X and Y that are not in teleportListIndex
        Integer index = randomTeleportListIndex;
        if (index >= randomTeleportTargetList.size()) {
            index = 0;
        }
        Location loc = randomTeleportTargetList.get(index).clone();
        // Increment index
        randomTeleportListIndex++;

        // Find the ground Y, that is the same as the player's Y
        loc.setY(event.getFrom().getY());
        loc.setZ(loc.getZ() + 2);
        loc.setPitch(0);
        loc.setYaw(0);
        loc.setWorld(event.getFrom().getWorld());

        // Send a message to player
        // p.sendMessage("Teleporting to " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());

        // Teleport to it after 5 seconds
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) {
                p.teleport(loc);
            }
        }, 100L);

        event.setCancelled(true);
    }

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
        // Loop through uniqueEntityTypes and make sure each exist in the world
        for (EntityType entityType : uniqueEntityTypes) {
            if (Bukkit.getWorld("flat").getEntitiesByClass(entityType.getEntityClass()).size() == 0) {
                sender.sendMessage("Entity " + entityType + " does not exist in the world!");
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        for (World world : Bukkit.getWorlds()) {
            getLogger().info("World: " + world.getName() + ", " + world);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setTime(1000); // 1000 = morning
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        // Create a timer that runs every 1 second
        // Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
        //     // Loop all entities in all worlds
        //     for (org.bukkit.World world : Bukkit.getWorlds()) {
        //         for (Entity entity : world.getEntities()) {
        //             if (entity instanceof LivingEntity) {
        //                 LivingEntity livingEntity = (LivingEntity) entity;
        //                 // If entity is an ender dragon or wither, cause damage to it
        //                 if (livingEntity instanceof org.bukkit.entity.EnderDragon || livingEntity instanceof org.bukkit.entity.Wither) {
        //                     if (livingEntity.getHealth() <= 10) {
        //                         livingEntity.setHealth(0);
        //                     } else if (livingEntity.getHealth() <= 100) {
        //                         livingEntity.setHealth(livingEntity.getHealth() - 10);
        //                     } else if (livingEntity.getHealth() <= 170) {
        //                         livingEntity.setHealth(livingEntity.getHealth() - 5);
        //                     } else {
        //                         livingEntity.setHealth(livingEntity.getHealth() - 1);
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }, 0L, 20L);


        // Populate randomTeleportTargetList with 90 random locations
        // they are just x, y with random values for x: 0-9 and for y: 0-8
        // each combination must be in the list only once and exactly once
        for (int x=0; x < 10; x++) {
            for (int y=0; y < 8; y++) {
                // y is actually z
                randomTeleportTargetList.add(new Location(Bukkit.getWorld("flat"), x * 10, -60, y * 10));
            }
        }
        // Pop until there are only 57
        while (randomTeleportTargetList.size() > 60) {
            randomTeleportTargetList.remove(randomTeleportTargetList.size() - 1);
        }
        // Add 5 more locations
        randomTeleportTargetList.add(new Location(Bukkit.getWorld("flat"), 1 * 10, -60, 9 * 10));
        randomTeleportTargetList.add(new Location(Bukkit.getWorld("flat"), 3 * 10, -60, 9 * 10));

        clearWorld();

        EntityType[] flyingEntityTypes = {
            EntityType.BAT,
            EntityType.BEE,
            EntityType.BLAZE,
            EntityType.CAVE_SPIDER,
            EntityType.CHICKEN,
            EntityType.DOLPHIN,
            EntityType.ENDERMITE,
            EntityType.GHAST,
            EntityType.GLOW_SQUID,
            EntityType.HOGLIN,
            EntityType.PHANTOM,
            EntityType.SILVERFISH,
            EntityType.SKELETON_HORSE,
            EntityType.SLIME,
            EntityType.SPIDER,
            EntityType.WITCH,
            EntityType.VEX,
        };

        EntityType[] fishEntityTypes = {
            EntityType.COD,
            EntityType.DOLPHIN,
            EntityType.GLOW_SQUID,
            EntityType.PUFFERFISH,
            EntityType.SALMON,
            EntityType.SQUID,
            EntityType.TADPOLE,
            EntityType.TROPICAL_FISH,
        };
        
        for (Entity entity : Bukkit.getWorld("flat").getEntities()) {
            entity.remove();
        }

        // Loop all entity types and add them to each location
        for (int index=0; index < uniqueEntityTypes.length; index++) {
            EntityType entityType = uniqueEntityTypes[index];

            try {
                // Get the location from the list
                Location loc = randomTeleportTargetList.get(index).clone();
                getLogger().log(Level.INFO, "Spawning entity: {0} at index {1}", new Object[]{entityType, index});
                loc.setZ(loc.getZ() + 3);
                // Ender dragon goes 10 blockmore Z and 3 blocks y
                if (entityType == EntityType.ENDER_DRAGON) {
                    loc.setZ(loc.getZ() + 10);
                    loc.setY(loc.getY() + 3);
                }
                // Spawn the entity at the location
                Entity entity = loc.getWorld().spawnEntity(loc, entityType);
                // Add name tag
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setCustomNameVisible(true);
                    ((LivingEntity) entity).setCustomName(entityType.toString());
                    ((LivingEntity) entity).setRemoveWhenFarAway(false);
                }
                // Set the entity to be passive
                if (entity instanceof LivingEntity) {
                    makePassive((LivingEntity) entity);
                }
                // Make the entity silent
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    mob.setSilent(true);
                }
                boolean isFlying = false;
                for (EntityType flyingEntityType : flyingEntityTypes) {
                    if (entityType == flyingEntityType) {
                        isFlying = true;
                        break;
                    }
                }
                boolean isFish = false;
                for (EntityType fishEntityType : fishEntityTypes) {
                    if (entityType == fishEntityType) {
                        isFish = true;
                        break;
                    }
                }
                // If the entity is a fish, replace some floor with water (3x3)
                if (isFish) {
                    Location waterLoc = loc.clone();
                    waterLoc.setY(waterLoc.getY() - 1);
                    loc.getWorld().getBlockAt(waterLoc).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, 0)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, 0)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, 1)).setType(Material.WATER);
                    waterLoc = loc.clone();
                    waterLoc.setY(waterLoc.getY() - 2);
                    loc.getWorld().getBlockAt(waterLoc).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, 0)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, 0)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(1, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(-1, 0, 1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, -1)).setType(Material.WATER);
                    loc.getWorld().getBlockAt(waterLoc.clone().add(0, 0, 1)).setType(Material.WATER);
                }
                generatePortalAt(randomTeleportTargetList.get(index));
                
            } catch (Exception e) {
                getLogger().log(Level.INFO, "Failed to spawn entity: {0}", entityType);
                // Log exception
                getLogger().log(Level.SEVERE, "Exception: ", e);
            }
        }

        // Shuffle the list
        Random random = new Random();
        for (int i = randomTeleportTargetList.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Location temp = randomTeleportTargetList.get(i);
            randomTeleportTargetList.set(i, randomTeleportTargetList.get(j));
            randomTeleportTargetList.set(j, temp);
        }

        Integer entityCount = 0;
        for (Entity entity : Bukkit.getWorld("flat").getEntities()) {
            entityCount++;
        }
        getLogger().info("Spawned " + entityCount + " entities");
    }

    private void clearWorld() {
        getLogger().info("Clearing world");
        // Delete all entities
        for (Entity entity : Bukkit.getWorld("flat").getEntities()) {
            if (entity instanceof Creature) {
                entity.remove();
            }
        }
        // Clear the area within 0, -65, 0, 100, -50, 100
        // Everything is air except for Y -60
        World world = Bukkit.getWorld("flat");
        for (int x = -10; x < 150; x++) {
            for (int z = -10; z < 150; z++) {
                for (int y = -70; y < -60; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.STONE) {
                        block.setType(Material.STONE);
                    }
                }
                for (int y = -60; y < 0; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                    }
                }
            }
            getLogger().info("Cleared " + x + " blocks");
        }
    }

    private void generatePortalAt(Location baseLoc) {
        getLogger().info("Generating portal at " + baseLoc);
        // Build a 4x5 classic nether portal frame, standing upright on X axis
        World world = baseLoc.getWorld();
        int x = baseLoc.getBlockX() - 1;
        int y = baseLoc.getBlockY() - 1;
        int z = baseLoc.getBlockZ();
    
        // Obsidian frame (3 wide, 4 tall inside, 5 tall total)
        for (int i = 0; i < 5; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.OBSIDIAN);         // left side
            world.getBlockAt(x + 2, y + i, z).setType(Material.OBSIDIAN);     // right side
        }
        for (int i = 0; i < 3; i++) {
            world.getBlockAt(x + i, y, z).setType(Material.OBSIDIAN);         // bottom
            world.getBlockAt(x + i, y + 4, z).setType(Material.OBSIDIAN);     // top
        }
    
        // Portal blocks inside
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                world.getBlockAt(x + 1, y + i, z).setType(Material.NETHER_PORTAL);
            }
        }

        // next we make a house around the portal
        // Back wall is z - 1
        // right wall is x + 3
        // left wall is x - 3
        // front wall is z - 6
        // floor is y - 1
        // no roof

        // Make it all from wood now
        int yy;
        int xx;
        int zz;
        int WIDTH = 10;
        int LENGTH = 10;
        int HEIGHT = 5;
        boolean roof = true;

        if (z == 90) {
            WIDTH = 20;
            LENGTH = 20;
            HEIGHT = 20;
        }

        int x_start = x - (WIDTH / 2);
        // Backwall
        for (yy=y-1; yy<y+HEIGHT; yy++) {
            // Create a row of blocks
            // z is always z - 1 here
            zz = z - 1;
            xx = x_start;
            for (int i=0; i<WIDTH; i++) {
                world.getBlockAt(xx++, yy, zz).setType(Material.OAK_WOOD);
            }
        }

        // Right wall
        for (yy=y-1; yy<y+HEIGHT; yy++) {
            // Create a row of blocks
            // x is always x + 3 here
            zz = z - 1;
            xx = x_start + WIDTH;
            for (int i=0; i<LENGTH; i++) {
                world.getBlockAt(xx, yy, zz++).setType(Material.OAK_WOOD);
            }
        }

        // Left wall
        for (yy=y-1; yy<y+HEIGHT; yy++) {
            // Create a row of blocks
            // x is always x_start here
            zz = z - 1;
            xx = x_start;
            for (int i=0; i<LENGTH; i++) {
                world.getBlockAt(xx, yy, zz++).setType(Material.OAK_WOOD);
            }
        }

        // Front wall
        for (yy=y-1; yy<y+HEIGHT; yy++) {
            // Create a row of blocks
            // z is always z + 5 here
            zz = z - 1 + LENGTH;
            xx = x_start;
            for (int i=0; i<WIDTH; i++) {
                world.getBlockAt(xx++, yy, zz).setType(Material.OAK_WOOD);
            }
        }

        // Roof
        if (roof) {
            for (int i=0; i<WIDTH; i++) {
                for (int j=0; j<LENGTH; j++) {
                    world.getBlockAt(x_start + i, y + HEIGHT, z - 1 + j).setType(Material.STONE);
                }
            }
            // Add torch
            world.getBlockAt(x_start + (WIDTH / 2), y + 1, z - 1 + (LENGTH / 2)).setType(Material.TORCH);
        }

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
