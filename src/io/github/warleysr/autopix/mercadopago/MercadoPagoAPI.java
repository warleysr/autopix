package io.github.warleysr.autopix.mercadopago;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.domain.OrderProduct;
import io.github.warleysr.autopix.domain.PaymentInfo;
import io.github.warleysr.autopix.domain.PixData;

public class MercadoPagoAPI {
	
	private static final String API_URL = "https://api.mercadopago.com/v1/payments";
	
	public static PixData createPixPayment(
			AutoPix ap, Player p, OrderProduct product, float price
			) throws Exception {	
		
		String jsonBody = "{\r\n"
				+ "  \"description\": \"" + product.getProduct() + "\",\r\n"
				+ "  \"payer\": {\r\n"
				+ "    \"entity_type\": \"individual\",\r\n"
				+ "    \"type\": \"customer\",\r\n"
				+ "    \"email\": \"" + p.getName() + "@autopix.com\"\r\n"
				+ "  },\r\n"
				+ " \"external_reference\": \"" + p.getName() + "\","
				+ "  \"payment_method_id\": \"pix\",\r\n"
				+ "  \"transaction_amount\": " + String.format(Locale.ROOT, "%.2f", price) + ",\r\n"
				+ "	\"notification_url\": \"" 
				+ ap.getConfig().getString("automatico.notificacoes") + "\"\r\n"
				+ "}";

		URL url = new URL(API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + ap.getConfig().getString("token-mp"));
        connection.setRequestProperty("X-Idempotency-Key", UUID.randomUUID().toString());
        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.writeBytes(jsonBody);
            outputStream.flush();
        }
        
		int statusCode = connection.getResponseCode();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
		
		if (statusCode != 201) {
			Bukkit.getConsoleSender().sendMessage("\u00a7b[AutoPix] \u00a7cErro ao validar PIX:\n" 
					+ statusCode + " - " + response.toString()
					+ "\nVerifique se configurou corretamente o token do MP.");
			MSG.sendMessage(p, "erro-validar");
			return null;
		}
		
		JSONObject json = (JSONObject) new JSONParser().parse(response.toString());
		
		String paymentId = String.valueOf(json.get("id"));
		
		JSONObject poi = (JSONObject) json.get("point_of_interaction");
		String qr = (String) ((JSONObject) poi.get("transaction_data"))
				        .get("qr_code");
		
		return new PixData(paymentId, qr);
	}
	
	public static PaymentInfo getPayment(AutoPix ap, String id) {
		String responseMP = "";
		try {
			URL url = new URL(API_URL + "/" + id);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "application/json");
	        connection.setRequestProperty("Authorization", "Bearer " + ap.getConfig().getString("token-mp"));
	        
			int statusCode = connection.getResponseCode();
			if (statusCode != 200) {
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line;
	        StringBuilder response = new StringBuilder();

	        while ((line = reader.readLine()) != null) {
	            response.append(line);
	        }
	        reader.close();
	        
	        responseMP = response.toString();
	        
			JSONObject json = (JSONObject) new JSONParser().parse(response.toString());
			String status = (String) json.get("status");
			
			String pixId = null;
			double paid = 0.0;
			
			if (status.equals("approved")) {
				JSONObject details = (JSONObject) json.get("transaction_details");
				pixId = ((String) details.get("transaction_id")).substring(3);
				paid = (double) details.get("total_paid_amount");
			}
			
			return new PaymentInfo(pixId, status, paid);
			
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getConsoleSender().sendMessage("\u00a7b========== \u00a7aDEBUG \u00a7b==========");
			Bukkit.getConsoleSender().sendMessage("\u00a7aPagamento: \u00a7f" + id);
			Bukkit.getConsoleSender().sendMessage("\u00a7aRetorno MP:");
			Bukkit.getConsoleSender().sendMessage(responseMP.toString());
			Bukkit.getConsoleSender().sendMessage("\u00a7b================================================");
		}
		return null;
	}

}
