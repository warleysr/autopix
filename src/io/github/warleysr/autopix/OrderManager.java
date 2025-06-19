package io.github.warleysr.autopix;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.warleysr.autopix.domain.DonorInfo;
import io.github.warleysr.autopix.domain.Order;
import io.github.warleysr.autopix.domain.OrderProduct;
import io.github.warleysr.autopix.domain.PaymentInfo;
import io.github.warleysr.autopix.domain.PixData;
import io.github.warleysr.autopix.inventory.InventoryManager;
import io.github.warleysr.autopix.mercadopago.MercadoPagoAPI;
import io.github.warleysr.autopix.qrcode.ImageCreator;

public class OrderManager {
	
	private static Connection conn;
	
	protected static boolean startOrderManager(AutoPix ap) throws SQLException {
		FileConfiguration cfg = ap.getConfig();
		
		String type = cfg.getString("database.type");
		String autoIncrement = null;
		
		if (type.equalsIgnoreCase("mysql")) {
			String host = cfg.getString("database.host").trim(), user = cfg.getString("database.user").trim(), 
				   pass = cfg.getString("database.pass").trim(), db = cfg.getString("database.db").trim();
			int port = cfg.getInt("database.port");
			autoIncrement = "AUTO_INCREMENT";
			
			String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true&characterEncoding=utf8&useSSL=false";
			conn = DriverManager.getConnection(url, user, pass);
		}
		else if (type.equalsIgnoreCase("sqlite")) {
			autoIncrement = "AUTOINCREMENT";
			File flatFile = new File(ap.getDataFolder(), "autopix.db");
			conn = DriverManager.getConnection("jdbc:sqlite:" + flatFile.getAbsolutePath());
		}
		else {
			MSG.sendMessage(Bukkit.getConsoleSender(), "db-invalido");
			return false;
		}
		
		conn.prepareStatement("CREATE TABLE IF NOT EXISTS autopix_orders "
				+ "(id INTEGER PRIMARY KEY " + autoIncrement + ", player VARCHAR(16) NOT NULL,"
				+ "product VARCHAR(16) NOT NULL, price DECIMAL(10, 2) NOT NULL, "
				+ "created TIMESTAMP NOT NULL, pix VARCHAR(32) UNIQUE NULL);").executeUpdate();
		
		conn.prepareStatement("CREATE TABLE IF NOT EXISTS autopix_pix_data " 
				+ "(payment_id VARCHAR(128) PRIMARY KEY, order_id INTEGER NOT NULL UNIQUE, " 
				+ "status VARCHAR(16), qr_code VARCHAR(512) NOT NULL UNIQUE);").executeUpdate();
		
		return true;
	}
	
