package io.github.warleysr.autopix.inventory;

import java.util.ArrayList;
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

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.OrderProduct;

public class InventoryManager {
	
	private static Inventory MENU;
	private static Inventory CONFIRM;
	private static String menuTitle;
	private static String confirmTitle;
	private static int cancelSlot, confirmSlot;
	private static ItemStack INFO;
	
	private static final HashMap<Integer, OrderProduct> PRODUCTS = new HashMap<>();
	
	public static void createMenuInventory(AutoPix ap) {
		menuTitle = ap.getConfig().getString("menu.titulo").replace('&', '\u00a7');
		MENU = Bukkit.createInventory(null, ap.getConfig().getInt("menu.tamanho"), menuTitle);
		
		for (String product : ap.getConfig().getConfigurationSection("menu.produtos").getKeys(false)) {
			float price = (float) ap.getConfig().getDouble("menu.produtos." + product + ".preco");
			int slot = ap.getConfig().getInt("menu.produtos." + product + ".icone.slot") - 1;
			
			ItemStack icon = loadItem(ap.getConfig(), "menu.produtos." + product + ".icone", price);
			
			MENU.setItem(slot, icon);
			PRODUCTS.put(slot, new OrderProduct(product, price, icon));
		}
		
		confirmTitle = ap.getConfig().getString("menu.confirmar.titulo").replace('&', '\u00a7');
		CONFIRM = Bukkit.createInventory(null, ap.getConfig().getInt("menu.confirmar.tamanho"), confirmTitle);
		
		cancelSlot = ap.getConfig().getInt("menu.confirmar.cancelar.slot") - 1;
		confirmSlot = ap.getConfig().getInt("menu.confirmar.confirmar.slot") - 1;
		
		ItemStack cancel = loadItem(ap.getConfig(), "menu.confirmar.cancelar.icone", 0);
		ItemStack confirm = loadItem(ap.getConfig(), "menu.confirmar.confirmar.icone", 0);
		
		CONFIRM.setItem(cancelSlot, cancel);
		CONFIRM.setItem(confirmSlot, confirm);
		
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
	
	public static void openMenu(Player p) {
		p.openInventory(MENU);
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
	
	public static String getMenuTitle() {
		return menuTitle;
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
	
	public static OrderProduct getOrderProduct(int slot) {
		return PRODUCTS.get(slot);
	}
	
	@SuppressWarnings("deprecation")
	private static ItemStack loadItem(FileConfiguration fc, String path, float price) {
		Material material = Material.getMaterial(fc.getString(path + ".material").toUpperCase());
		ItemStack item = new ItemStack(material != null ? material : Material.STONE);
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(fc.getString(path + ".nome").replace('&', '\u00a7')
				.replace("{preco}", Float.toString(price)));
		
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

}
