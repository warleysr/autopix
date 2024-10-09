package io.github.warleysr.autopix;

public class DonorInfo {
	
	private String donor;
	private float total;
	
	public DonorInfo(String donor, float total) {
		this.donor = donor;
		this.total = total;
	}
	
	public String getDonor() {
		return this.donor;
	}
	
	public float getTotal() {
		return this.total;
	}

}
