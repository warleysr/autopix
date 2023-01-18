package io.github.warleysr.autopix.qrcode;

public class PixGenerator {
	
	private static final String PAYLOAD_1 = "00020126330014BR.GOV.BCB.PIX01";
	private static final String PAYLOAD_2 = "52040000530398654";
	private static final String PAYLOAD_3 = "5802BR59";
	private static final String PAYLOAD_4 = "6008BRASILIA620805";
	private static final String PAYLOAD_5 = "6304";
	
	public static String generatePayload(String key, String name, String description, float price) {
		String priceFmt = String.format("%.2f", price);
		
		String payload = PAYLOAD_1 + Integer.toString(key.length()) + PAYLOAD_2 
				+ String.format("%02d", priceFmt.length()) + priceFmt + PAYLOAD_3
				+ String.format("%02d", name.length()) + name + PAYLOAD_4 
				+ String.format("%02d", description.length()) + description + PAYLOAD_5;
		
		return payload + CRC16.calculateCRC(payload);
	}

}
