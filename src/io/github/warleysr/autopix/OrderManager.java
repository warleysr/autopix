package io.github.warleysr.autopix;

import java.awt.image.BufferedImage;
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
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.warleysr.autopix.mercadopago.MercadoPagoAPI;
import io.github.warleysr.autopix.qrcode.ImageCreator;

public class OrderManager {
	
	private static Connection conn;
	
	protected static void startOrderManager(AutoPix ap) throws SQLException {
		FileConfiguration cfg = ap.getConfig();
		
		String host = cfg.getString("mysql.host").trim(), user = cfg.getString("mysql.user").trim(), 
			   pass = cfg.getString("mysql.pass").trim(), db = cfg.getString("mysql.db").trim();
		int port = cfg.getInt("mysql.port");
		
		String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true&characterEncoding=utf8";
		conn = DriverManager.getConnection(url, user, pass);
		
		conn.prepareStatement("CREATE TABLE IF NOT EXISTS autopix_orders "
				+ "(id INT PRIMARY KEY AUTO_INCREMENT, player VARCHAR(16) NOT NULL,"
				+ "product VARCHAR(16) NOT NULL, price DECIMAL(10, 2) NOT NULL, "
				+ "created TIMESTAMP NOT NULL, pix VARCHAR(32) UNIQUE NULL);").executeUpdate();
		
		conn.prepareStatement("CREATE TABLE IF NOT EXISTS autopix_pendings " 
				+ "(id VARCHAR(32) PRIMARY KEY, player VARCHAR(16) NOT NULL);").executeUpdate();
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
			
			Order ord = new Order(p.getName(), product, price, 0L);
			
			return ord;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
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
				int id = rs.getInt(1);
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
	
	protected static void validatePendings(AutoPix ap) {
		try {
			PreparedStatement st = conn.prepareStatement("SELECT * FROM autopix_pendings;");
			ResultSet rs = st.executeQuery();
			
			while (rs.next()) {
				String player = rs.getString("player");
				if (Bukkit.getPlayerExact(player) == null) continue;
				
				String id = rs.getString("id");
				
				Object[] data = MercadoPagoAPI.getPayment(ap, id);
				if (data == null) continue;
				
				String pixId = (String) data[0];
				double paid = (double) data[1];
				
				for (Order order : getOrders(player)) {
					if (order.isValidated()) continue;
					if (Math.abs(order.getPrice() - paid) > 0.001) continue;
					
					if (!(OrderManager.setTransaction(order, pixId))) continue;
					
					deletePending(id);
					
					new BukkitRunnable() {
						@SuppressWarnings("deprecation")
						@Override
						public void run() {
							Player p = Bukkit.getPlayerExact(player);
							
							try {
								String mapMaterial = AutoPix.getRunningVersion() >= 1013 ? "FILLED_MAP" : "MAP";
								if (p.getItemInHand().getType().name() == mapMaterial) {
									BufferedImage gif = ImageIO.read(AutoPix.getInstance().getResource("success.png"));
									ImageCreator.generateMap(gif, p, null);
								}
							} catch (Exception e) {}
							
							if (ap.getConfig().getBoolean("som.ativar")) {
								try {
									Sound sound = Sound.valueOf(
											ap.getConfig().getString("som.efeito").toUpperCase());
									p.playSound(p, sound, 1, 1);
								} catch (Exception e) {}
							}
							
							for (String cmd : ap.getConfig().getStringList("menu.produtos." 
									+ order.getProduct() + ".comandos")) {
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
										cmd.replace("{player}", p.getName()).replace('&', '\u00a7'));
							}
						}
					}.runTask(ap);
					break;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void deletePending(String id) throws SQLException {
		PreparedStatement st = conn.prepareStatement("DELETE FROM autopix_pendings WHERE id = ?;");
		st.setString(1, id);
		st.executeUpdate();
	}

}
