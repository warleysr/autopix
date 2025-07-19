package io.github.warleysr.autopix.expansion;

import java.util.List;

import org.bukkit.OfflinePlayer;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.domain.DonorInfo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class AutoPixExpansion extends PlaceholderExpansion {
	
	private static List<DonorInfo> topDonors;

	@Override
	public String getAuthor() {
		return String.join(", ", AutoPix.getInstance().getDescription().getAuthors());
	}

	@Override
	public String getIdentifier() {
		return "autopix";
	}

	@Override
	public String getVersion() {
		return AutoPix.getInstance().getDescription().getVersion();
	}
	
	@Override
	public String onRequest(OfflinePlayer player, String params) {
		if (params.startsWith("top_")) {
			try {
				int top = Integer.parseInt(params.substring(4));
				if (top > topDonors.size()) return "";
				DonorInfo info = topDonors.get(top - 1);
				String line = MSG.getMessage("corpo-top")
						.replace("{doador}", info.getDonor())
						.replace("{total}", String.format("%.2f", info.getTotal()).replace('.', ','));
				return line;
				
			} catch (NumberFormatException e) { return ""; }
		}
		return super.onRequest(player, params);
	}
	
	public static void updateTopDonorsCache(List<DonorInfo> topDonors) {
		AutoPixExpansion.topDonors = topDonors;
	}

}
