package io.github.warleysr.autopix.inventory;


import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.zxing.WriterException;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.TimeManager;
import io.github.warleysr.autopix.domain.Order;
import io.github.warleysr.autopix.domain.OrderProduct;
import io.github.warleysr.autopix.domain.PixData;
import io.github.warleysr.autopix.mercadopago.MercadoPagoAPI;
import io.github.warleysr.autopix.qrcode.ImageCreator;
import io.github.warleysr.autopix.qrcode.PixGenerator;

public class InventoryListener implements Listener {
	
	private static final HashMap<String, String> BUYING_MENU = new HashMap<>();
	private static final HashMap<String, Integer> BUYING_SLOT = new HashMap<>();
	private static final HashMap<String, Float> DISCOUNT_PRICES = new HashMap<>();
	private static final ArrayList<String> SET_DISCOUNT = new ArrayList<>();
	private static final ArrayList<String> NO_RENDER_NEEDED = new ArrayList<>();
	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		String menu = InventoryManager.getMenuByTitle(e.getView().getTitle());
		if (menu != null) {
		
			if (e.getCurrentItem() == null) return;
			
			e.setCancelled(true);
			
			final OrderProduct op = InventoryManager.getOrderProduct(menu, e.getSlot());
			if (op == null) return;
			
			BUYING_MENU.put(e.getWhoClicked().getName(), menu);
			BUYING_SLOT.put(e.getWhoClicked().getName(), e.getSlot());
			DISCOUNT_PRICES.remove(e.getWhoClicked().getName());
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
			if (e.getSlot() == InventoryManager.getDiscountSlot()) {
				p.closeInventory();
				SET_DISCOUNT.add(p.getName());
				MSG.sendMessage(p, "inserir-cupom");
				return;
			}
			if (e.getSlot() != InventoryManager.getConfirmSlot()) return;
			
			p.closeInventory();
			
			if (p.getInventory().getItemInHand().getType() != Material.AIR) {
				MSG.sendMessage(p, "mao-vazia");
				return;
			}
			if (!(TimeManager.canExecute(AutoPix.getInstance(), p, "create"))) return;
			
			menu = BUYING_MENU.get(p.getName());
			Integer slot = BUYING_SLOT.get(p.getName());
			if (menu == null || slot == null) return;
			
			final OrderProduct op = InventoryManager.getOrderProduct(menu, slot);
			if (op == null) return;
			
			final float price = DISCOUNT_PRICES.getOrDefault(p.getName(), op.getPrice());
			
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						Object payload = null;
						// If using automatic mode, prevent from creating a new order if the last one is pending
						boolean automaticMode = AutoPix.getInstance().getConfig().getBoolean("automatico.ativado");
						boolean generateMap = AutoPix.getInstance().getConfig().getBoolean("pix.mapa");
						
						if (automaticMode) {
							Order lastOrder = OrderManager.getLastOrder(p.getName());
							if (lastOrder != null) {
								PixData pd = OrderManager.getPixData(lastOrder);
								if (pd != null && pd.isPending()) {
									MSG.sendMessage(p, "cancelar-primeiro");
									return;
								}
							}
							MSG.sendMessage(p, "criando-mapa");
							payload = MercadoPagoAPI.createPixPayment(AutoPix.getInstance(), p, op, price);
						
						} else {
							MSG.sendMessage(p, "criando-mapa");
							
							// If automatic mode is not enabled, create a static QR code
							payload = PixGenerator.generatePayload(
									AutoPix.getPixKey(), AutoPix.getPixName(), op.getProduct(), price
									);
							
							if (AutoPix.getInstance().getConfig().getBoolean("pix.debug", false))
								Bukkit.getConsoleSender().sendMessage(
										"\u00a7b[AutoPix] \u00a7aPayload: \u00a7f" + payload
										);
							
						}
						if (payload == null) {
							MSG.sendMessage(p, "erro");
							return;
						}
						
						Order ord = OrderManager.createOrder(p, op.getProduct(), price);
						if (ord == null) {
							MSG.sendMessage(p, "erro");
							return;
						}
						if (payload instanceof PixData) {
							PixData pd = (PixData) payload;
							pd.setOrderId(ord.getId());
							boolean success = OrderManager.savePixData(pd);
							if (!(success)) {
								MSG.sendMessage(p, "erro");
								return;
							}
							payload = pd.getQrCode();
						}
						
						final BufferedImage qr = (automaticMode || generateMap) 
								? ImageCreator.generateQR((String) payload) : null;
						
						new BukkitRunnable() {
							@Override
							public void run() {
								for (String preCmd : op.getPreCommands())
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), preCmd.replace("{player}", p.getName()));
								
								if (automaticMode || generateMap) {
									ItemStack map = ImageCreator.generateMap(qr, p, op);
									p.setItemInHand(map);
									
									if (!(NO_RENDER_NEEDED.contains(p.getName())))
										NO_RENDER_NEEDED.add(p.getName());
								}
								
								try {
									Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
									p.sendTitle(MSG.getMessage("titulo-qr"), MSG.getMessage("subtitulo-qr"), 10, 70, 20);
								} catch (NoSuchMethodException e) {
									MSG.sendMessage(p, "titulo-qr");
									MSG.sendMessage(p, "subtitulo-qr");
								}
							}
						}.runTask(AutoPix.getInstance());
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}.runTaskAsynchronously(AutoPix.getInstance());
		}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		ItemStack item = e.getItemDrop().getItemStack();
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
				&& item.getItemMeta().getDisplayName().equals(InventoryManager.getMapTitle())) {
			if (item.getItemMeta().hasLore())
				e.setCancelled(true);
			else
				e.getItemDrop().remove();
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.hasItem() && e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasDisplayName() 
				&& e.getItem().getItemMeta().getDisplayName().equals(InventoryManager.getMapTitle()))
			e.setCancelled(true);
	}
	
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (NO_RENDER_NEEDED.contains(p.getName())) return;
		
		for (int i = 0; i < 9; i++) {
			final int slot = i;
			
			ItemStack item = p.getInventory().getItem(slot);
			if (!(InventoryManager.isPaymentMap(item))) continue;
			
			new BukkitRunnable() {
				@Override
				public void run() {
					Order order = OrderManager.getLastOrder(p.getName());
					if (order == null) return;
					
					OrderProduct op = InventoryManager.getProductByOrder(order);
					if (op == null) return;
					
					PixData pd = OrderManager.getPixData(order);
					if (pd == null) return;
					
					new BukkitRunnable() {
						@Override
						public void run() {
							try {
								BufferedImage qrCode = ImageCreator.generateQR(pd.getQrCode());
								ItemStack map = ImageCreator.generateMap(qrCode, p, op);
								
								p.getInventory().setItem(slot, map);
								NO_RENDER_NEEDED.add(p.getName());
								
							} catch (UnsupportedEncodingException | WriterException e1) {
								e1.printStackTrace();
							}
						}
					}.runTask(AutoPix.getInstance());
				}
			}.runTaskAsynchronously(AutoPix.getInstance());
					
			break;
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (e.isCancelled()) return;
		Player p = e.getPlayer();
		
		if (!(SET_DISCOUNT.contains(p.getName()))) return;
		
		e.setCancelled(true);
		
		String coupon = e.getMessage().trim();
		if (coupon.equalsIgnoreCase("cancelar")) {
			SET_DISCOUNT.remove(p.getName());
			MSG.sendMessage(p, "compra-cancelada");
			return;
		}
		FileConfiguration cfg = AutoPix.getInstance().getConfig();
		if (!(cfg.isSet("cupons." + coupon))) {
			MSG.sendMessage(p, "cupom-invalido");
			return;
		}
		String menu = BUYING_MENU.get(p.getName());
		Integer slot = BUYING_SLOT.get(p.getName());
		OrderProduct op = InventoryManager.getOrderProduct(menu, slot);
		
		if (!(cfg.getStringList("cupons." + coupon + ".itens").contains(op.getProduct()))) {
			MSG.sendMessage(p, "cupom-nao-aplicavel");
			return;
		}
		
		if (cfg.isSet("cupons." + coupon + ".validade")) {
			String expire = cfg.getString("cupons." + coupon + ".validade");
			try {
				Date expireDate = SDF.parse(expire);
				Date today = new Date();
				if (today.after(expireDate)) {
					MSG.sendMessage(p, "cupom-expirado");
					return;
				}
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}
		int discount = cfg.getInt("cupons." + coupon + ".porcentagem");
		float newPrice = (float) ((1.0 - (discount / 100.0)) * op.getPrice());

		SET_DISCOUNT.remove(p.getName());
		DISCOUNT_PRICES.put(p.getName(), newPrice);
		
		p.sendMessage(MSG.getMessage("cupom-aplicado")
				.replace("{porcentagem}", Integer.toString(discount))
				.replace("{valor}", String.format("%.2f", newPrice).replace('.', ','))
				.replace("{produto}", op.getProduct()));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				InventoryManager.openConfirmation(p);
			}
		}.runTask(AutoPix.getInstance());
		
	}
}
