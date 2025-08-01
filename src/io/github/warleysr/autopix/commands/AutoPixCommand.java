package io.github.warleysr.autopix.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.MSG;
import io.github.warleysr.autopix.OrderManager;
import io.github.warleysr.autopix.TimeManager;
import io.github.warleysr.autopix.domain.DonorInfo;
import io.github.warleysr.autopix.domain.Order;
import io.github.warleysr.autopix.domain.PixData;
import io.github.warleysr.autopix.inventory.InventoryManager;
import io.github.warleysr.autopix.mercadopago.MPValidator;
import io.github.warleysr.autopix.mercadopago.MercadoPagoAPI;

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
				if (AutoPix.getInstance().getConfig().getBoolean("automatico.ativado")) {
					return false;
				}
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
			} else if (args[0].equalsIgnoreCase("cancelar")) {
				if (!(sender instanceof Player)) {
					MSG.sendMessage(sender, "in-game");
					return false;
				}
				Player p = (Player) sender;
				if (!TimeManager.canExecute(AutoPix.getInstance(), p, "cancel")) {
					return false;
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						Order order = OrderManager.getLastOrder(p.getName());
						if (order == null || order.isValidated()) {
							MSG.sendMessage(p, "sem-pedido-pendente");
							return;
						}

						PixData pd = OrderManager.getPixData(order);
						if (pd == null || !pd.isPending()) {
							MSG.sendMessage(p, "sem-pedido-pendente");
							return;
						}

						try {
							boolean cancelled = MercadoPagoAPI.cancelPayment(AutoPix.getInstance(), pd.getPaymentId());
							if (!cancelled) {
								MSG.sendMessage(p, "erro-cancelar");
								return;
							}

							if (!OrderManager.setPixDataStatus(pd, "cancelled")) {
								MSG.sendMessage(p, "erro-cancelar");
								return;
							}

							InventoryManager.removeUnpaidMaps(p);
							MSG.sendMessage(p, "cancelado");

						} catch (Exception e) {
							e.printStackTrace();
							MSG.sendMessage(p, "erro-cancelar");
						}
					}
				}.runTaskAsynchronously(AutoPix.getInstance());
				
				return false;
				
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("autopix.admin")) {
					MSG.sendMessage(sender, "sem-permissao");
					return false;
				}
				AutoPix.reloadPlugin();
				MSG.sendMessage(sender, "reload-executado");
				return false;
			} else if (args[0].equalsIgnoreCase("top")) {
				if (sender instanceof Player && !TimeManager.canExecute(AutoPix.getInstance(), (Player) sender, "top"))
					return false;

				new Thread(() -> {
					List<DonorInfo> topDonors = OrderManager.getTopDonors();

					if (topDonors.size() == 0) {
						MSG.sendMessage(sender, "sem-doadores");
						return;
					}
					
					StringBuilder message = new StringBuilder();
					message.append(MSG.getMessage("cabecalho-top"));
					message.append("\n");
					
					for (DonorInfo info : topDonors) {
						message.append(MSG.getMessage("corpo-top")
								.replace("{doador}", info.getDonor())
								.replace("{total}", String.format("%.2f", info.getTotal()).replace('.', ','))
								);
						message.append("\n");
					}
					message.append("\n");
					
					sender.sendMessage(message.toString());
					
				}).start();
				return false;
			}
		}

		// Caso nenhum subcomando seja identificado, envia a mensagem de ajuda.
		MSG.sendMessage(sender, "ajuda-autopix");
		return false;
	}
	
	private void sendOrderList(final CommandSender sender, final String player) {
		FileConfiguration config = AutoPix.getInstance().getConfig();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<Order> orders = OrderManager.getOrders(player);
				if (orders.isEmpty()) {
					MSG.sendMessage(sender, "sem-ordens");
					return;
				}
				StringBuilder message = new StringBuilder();
				message.append(MSG.getMessage("cabecalho"));
				message.append("\n");
				for (Order ord : orders) {
					String status = ord.isValidated() ? "approved" : "pending";
					
					if (config.getBoolean("automatico.ativado")) {
						PixData pd = OrderManager.getPixData(ord);
						if (pd != null)
							status = pd.getStatus();
					}
					else {
						long diff = System.currentTimeMillis() - ord.getCreated().getTime();
						long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
						
						if (minutes >= config.getInt("mapa.tempo-pagar"))
							status = "cancelled";
					}
					
					message.append(MSG.getMessage("corpo").replace("{id}", Integer.toString(ord.getId()))
							.replace("{data}", DATE_FORMAT.format(new Date(ord.getCreated().getTime())))
							.replace("{preco}", String.format("%.2f", ord.getPrice()).replace('.', ','))
							.replace("{produto}", ord.getProduct())
							.replace("{status}", getPrettyStatus(status)));
					message.append("\n");
				}

				message.append("\n");
				sender.sendMessage(message.toString());
			}
		}).start();
	}
	
	private String getPrettyStatus(String status) {
		switch (status) {
		case "approved":
			return "\u00a7aAPROVADO";
		case "pending":
			return "\u00a7ePENDENTE";
		case "cancelled":
			return "\u00a7cCANCELADO";
		default:
			return "\u00a77INDEFINIDO";
		}
	}

}
