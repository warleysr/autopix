package io.github.warleysr.autopix;

import java.io.File;
import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MSG {
	
	private static final HashMap<String, String> MESSAGES = new HashMap<>();
	
	
	protected static void loadMessages(AutoPix ap) {
		File msgFile = new File(ap.getDataFolder(), "mensagens.yml");
		if (!(msgFile.exists()))
			ap.saveResource("mensagens.yml", false);
		
		FileConfiguration msg = YamlConfiguration.loadConfiguration(msgFile);
		for (String key : msg.getKeys(false)) {
			String message = "";
			
			if (msg.isList(key)) {
				for (String line : msg.getStringList(key))
					message += line.replace('&', '\u00a7') + "\n";
			}
			else
				message = msg.getString(key).replace('&', '\u00a7');
			
			MESSAGES.put(key, message);
		}
	}
	
	public static void sendMessage(CommandSender sender, String key) {
		sender.sendMessage(MESSAGES.get(key));
	}
	
	public static String getMessage(String key) {
		return MESSAGES.get(key);
	}

}
