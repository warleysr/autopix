package io.github.warleysr.autopix.qrcode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.Order;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.inventory.InventoryManager;


public class MPValidator {
	
	private static final String API_URL = "https://api.mercadopago.com/v1/payments/search" 
										+ "?sort=date_created&criteria=desc";
	
	public static void validateTransaction(AutoPix ap, String txid, Player p) {
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
									 .GET()
									 .header("Authorization", "Bearer " + ap.getConfig().getString("token-mp"))
									 .build();
		
		try {
			HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				Bukkit.getConsoleSender().sendMessage("\u00a7b[AutoPix] \u00a7cErro ao validar PIX:\n" 
							+ response.statusCode() + " - " + response.body() 
							+ "\nVerifique se configurou corretamente o token do MP.");
				MSG.sendMessage(p, "erro-validar");
				return;
			}
			List<Order> orders = OrderManager.getOrders(p.getName());
			
			JSONObject json = (JSONObject) new JSONParser().parse(response.body());
			
			// Iterate over transaction list to find which one has the provided ID
			for (Object resElem : (JSONArray) json.get("results")) {
				JSONObject details = (JSONObject)((JSONObject) resElem).get("transaction_details");
				
				if (details == null) continue;
				if (details.get("transaction_id") == null) continue;
				
				String transaction = (String) details.get("transaction_id");
				if (!(transaction.startsWith("PIX"))) continue;
				if (!(transaction.substring(3).equals(txid))) continue;
				
				Object paidObj = details.get("total_paid_amount");
				if (!(paidObj instanceof Number))
					continue;
				
				double paid = ((Number) paidObj).doubleValue();
				
				// Iterate over player orders to get the corresponding 
				for (Order order : orders) {
					if (order.isValidated()) continue;
					if (Math.abs(order.getPrice() - paid) > 0.001) continue;
					if (OrderManager.setTransaction(order, txid)) {
					
							new BukkitRunnable() {
								
								@Override
								public void run() {
									InventoryManager.removeUnpaidMaps(p);
									
									for (String cmd : ap.getConfig().getStringList("menu.produtos." 
											+ order.getProduct() + ".comandos")) {
										Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
												cmd.replace("{player}", p.getName()).replace('&', '\u00a7'));
									}
								}
							}.runTask(ap);
					}
					else
						MSG.sendMessage(p, "erro-validar");
					return;
				}
			}	
			
			MSG.sendMessage(p, "pix-nao-validado");
			
		} catch (Exception e) {
			e.printStackTrace();
			MSG.sendMessage(p, "erro-validar");
		}
	}

}
