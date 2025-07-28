package io.github.warleysr.autopix;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.warleysr.autopix.commands.APMenuCommand;
import io.github.warleysr.autopix.commands.AutoPixCommand;
import io.github.warleysr.autopix.domain.Order;
import io.github.warleysr.autopix.expansion.AutoPixExpansion;
import io.github.warleysr.autopix.inventory.InventoryListener;
import io.github.warleysr.autopix.inventory.InventoryManager;

public class AutoPix extends JavaPlugin {
	
	private static String PIX_KEY;
	private static String PIX_NAME;
	private static BukkitTask VALIDATE_TASK, MAPS_TASK, HOLOGRAM_TASK;
	
	private static AutoPix instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		saveDefaultConfig();
		if (!reloadPlugin()) return;
		
		getCommand("autopix").setExecutor(new AutoPixCommand());
		getCommand("autopixmenu").setExecutor(new APMenuCommand());
		
		Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
		
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new AutoPixExpansion().register();
		}
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
	
	public static boolean reloadPlugin() {
		AutoPix plugin = getInstance();
		plugin.reloadConfig();
		
		PIX_KEY = plugin.getConfig().getString("pix.chave");
		PIX_NAME = plugin.getConfig().getString("pix.nome");
		
		MSG.loadMessages(plugin);
		
		try {
			if (!(OrderManager.startOrderManager(plugin))) {
				plugin.setEnabled(false);
				return false;
			}
			
		} catch (SQLException e) {
			Bukkit.getConsoleSender().sendMessage(MSG.getMessage("erro-sql")
					.replace("{mensagem}", e.getMessage()));
			plugin.setEnabled(false);
			return false;
		}
		
		InventoryManager.createMenuInventory(plugin);
		
		// Start async task to validate transactions automatically
		if (plugin.getConfig().getBoolean("automatico.ativado")) {
			if (VALIDATE_TASK != null)
				VALIDATE_TASK.cancel();
			
			int interval = plugin.getConfig().getInt("automatico.intervalo");
			
			VALIDATE_TASK = new BukkitRunnable() {
				@Override
				public void run() {
					OrderManager.validatePendings(plugin);
				}
			}.runTaskTimerAsynchronously(plugin, interval * 20L, interval * 20L);
		}
		
		// Start task to remove unpaid maps
		if (MAPS_TASK != null)
			MAPS_TASK.cancel();
		
		int remInterval = plugin.getConfig().getInt("mapa.intervalo");
		
		MAPS_TASK = new BukkitRunnable() {		
			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					Order order = OrderManager.getLastOrder(p.getName());
					if (order == null) continue;
					
					long diff = System.currentTimeMillis() - order.getCreated().getTime();
					long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
					
					if (minutes >= plugin.getConfig().getInt("mapa.tempo-pagar"))
						InventoryManager.removeUnpaidMaps(p);
				}
			}
		}.runTaskTimerAsynchronously(plugin, remInterval * 20L, remInterval * 20L);
		
		// Start task to update top donors hologram data
		if (HOLOGRAM_TASK != null)
			HOLOGRAM_TASK.cancel();
		
		int holoInterval = plugin.getConfig().getInt("tempos.holograma", 30);
		
		HOLOGRAM_TASK = new BukkitRunnable() {		
			@Override
			public void run() {
				AutoPixExpansion.updateTopDonorsCache(OrderManager.getTopDonors());
			}
		}.runTaskTimerAsynchronously(plugin, 0L, holoInterval * 20L);
		
		return true;
	}

}
