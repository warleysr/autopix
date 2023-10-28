package io.github.warleysr.autopix;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
			if (!(OrderManager.startOrderManager(this))) {
				setEnabled(false);
				return;
			}
			
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
		
		// Start async task to validate transactions automatically
		if (getConfig().getBoolean("automatico.ativado")) {
			int interval = getConfig().getInt("automatico.intervalo");
			
			new BukkitRunnable() {
				@Override
				public void run() {
					OrderManager.validatePendings(AutoPix.this);
				}
			}.runTaskTimerAsynchronously(this, interval * 20L, interval * 20L);
		}
		
		// Start task to remove unpaid maps
		int remInterval = getConfig().getInt("mapa.intervalo");
		new BukkitRunnable() {		
			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					Order order = OrderManager.getLastOrder(p.getName());
					if (order == null) continue;
					
					long diff = System.currentTimeMillis() - order.getCreated().getTime();
					long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
					
					if (minutes >= getInstance().getConfig().getInt("mapa.tempo-pagar"))
						InventoryManager.removeUnpaidMaps(p);
				}
			}
		}.runTaskTimerAsynchronously(this, remInterval * 20L, remInterval * 20L);
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
