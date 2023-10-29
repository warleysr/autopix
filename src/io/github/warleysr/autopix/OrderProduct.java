package io.github.warleysr.autopix;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class OrderProduct {
	
	private String product;
	private float price;
	private ItemStack icon;
	private List<String> commands;
	
	public OrderProduct(String product, float price, ItemStack icon, List<String> commands) {
		this.product = product;
		this.price = price;
		this.icon = icon;
		this.commands = commands;
	}
	
	public String getProduct() {
		return product;
	}
	
	public float getPrice() {
		return price;
	}
	
	public ItemStack getIcon() {
		return icon;
	}
	
	public List<String> getCommands() {
		return commands;
	}

}
