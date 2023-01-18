package io.github.warleysr.autopix.qrcode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.Order;
import io.github.warleysr.autopix.OrderManager;


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
				MSG.sendMessage(p, "erro-validar");
				return;
			}
			List<Order> orders = OrderManager.getOrders(p.getName());
			
			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			
			// Iterate over transaction list to find which one has the provided ID
			for (JsonElement resElem : json.get("results").getAsJsonArray()) {
				JsonObject details = resElem.getAsJsonObject().get("transaction_details").getAsJsonObject();
				
				if (details == null) continue;
				if (details.get("transaction_id") == null) continue;
				
				String transaction = details.get("transaction_id").getAsString();
				if (!(transaction.startsWith("PIX"))) continue;
				if (!(transaction.substring(3).equals(txid))) continue;
				
				float value = details.get("total_paid_amount").getAsFloat();
				
				
				// Iterate over player orders to get the corresponding 
				for (Order ord : orders) {
					if (ord.isValidated()) continue;
					if (ord.getPrice() == value) {
						if (OrderManager.setTransaction(ord, txid)) {
						
							
								new BukkitRunnable() {
									
									@Override
									public void run() {
										for (String cmd : ap.getConfig().getStringList("menu.produtos." 
												+ ord.getProduct() + ".comandos")) {
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
			}
			
			MSG.sendMessage(p, "pix-nao-validado");
			
		} catch (Exception e) {
			e.printStackTrace();
			MSG.sendMessage(p, "erro-validar");
		}
	}

}
