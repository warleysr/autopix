package io.github.warleysr.autopix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.inventory.InventoryManager;

public class APMenuCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String command, String[] args) {
		if (cmd.getName().equalsIgnoreCase("autopixmenu")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Esse comando so pode ser usado in-game.");
				return false;
			}
			Player p = (Player) sender;
			if (!(p.hasPermission("autopix.use"))) {
				MSG.sendMessage(p, "sem-permissao");
				return false;
			}
			if (args.length == 0)
				InventoryManager.openMenu(p, "principal");
			else
				InventoryManager.openMenu(p, args[0].toLowerCase());
		}
		return true;
	}
	
	

}
