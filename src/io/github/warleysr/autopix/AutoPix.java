package io.github.warleysr.autopix;

import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.warleysr.autopix.commands.APMenuCommand;
import io.github.warleysr.autopix.commands.AutoPixCommand;
import io.github.warleysr.autopix.inventory.InventoryListener;
import io.github.warleysr.autopix.inventory.InventoryManager;

public class AutoPix extends JavaPlugin {
	
	private static String PIX_KEY;
	private static String PIX_NAME;
	
	private static AutoPix instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		saveDefaultConfig();
		reloadConfig();
		
		PIX_KEY = getConfig().getString("pix.chave");
		PIX_NAME = getConfig().getString("pix.nome");
		
		MSG.loadMessages(this);
		
		try {
			OrderManager.startOrderManager(this);
			
		} catch (SQLException e) {
			Bukkit.getConsoleSender().sendMessage(MSG.getMessage("erro-sql")
					.replace("{mensagem}", e.getMessage()));
			setEnabled(false);
			return;
		}
		
		InventoryManager.createMenuInventory(this);
		
		getCommand("autopix").setExecutor(new AutoPixCommand());
		getCommand("autopixmenu").setExecutor(new APMenuCommand());
		
		Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
	}
	
	public static AutoPix getInstance() {
		return instance;
	}
	
	public static String getPixKey() {
		return PIX_KEY;
	}
	
	public static String getPixName() {
		return PIX_NAME;
	}
	
	public static int getRunningVersion() {
		String[] version = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
		int major = Integer.valueOf(version[0]);
		int minor = Integer.valueOf(version[1]);
		
		return major * 1000 + minor;
	}

}