	public static Order createOrder(Player p, String product, float price) {
		try {
			
			PreparedStatement ps = conn.prepareStatement("INSERT INTO autopix_orders "
					+ "(player, product, price, created) VALUES (?, ?, ?, ?);");
			ps.setString(1, p.getName());
			ps.setString(2, product);
			ps.setFloat(3, price);
			ps.setTimestamp(4, Timestamp.from(Instant.now()));
			
			ps.executeUpdate();
			
			
			return getLastOrder(p.getName());
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean savePixData(PixData pixData) {
		try {
			
			PreparedStatement ps = conn.prepareStatement("INSERT INTO autopix_pix_data "
					+ "(payment_id, order_id, status, qr_code) VALUES (?, ?, ?, ?);");
			
			ps.setString(1, pixData.getPaymentId());
			ps.setInt(2, pixData.getOrderId());
			ps.setString(3, pixData.getStatus());
			ps.setString(4, pixData.getQrCode());
			ps.executeUpdate();
			
			return true;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static List<Order> getOrders(String player) {
		return getOrders(player, 10);
	}
	
	public static List<Order> getOrders(String player, int limit) {
		List<Order> orders = new ArrayList<>();
		
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM autopix_orders WHERE player = ? " + 
														 "ORDER BY created DESC LIMIT ?;");
			ps.setString(1, player);
			ps.setInt(2, limit);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt("id");
				String player_real = rs.getString("player");
				String product = rs.getString("product");
				float price = rs.getFloat("price");
				Timestamp created = rs.getTimestamp("created");
				String transaction = rs.getString("pix");
				
				Order ord = new Order(player_real, product, price, created.getTime());
				ord.setId(id);
				ord.setTransaction(transaction);
				
				orders.add(ord);
			}
			
			return orders;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return orders;
	}
	
	public static Order getLastOrder(String player) {
		List<Order> orders = getOrders(player, 1);
		return orders.isEmpty() ? null : orders.get(0);
	}
	
	public static Order getOrderById(int orderId) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM autopix_orders WHERE id = ?;");
			ps.setInt(1, orderId);
			
			ResultSet rs = ps.executeQuery();
			if (!(rs.next())) return null;
			
			int id = rs.getInt("id");
			String player_real = rs.getString("player");
			String product = rs.getString("product");
			float price = rs.getFloat("price");
			Timestamp created = rs.getTimestamp("created");
			String transaction = rs.getString("pix");
			
			Order order = new Order(player_real, product, price, created.getTime());
			order.setId(id);
			order.setTransaction(transaction);
			
			return order;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static List<PixData> getAllPendingPixData() {
		List<PixData> data = new ArrayList<>();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM autopix_pix_data WHERE status = 'pending';");
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String paymentId = rs.getString("payment_id");
				int orderId = rs.getInt("order_id");
				String status = rs.getString("status");
				String qrCode = rs.getString("qr_code");
				
				PixData pd = new PixData(paymentId, qrCode);
				pd.setOrderId(orderId);
				pd.setStatus(status);
				
				data.add(pd);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public static boolean isTransactionValidated(String transactionId) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT id FROM autopix_orders WHERE pix = ?;");
			ps.setString(1, transactionId);
			return ps.executeQuery().next();
		} catch (SQLException e) {
			e.printStackTrace();
			return true;
		}
	}
	
	public static boolean setTransaction(Order ord, String transactionId) {
		try {
			PreparedStatement ps = conn.prepareStatement("UPDATE autopix_orders SET pix = ? WHERE id = ?;");
			ps.setString(1, transactionId);
			ps.setInt(2, ord.getId());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean setPixDataStatus(PixData pixData, String status) {
		try {
			PreparedStatement ps = conn.prepareStatement("UPDATE autopix_pix_data SET status = ? WHERE payment_id = ?;");
			ps.setString(1, status);
			ps.setString(2, pixData.getPaymentId());
			ps.executeUpdate();
			pixData.setStatus(status);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	protected static void validatePendings(AutoPix ap) {
		for (PixData pd : getAllPendingPixData()) {
			
			Order order = getOrderById(pd.getOrderId());
			
			if (order == null) continue;
			if (order.isValidated()) continue;
			
			Player p = Bukkit.getPlayerExact(order.getPlayer());
			if (p == null) continue;
			
			OrderProduct op = InventoryManager.getProductByOrder(order);
			if (op == null) continue;
			
			PaymentInfo info = MercadoPagoAPI.getPayment(ap, pd.getPaymentId());
			if (info == null) continue;
			
			if (!setPixDataStatus(pd, info.getStatus())) continue;
			
			if (!info.getStatus().equals("approved")) continue;
			if (Math.abs(order.getPrice() - info.getPaidAmount()) > 0.001) continue;
			
			if (!setTransaction(order, info.getTransactionId())) continue;

			new BukkitRunnable() {
				@SuppressWarnings("deprecation")
				@Override
				public void run() {	
					try {
						String mapMaterial = Material.getMaterial("FILLED_MAP")!= null ? "FILLED_MAP" : "MAP";
						if (p.getItemInHand().getType().name() == mapMaterial) {
							BufferedImage gif = ImageIO.read(AutoPix.getInstance().getResource("success.png"));
							ImageCreator.generateMap(gif, p, null);
						}
					} catch (Exception e) {}
					
					if (ap.getConfig().getBoolean("som.ativar")) {
						try {
							Sound sound = Sound.valueOf(
									ap.getConfig().getString("som.efeito").toUpperCase());
							p.playSound(p.getLocation(), sound, 1, 1);
						} catch (Exception e) {}
					}
					
					for (String cmd : op.getCommands())
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
								cmd.replace("{player}", p.getName()).replace('&', '\u00a7'));
				}
			}.runTask(ap);
		}
	}
	
	public static List<DonorInfo> getTopDonors(){
		ArrayList<DonorInfo> topDonors = new ArrayList<>();
		
		try {
			PreparedStatement st = conn.prepareStatement(
					"SELECT DISTINCT player AS donor, "
					+ "(SELECT SUM(price) FROM autopix_orders WHERE player = donor AND pix != 'NULL') "
					+ "AS total FROM autopix_orders WHERE pix != 'NULL' ORDER BY total DESC LIMIT 5;");
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				topDonors.add(new DonorInfo(rs.getString("donor"), rs.getFloat("total")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return topDonors;
	}

}
