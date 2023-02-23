package io.github.warleysr.autopix.mercadopago;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.Order;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.OrderProduct;

public class MercadoPagoAPI {
	
	private static final String API_URL = "https://api.mercadopago.com/v1/payments";
	
	public static String createPixPayment(AutoPix ap, Player p, OrderProduct product) {
		HttpClient client = HttpClient.newHttpClient();
		
		String jsonBody = "{\r\n"
				+ "  \"description\": \"" + product.getProduct() + "\",\r\n"
				+ "  \"payer\": {\r\n"
				+ "    \"entity_type\": \"individual\",\r\n"
				+ "    \"type\": \"customer\",\r\n"
				+ "    \"email\": \"" + p.getName() + "@autopix.com\",\r\n"
				+ "  },\r\n"
				+ "  \"payment_method_id\": \"pix\",\r\n"
				+ "  \"transaction_amount\": " + String.format("%.2f", product.getPrice()) + ",\r\n"
				+ "	\"notification_url\": \"" 
				+ ap.getConfig().getString("automatico.notificacoes") + "\"\r\n"
				+ "}";
			
		HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
									 .header("Content-Type", "application/json")
									 .header("Authorization", "Bearer " + ap.getConfig().getString("token-mp"))
									 .POST(BodyPublishers.ofString(jsonBody))
									 .build();
		
		try {
			HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 201) {
				Bukkit.getConsoleSender().sendMessage("\u00a7b[AutoPix] \u00a7cErro ao validar PIX:\n" 
						+ response.statusCode() + " - " + response.body() 
						+ "\nVerifique se configurou corretamente o token do MP.");
				MSG.sendMessage(p, "erro-validar");
				return null;
			}
			
			Order order = OrderManager.createOrder(p, product.getProduct(), product.getPrice());
			if (order == null) {
				MSG.sendMessage(p, "erro-validar");
				return null;
			}
			
			JSONObject json = (JSONObject) new JSONParser().parse(response.body());
			JSONObject poi = (JSONObject) json.get("point_of_interaction");
			String qr = (String) ((JSONObject) poi.get("transaction_data"))
					        .get("qr_code");
			
			return qr;
			
		} catch (Exception e) {
			e.printStackTrace();
			MSG.sendMessage(p, "erro-validar");
		}
		return null;
	}
	
	public static Object[] getPayment(AutoPix ap, String id) {
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL + "/" + id))
				 .header("Authorization", "Bearer " + ap.getConfig().getString("token-mp"))
				 .GET()
				 .build();
		
		try {
			HttpResponse<String> response = client.send(req, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return null;
			}
			JSONObject json = (JSONObject) new JSONParser().parse(response.body());
			String status = (String) json.get("status");
			if (!(status.equals("approved"))) return null;
			
			JSONObject details = (JSONObject) json.get("transaction_details");
			
			String pixId = ((String) details.get("transaction_id")).substring(3);
			double paid = (double) details.get("total_paid_amount");
			
			return new Object[] {pixId, paid};
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
