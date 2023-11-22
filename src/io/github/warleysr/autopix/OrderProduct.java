package io.github.warleysr.autopix;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class OrderProduct {
	
	private String product;
	private float price;
	private ItemStack icon;
	private List<String> preCommands;
	private List<String> commands;
	
	public OrderProduct(String product, float price, ItemStack icon, List<String> preCommands, List<String> commands) {
		this.product = product;
		this.price = price;
		this.icon = icon;
		this.preCommands = preCommands;
		this.commands = commands;
	}
	
	public String getProduct() {
		return this.product;
	}
	
	public float getPrice() {
		return this.price;
	}
	
	public ItemStack getIcon() {
		return this.icon;
	}
	
	public List<String> getPreCommands() {
		return this.preCommands;
	}
	
	public List<String> getCommands() {
		return this.commands;
	}

}
