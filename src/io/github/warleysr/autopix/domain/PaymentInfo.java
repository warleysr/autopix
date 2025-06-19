package io.github.warleysr.autopix.domain;

public class PaymentInfo {
	
	private String transactionId;
	private String status;
	private double paidAmount;
	
	public PaymentInfo(String transactionId, String status, double paidAmount) {
		this.transactionId = transactionId;
		this.status = status;
		this.paidAmount = paidAmount;
	}

	public String getTransactionId() {
		return transactionId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public double getPaidAmount() {
		return paidAmount;
	}

}
