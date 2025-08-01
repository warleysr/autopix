package io.github.warleysr.autopix.domain;

public class PixData {
	
	private String paymentId;
	private int orderId;
	private String status = "pending";
	private String qrCode;
	
	public PixData(String paymentId, String qrCode) {
		this.paymentId = paymentId;
		this.qrCode = qrCode;
	}
	
	public String getPaymentId() {
		return paymentId;
	}
	
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	
	public int getOrderId() {
		return orderId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getQrCode() {
		return qrCode;
	}
	
	public boolean isPending() {
		return status.equals("pending");
	}
	
}
