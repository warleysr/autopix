package io.github.warleysr.autopix.mercadopago;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import io.github.warleysr.autopix.OrderProduct;
import io.github.warleysr.autopix.inventory.InventoryManager;


public class MPValidator {
	
	private static final String API_URL = "https://api.mercadopago.com/v1/payments/search" 
										+ "?sort=date_created&criteria=desc";
	
	public static void validateTransaction(AutoPix ap, String txid, Player p) {	
		try {
			URL url = new URL(API_URL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "application/json");
	        connection.setRequestProperty("Authorization", "Bearer " + ap.getConfig().getString("token-mp"));

	        int statusCode = connection.getResponseCode();
	        
	        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line;
	        StringBuilder response = new StringBuilder();

	        while ((line = reader.readLine()) != null) {
	            response.append(line);
	        }
	        reader.close();
	        
			if (statusCode != 200) {
				Bukkit.getConsoleSender().sendMessage("\u00a7b[AutoPix] \u00a7cErro ao validar PIX:\n" 
							+ statusCode + " - " + response.toString() 
							+ "\nVerifique se configurou corretamente o token do MP.");
				MSG.sendMessage(p, "erro-validar");
				return;
			}
			
			List<Order> orders = OrderManager.getOrders(p.getName());
			
			JSONObject json = (JSONObject) new JSONParser().parse(response.toString());
			
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
					OrderProduct op = InventoryManager.getProductByOrder(order);
					if (op == null) continue;
					if (order.isValidated()) continue;
					if (Math.abs(order.getPrice() - paid) > 0.001) continue;
					if (OrderManager.setTransaction(order, txid)) {
					
							new BukkitRunnable() {
								
								@Override
								public void run() {
									InventoryManager.removeUnpaidMaps(p);
									
									for (String cmd : op.getCommands())
										Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
												cmd.replace("{player}", p.getName()).replace('&', '\u00a7'));
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
