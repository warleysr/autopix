package io.github.warleysr.autopix;

import org.bukkit.inventory.ItemStack;

public class OrderProduct {
	
	private String product;
	private float price;
	private ItemStack icon;
	
	public OrderProduct(String product, float price, ItemStack icon) {
		this.product = product;
		this.price = price;
		this.icon = icon;
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

}
