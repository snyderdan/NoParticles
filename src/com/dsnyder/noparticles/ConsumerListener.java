package com.dsnyder.noparticles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class that handles the grunt work of the NoParticles plugin.
 * This class listens for player consumption and potion splashes and
 * manages permissions as well as the configuration file.
 * @author Daniel
 * 
 */
public class ConsumerListener implements Listener {
	
	public static final String PERMISSION_NO_PARTICLES 	= "noparticles.noparticles";
	public static final String CONFIG_ENABLED_MOBS 		= "enabledMobs";
	
	/*  All mobs that are supported by NoParticles
	 *********************************************************************************************/
	private final List<EntityType> supportedHostileMobs = Arrays.asList(new EntityType[] {
			EntityType.BLAZE, EntityType.CREEPER, EntityType.ENDER_DRAGON, 
			EntityType.ENDERMITE, EntityType.GHAST, EntityType.GUARDIAN, 
			EntityType.MAGMA_CUBE, EntityType.SILVERFISH, EntityType.SKELETON,
			EntityType.SLIME, EntityType.WITCH, EntityType.WITHER,
			EntityType.ZOMBIE
	});
	
	private final List<EntityType> supportedNeutralMobs = Arrays.asList(new EntityType[] {
			EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.PIG_ZOMBIE, EntityType.SPIDER
	});
	
	private final List<EntityType> supportedPassiveMobs = Arrays.asList(new EntityType[] {
			EntityType.BAT, EntityType.CHICKEN, EntityType.COW, EntityType.MUSHROOM_COW,
			EntityType.PIG, EntityType.RABBIT, EntityType.SHEEP, EntityType.SQUID, 
			EntityType.VILLAGER, EntityType.HORSE, EntityType.IRON_GOLEM, 
			EntityType.OCELOT, EntityType.WOLF, EntityType.SNOWMAN
	});
	/********************************************************************************************/
	
	// entities that are to have their particles removed according to config.yml
	private Set<EntityType> enabledEntities;
	private String configFile = NoParticles.main.getDataFolder() + File.separator + "config.yml";
	
	public ConsumerListener() {
		super();
		reloadConfig();
	}
	
