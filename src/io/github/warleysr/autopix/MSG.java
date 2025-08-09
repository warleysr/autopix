package io.github.warleysr.autopix;

import java.io.File;
import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

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
					message += translateColors(line) + "\n";
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
	
	private static boolean isPaper() {
	    try {
	        Class.forName("com.destroystokyo.paper.PaperConfig");
	        return true;
	    } catch (ClassNotFoundException e) {
	        return false;
	    }
	}
	
	private static String translateColors(String message) {
		if (!isPaper())
			return ChatColor.translateAlternateColorCodes('&', message);
		
		message = legacyToMiniMessage(message);
		Component component = MiniMessage.miniMessage().deserialize(message);
		return LegacyComponentSerializer.legacySection().serialize(component);
	}
	
    private static String legacyToMiniMessage(String legacyText) {
        return legacyText
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }
}
