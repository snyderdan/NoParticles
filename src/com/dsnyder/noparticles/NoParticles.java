package com.dsnyder.noparticles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for plugin. Registers listener and handles commands
 * @author Daniel
 * @version 1.1.0
 */
public class NoParticles extends JavaPlugin {
	
	public static final String CURRENT_VERSION = "1.1.1";
	
	static JavaPlugin main;
	
	private ConsumerListener myListener;
	
	@Override
	public void onEnable() {
		main = this;
		myListener = new ConsumerListener();
		getServer().getPluginManager().registerEvents(myListener, this);
	}
	
	@Override
	public void onDisable() {
		
	}
	
	@Override
    public boolean onCommand(CommandSender sender, 
    		Command command, String label, String[] args) {

		if (!(sender instanceof Player) && !(sender instanceof CommandSender))
            return false;
		// decode command
        if (command.getName().equalsIgnoreCase("noparticles")) {
        	// only main command recognized, specified in plugin.yml
        	if (args.length == 0) {
        		showHelp(sender);
        	} else if (args[0].equalsIgnoreCase("reload")) {
        		myListener.reloadConfig();
        		sender.sendMessage("Config successfully reloaded");
        	} else if (args[0].equalsIgnoreCase("version")) {
        		sender.sendMessage("NoParticles Version " + CURRENT_VERSION);
        	} else if (args[0].equalsIgnoreCase("list")) {
        		sender.sendMessage(myListener.listAllMobs());
        	} else if (args[0].equalsIgnoreCase("supported")) {
        		sender.sendMessage(myListener.listSupportedMobs());
        	}
        	else {
        		showHelp(sender);
        	}
        	return true;
        }
        
        return false;
    }
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage("§e/noparticles help §7- §aShow the help page");
		sender.sendMessage("§e/noparticles list §7- §aList mobs that will have particles removed");
        sender.sendMessage("§e/noparticles reload §7- §aReloads the plugin config");
        sender.sendMessage("§e/noparticles supported §7- §aList supported mobs");
        sender.sendMessage("§e/noparticles version §7- §aList current version of NoParticles");
	}
}