	public void reloadConfig() {
		
		enabledEntities = new HashSet<>();

		FileConfiguration config = NoParticles.main.getConfig();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		// check if there is an enabled mobs configuration section
		if (!config.contains(CONFIG_ENABLED_MOBS) || !(config.isConfigurationSection(CONFIG_ENABLED_MOBS))) {
			System.out.println("No configuration file loaded - mobs disabled");
			config.createSection(CONFIG_ENABLED_MOBS);
			config.getConfigurationSection(CONFIG_ENABLED_MOBS).set("allmobs", false);
			try {
				config.save(configFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		ConfigurationSection mobs = config.getConfigurationSection(CONFIG_ENABLED_MOBS);
		
		for (String key : mobs.getKeys(false)) {
			
			key = key.toUpperCase();
			
			if (key.equalsIgnoreCase("allmobs")) {
				if (mobs.getBoolean(key)) {
					enabledEntities.addAll(supportedHostileMobs);
					enabledEntities.addAll(supportedNeutralMobs);
					enabledEntities.addAll(supportedPassiveMobs);
				} else {
					enabledEntities.removeAll(supportedHostileMobs);
					enabledEntities.removeAll(supportedNeutralMobs);
					enabledEntities.removeAll(supportedPassiveMobs);
				}
			} else if (key.equalsIgnoreCase("passivemobs")) {
				if (mobs.getBoolean(key)) enabledEntities.addAll(supportedPassiveMobs);
				else enabledEntities.removeAll(supportedPassiveMobs);
			} else if (key.equalsIgnoreCase("neutralmobs")) {
				if (mobs.getBoolean(key)) enabledEntities.addAll(supportedNeutralMobs);
				else enabledEntities.removeAll(supportedNeutralMobs);
			} else if (key.equalsIgnoreCase("hostilemobs")) {
				if (mobs.getBoolean(key)) enabledEntities.addAll(supportedHostileMobs);
				else enabledEntities.removeAll(supportedHostileMobs);
			} else if (supportedPassiveMobs.contains(EntityType.valueOf(key))) {
				if (mobs.getBoolean(key)) enabledEntities.add(EntityType.valueOf(key));
				else enabledEntities.remove(EntityType.valueOf(key));
			} else if (supportedNeutralMobs.contains(EntityType.valueOf(key))) {
				if (mobs.getBoolean(key)) enabledEntities.add(EntityType.valueOf(key));
				else enabledEntities.remove(EntityType.valueOf(key));
			} else if (supportedHostileMobs.contains(EntityType.valueOf(key))) {
				if (mobs.getBoolean(key)) enabledEntities.add(EntityType.valueOf(key));
				else enabledEntities.remove(EntityType.valueOf(key));
			} else {
				System.out.println("'" + key + "'" + " is not a valid mob name in config");
			}
		}
	}
	
	/**
	 * 
	 * @return A String list of all mobs that will have their particles removed from splash potions
	 */
	public String listAllMobs() {
		String mobs = "Enabled mobs: ";
		
		for (EntityType ent : enabledEntities) mobs += ent.name() + ", ";
		return mobs.substring(0, mobs.length()-2);
	}
	
	/**
	 * 
	 * @return A String list of all mobs supported by NoParticles
	 */
	public String listSupportedMobs() {
		String mobs = "Supported mobs: ";
		
		for (EntityType ent : supportedHostileMobs) mobs += ent.name() + ", ";
		for (EntityType ent : supportedNeutralMobs) mobs += ent.name() + ", ";
		for (EntityType ent : supportedPassiveMobs) mobs += ent.name() + ", ";
		return mobs.substring(0, mobs.length()-2);
	}

	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onItemConsume(PlayerItemConsumeEvent event) {
		
		Player pl = event.getPlayer();
		
		if (!pl.hasPermission(PERMISSION_NO_PARTICLES)) return;
		
		if (!event.getItem().getType().equals(Material.POTION)) return;
		
		new ParticleCleaner(pl).runTaskLater(NoParticles.main, 1); // schedule to fire on the next tick
	}
	
	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onItemSplash(PotionSplashEvent event) {
		
		ArrayList<LivingEntity> entities = new ArrayList<>();
		
		for (LivingEntity e : event.getAffectedEntities()) {
			if (e instanceof Player) {
				Player pl = (Player) e;
				if (!pl.hasPermission(PERMISSION_NO_PARTICLES)) continue;
				entities.add(e);
			} else if (enabledEntities.contains(e.getType())) {
				entities.add(e);
			}
		}
		
		if (entities.size() == 0) return;
		
		new ParticleCleaner(entities).runTaskLater(NoParticles.main, 1); // schedule to fire on the next tick
	}
	
	/**
	 * ParticleCleaner class is responsible for removing the particles of Entities
	 * A ParticleCleaner exists for every potion consumed or splash potion thrown
	 * @author Daniel
	 *
	 */
	private class ParticleCleaner extends BukkitRunnable {
		
		ArrayList<LivingEntity> entities;
		/**
		 * Initializer for a single entity consuming a potion
		 * @param ent the entity that consumed a potion
		 */
		public ParticleCleaner(LivingEntity ent) {
			super();
			entities = new ArrayList<>();
			entities.add(ent);
		}
		
		/**
		 * Initializer for a list of entities that have been hit by a given splash potion
		 * @param ents the entities that were hit by the splash potion
		 */
		public ParticleCleaner(ArrayList<LivingEntity> ents) {
			// TODO Auto-generated constructor stub
			entities = ents;
		}
		
		@Override
		public void run() {
			// iterate through each entity
			for (LivingEntity entity : entities) {
				// go through all of their potion effects -- Maybe only clear for specific potion?
				for (PotionEffect p : entity.getActivePotionEffects()) {
					if (p.hasParticles()) {
						// if the effect has particles, we remove it and add an identical one without particles
						entity.removePotionEffect(p.getType());
						entity.addPotionEffect(new PotionEffect(p.getType(), p.getDuration(), p.getAmplifier(), false, false));
					}
				}
			}
		}
	}
}
