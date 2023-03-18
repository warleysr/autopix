package io.github.warleysr.autopix.inventory;


import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.Order;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.OrderProduct;
import io.github.warleysr.autopix.TimeManager;
import io.github.warleysr.autopix.mercadopago.MercadoPagoAPI;
import io.github.warleysr.autopix.qrcode.ImageCreator;
import io.github.warleysr.autopix.qrcode.PixGenerator;

public class InventoryListener implements Listener {
	
	private static final HashMap<String, Integer> BUYING = new HashMap<>();
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getView().getTitle().equals(InventoryManager.getMenuTitle())) {
		
			if (e.getCurrentItem() == null) return;
			
			e.setCancelled(true);
			
			final OrderProduct op = InventoryManager.getOrderProduct(e.getSlot());
			if (op == null) return;
			
			BUYING.put(e.getWhoClicked().getName(), e.getSlot());
			InventoryManager.openConfirmation((Player) e.getWhoClicked());
	}
	else if (e.getView().getTitle().equals(InventoryManager.getConfirmTitle())) {
		
		if (e.getCurrentItem() == null) return;
		
		e.setCancelled(true);
		
		final Player p = (Player) e.getWhoClicked();
		
		if (e.getSlot() == InventoryManager.getCancelSlot()) {
			p.closeInventory();
			return;
		}
		if (e.getSlot() != InventoryManager.getConfirmSlot()) return;
		
		p.closeInventory();
		
		if (p.getInventory().getItemInHand().getType() != Material.AIR) {
			MSG.sendMessage(p, "mao-vazia");
			return;
		}
		if (!(TimeManager.canExecute(AutoPix.getInstance(), p, "create"))) return;
		
		Integer slot = BUYING.get(p.getName());
		if (slot == null) return;
		
		final OrderProduct op = InventoryManager.getOrderProduct(slot);
		if (op == null) return;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Order ord = OrderManager.createOrder(p, op.getProduct(), op.getPrice());
				if (ord == null) {
					MSG.sendMessage(p, "erro");
					return;
				}
				
				try {
					String payload = null;
					// If automatic mode is not enabled, create a static QR code
					if (!(AutoPix.getInstance().getConfig().getBoolean("automatico.ativado")))
						payload = PixGenerator
								.generatePayload(AutoPix.getPixKey(), AutoPix.getPixName(), 
										op.getProduct(), op.getPrice());
					else
						payload = MercadoPagoAPI.createPixPayment(AutoPix.getInstance(), p, op);
					
					
					final BufferedImage qr = ImageCreator.generateQR(payload);
					
					new BukkitRunnable() {
						@Override
						public void run() {
							
							ImageCreator.generateMap(qr, p, op);
							
							if (AutoPix.getRunningVersion() >= 1009)
								p.sendTitle(MSG.getMessage("titulo-qr"), MSG.getMessage("subtitulo-qr"), 10, 70, 20);
							else {
								MSG.sendMessage(p, "titulo-qr");
								MSG.sendMessage(p, "subtitulo-qr");
							}
						}
					}.runTask(AutoPix.getInstance());
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}).start();
	}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		ItemStack item = e.getItemDrop().getItemStack();
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
				&& item.getItemMeta().getDisplayName().equals(InventoryManager.getMapTitle()))
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.hasItem() && e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasDisplayName() 
				&& e.getItem().getItemMeta().getDisplayName().equals(InventoryManager.getMapTitle()))
			e.setCancelled(true);
	}
}
