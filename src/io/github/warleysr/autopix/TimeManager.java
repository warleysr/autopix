package io.github.warleysr.autopix;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

public class TimeManager {
	
	private static final HashMap<String, Long> CREATE = new HashMap<>();
	private static final HashMap<String, Long> LIST = new HashMap<>();
	private static final HashMap<String, Long> VALIDATE = new HashMap<>();
	
	public static boolean canExecute(AutoPix ap, Player p, String command) {
		HashMap<String, Long> map = null;
		int delay = 0;
		
		switch (command) {
		case "create":
			map = CREATE;
			delay = ap.getConfig().getInt("tempos.criar-pedido");
			break;
		case "list":
			map = LIST;
			delay = ap.getConfig().getInt("tempos.lista");
			break;
		case "validate":
			map = VALIDATE;
			delay = ap.getConfig().getInt("tempos.validar");
			break;
		default:
			return true;
		}
		long time = System.currentTimeMillis();
		
		if (map.containsKey(p.getName())) {
			long diff = time - map.get(p.getName());
			long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
			
			if (seconds >= delay) {
				map.put(p.getName(), time);
				return true;
			}
			
			p.sendMessage(MSG.getMessage("aguarde").replace("{tempo}", Long.toString(delay - seconds)));
			return false;
		}
		else {
			map.put(p.getName(), time);
			return true;
		}
	}

}
