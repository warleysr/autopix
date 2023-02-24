package io.github.warleysr.autopix.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.Order;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.TimeManager;
import io.github.warleysr.autopix.inventory.InventoryManager;
import io.github.warleysr.autopix.qrcode.MPValidator;

public class AutoPixCommand implements CommandExecutor {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String command, String[] args) {
		if (args.length >= 1) {
			/* Checando os subcommands */
			if (args[0].equalsIgnoreCase("info")) {
				if (!(sender instanceof Player)) {
					MSG.sendMessage(sender, "in-game");
					return false;
				}
				InventoryManager.openInfo((Player) sender);
				return true;
			} else if (args[0].equalsIgnoreCase("lista")) {
				if (args.length == 1) {
					if (!(sender instanceof Player)) {
						MSG.sendMessage(sender, "ajuda-lista");
						return false;
					}
					if (!TimeManager.canExecute(AutoPix.getInstance(), (Player) sender, "list")) {
						return false;
					}
					sendOrderList(sender, sender.getName());
					return true;
				}
				if (!sender.hasPermission("autopix.admin")) {
					MSG.sendMessage(sender, "sem-permissao");
					return false;
				}
				sendOrderList(sender, args[1]);
				return true;
			} else if (args[0].equalsIgnoreCase("validar")) {
				if (!(sender instanceof Player)) {
					MSG.sendMessage(sender, "in-game");
					return false;
				}
				Player player = (Player) sender;
				if (args.length == 1) {
					MSG.sendMessage(player, "ajuda-validar");
					return false;
				}
				String pixId = args[1];
				if (pixId.length() != 32 || pixId.charAt(0) != 'E') {
					MSG.sendMessage(player, "pix-invalido");
					return false;
				}
				if (!TimeManager.canExecute(AutoPix.getInstance(), player, "validate")) {
					return false;
				}
				if (OrderManager.isTransactionValidated(pixId)) {
					MSG.sendMessage(player, "ja-validado");
					return false;
				}
				MSG.sendMessage(player, "validando");
				new Thread(() -> {
					MPValidator.validateTransaction(AutoPix.getInstance(), pixId, player);
				}).start();
				return true;
			}
		}

		// Caso nenhum subcomando seja identificado, envia a mensagem de ajuda.
		MSG.sendMessage(sender, "ajuda-autopix");
		return false;
	}
	
	private void sendOrderList(final CommandSender sender, final String player) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<Order> orders = OrderManager.getOrders(player);
				if (orders.isEmpty()) {
					MSG.sendMessage(sender, "sem-ordens");
					return;
				}
				String msg = MSG.getMessage("cabecalho") + "\n";
				for (Order ord : orders)
					msg += MSG.getMessage("corpo")
							.replace("{id}", Integer.toString(ord.getId()))
							.replace("{data}", DATE_FORMAT.format(new Date(ord.getCreated().getTime())))
							.replace("{preco}", Float.toString(ord.getPrice()))
							.replace("{produto}", ord.getProduct())
							.replace("{status}", ord.isValidated() ? "\u00a7aAPROVADO" : "\u00a7ePENDENTE") + "\n";
				
				msg += "\n";
				sender.sendMessage(msg);
			}
		}).start();
	}

}
