package io.github.warleysr.autopix;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

public class TimeManager {
	
	private static final HashMap<String, HashMap<String, Long>> DELAY = new HashMap<>();
	
	public static boolean canExecute(AutoPix ap, Player p, String command) {
		HashMap<String, Long> map = null;
		int delay = 0;
		
		switch (command) {
		case "create":
			delay = ap.getConfig().getInt("tempos.criar-pedido");
			break;
		case "list":
			delay = ap.getConfig().getInt("tempos.lista");
			break;
		case "validate":
			delay = ap.getConfig().getInt("tempos.validar");
			break;
		case "cancel":
			delay = ap.getConfig().getInt("tempos.cancelar");
			break;
		case "top":
			delay = ap.getConfig().getInt("tempos.top");
			break;
		default:
			return true;
		}
		long time = System.currentTimeMillis();
		
		if (DELAY.containsKey(p.getName())) {
			map = DELAY.get(p.getName());
			
			long diff = time - map.getOrDefault(command, 0L);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
			
			if (seconds >= delay) {
				map.put(command, time);
				return true;
			}
			
			p.sendMessage(MSG.getMessage("aguarde").replace("{tempo}", Long.toString(delay - seconds)));
			return false;
		}
		else {
			map = new HashMap<String, Long>();
			map.put(command, time);
			DELAY.put(p.getName(), map);
			return true;
		}
	}

}
