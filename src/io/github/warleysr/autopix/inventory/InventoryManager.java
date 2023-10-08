package io.github.warleysr.autopix.inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.OrderProduct;

public class InventoryManager {
	
	private static HashMap<String, Inventory> MENUS = new HashMap<>();
	private static HashMap<String, String> MENUS_TITLES = new HashMap<>();
	private static Inventory CONFIRM;
	private static String confirmTitle;
	private static String mapTitle;
	private static int cancelSlot, confirmSlot, discountSlot;
	private static ItemStack INFO;
	
	private static final HashMap<String, HashMap<Integer, OrderProduct>> PRODUCTS = new HashMap<>();
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy '\u00e0s' HH:mm");
	
	public static void createMenuInventory(AutoPix ap) {
		
		for (String menu : ap.getConfig().getConfigurationSection("menu").getKeys(false)) {
			menu = menu.toLowerCase();
			if (menu.equals("confirmar")) continue; 
			
			String menuTitle = ap.getConfig().getString("menu." + menu + ".titulo").replace('&', '\u00a7');
			Inventory inv = Bukkit.createInventory(null, ap.getConfig().getInt("menu." + menu + ".tamanho"), menuTitle);
			
			for (String product : ap.getConfig().getConfigurationSection("menu." + menu + ".produtos").getKeys(false)) {
				float price = (float) ap.getConfig().getDouble("menu." + menu + ".produtos." + product + ".preco");
				int slot = ap.getConfig().getInt("menu." + menu + ".produtos." + product + ".icone.slot") - 1;
				
				ItemStack icon = loadItem(ap.getConfig(), "menu." + menu + ".produtos." + product + ".icone", price);
				
				inv.setItem(slot, icon);
				
				if (!(PRODUCTS.containsKey(menu)))
					PRODUCTS.put(menu, new HashMap<>());
				
				PRODUCTS.get(menu).put(slot, new OrderProduct(product, price, icon));
			}
			
			MENUS.put(menu, inv);
			MENUS_TITLES.put(menuTitle, menu);
		}
		
		confirmTitle = ap.getConfig().getString("menu.confirmar.titulo").replace('&', '\u00a7');
		CONFIRM = Bukkit.createInventory(null, ap.getConfig().getInt("menu.confirmar.tamanho"), confirmTitle);
		
		mapTitle = ap.getConfig().getString("mapa.nome").replace('&', '\u00a7');
		
		cancelSlot = ap.getConfig().getInt("menu.confirmar.cancelar.slot") - 1;
		confirmSlot = ap.getConfig().getInt("menu.confirmar.confirmar.slot") - 1;
		discountSlot = ap.getConfig().getInt("menu.confirmar.desconto.slot") - 1;
		
		ItemStack cancel = loadItem(ap.getConfig(), "menu.confirmar.cancelar.icone", 0);
		ItemStack confirm = loadItem(ap.getConfig(), "menu.confirmar.confirmar.icone", 0);
		ItemStack discount = loadItem(ap.getConfig(), "menu.confirmar.desconto.icone", 0);
		
		CONFIRM.setItem(cancelSlot, cancel);
		CONFIRM.setItem(confirmSlot, confirm);
		CONFIRM.setItem(discountSlot, discount);
		
		INFO = new ItemStack(Material.WRITTEN_BOOK);
		
		BookMeta meta = (BookMeta) INFO.getItemMeta();
		
		for (String page : ap.getConfig().getConfigurationSection("info.paginas").getKeys(false)) {
			String content = "";
			for (String line : ap.getConfig().getStringList("info.paginas." + page))
				content += line.replace('&', '\u00a7') + "\n";
			
			meta.addPage(content);
		}
		meta.setTitle(ap.getConfig().getString("info.titulo").replace('&', '\u00a7'));
		meta.setAuthor(ap.getConfig().getString("info.autor").replace('&', '\u00a7'));
		
		INFO.setItemMeta(meta);
	}
	
	public static void openMenu(Player p, String menu) {
		if (MENUS.containsKey(menu))
			p.openInventory(MENUS.get(menu));
	}
	
	public static void openConfirmation(Player p) {
		p.openInventory(CONFIRM);
	}
	
	@SuppressWarnings("deprecation")
	public static void openInfo(Player p) {
		if (AutoPix.getRunningVersion() >= 1009)
			p.openBook(INFO);
		else if (p.getItemInHand().getType() == Material.AIR)
			p.getInventory().setItemInHand(INFO);
		else
			MSG.sendMessage(p, "mao-vazia");
		
	}
	
	public static String getMenuByTitle(String title) {
		if (MENUS_TITLES.containsKey(title))
			return MENUS_TITLES.get(title);
		return null;
	}
	
	public static String getMapTitle() {
		return mapTitle;
	}
	
	public static String getConfirmTitle() {
		return confirmTitle;
	}
	
	public static int getCancelSlot() {
		return cancelSlot;
	}
	
	public static int getConfirmSlot() {
		return confirmSlot;
	}
	
	public static int getDiscountSlot() {
		return discountSlot;
	}
	
	public static OrderProduct getOrderProduct(String menu, int slot) {
		return PRODUCTS.get(menu).get(slot);
	}
	
	@SuppressWarnings("deprecation")
	private static ItemStack loadItem(FileConfiguration fc, String path, float price) {
		Material material = Material.getMaterial(fc.getString(path + ".material").toUpperCase());
		ItemStack item = new ItemStack(material != null ? material : Material.STONE);
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(fc.getString(path + ".nome").replace('&', '\u00a7')
				.replace("{preco}", String.format("%.2f", price).replace('.', ',')));
		
		if (fc.isList(path + ".descricao")) {
			List<String> lore = new ArrayList<>();
			for (String line : fc.getStringList(path + ".descricao"))
				lore.add(line.replace('&', '\u00a7').replace("{preco}", Float.toString(price)));
			
			meta.setLore(lore);
		}
		
		item.setDurability((short) fc.getInt(path + ".data", 0));
		
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		
		return item;
	}
	
	public static void removeUnpaidMaps(Player p) {
		for (ItemStack item : p.getInventory().getContents()) {
			if (item == null) continue;
			if (!(item.hasItemMeta())) continue;
			if (!(item.getItemMeta().hasDisplayName())) continue;
			if (item.getItemMeta().getDisplayName().equals(mapTitle))
				p.getInventory().remove(item);
		}
	}
	
	public static void updateMapMeta(AutoPix ap, MapMeta meta, Player p, OrderProduct op) {
		meta.setDisplayName(mapTitle);
		
		List<String> lore = new ArrayList<>();
		if (op != null)
			for (String line : ap.getConfig().getStringList("mapa.descricao"))
				lore.add(line
						.replace('&', '\u00a7')
						.replace("{jogador}", p.getName())
						.replace("{produto}", op.getProduct())
						.replace("{data}", DATE_FORMAT.format(new Date())));
		
		meta.setLore(lore);
	}

}
