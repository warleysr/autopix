package io.github.warleysr.autopix.domain;

import java.util.Date;

public class Order {
	
	private int id;
	private String player;
	private String product;
	private float price;
	private Date created;
	private String transaction;
	
	public Order(String player, String product, float value, long created) {
		this.player = player;
		this.product = product;
		this.price = value;
		this.created = new Date(created);
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getPlayer() {
		return player;
	}
	
	public String getProduct() {
		return product;
	}
	
	public float getPrice() {
		return price;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public String getTransaction() {
		return transaction;
	}
	
	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}
	
	public boolean isValidated() {
		return transaction != null;
	}
	

}
