package io.github.warleysr.autopix.qrcode;

public class PixGenerator {
	
	private static final String PAYLOAD_1 = "00020126"; 
	private static final String PAYLOAD_2 = "0014BR.GOV.BCB.PIX01";
	private static final String PAYLOAD_3 = "52040000530398654";
	private static final String PAYLOAD_4 = "5802BR59";
	private static final String PAYLOAD_5 = "6008BRASILIA62";
	private static final String PAYLOAD_6 = "6304";
	
	public static String generatePayload(String key, String name, String description, float price) {
		String priceFmt = String.format("%.2f", price);
		String desc = "05" + String.format("%02d", description.length()) + description;
		String merchant = PAYLOAD_2 + Integer.toString(key.length()) + key;
		
		String payload = PAYLOAD_1 + String.format("%02d", merchant.length()) + merchant
				+ PAYLOAD_3 + String.format("%02d", priceFmt.length()) + priceFmt 
				+ PAYLOAD_4 + String.format("%02d", name.length()) + name
				+ PAYLOAD_5 + String.format("%02d", desc.length()) + desc
				+ PAYLOAD_6;
		
		return payload + String.format("%04X", CRC16.calculateCRC(payload)).toUpperCase();
	}
}