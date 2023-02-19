package io.github.warleysr.autopix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class OrderManager {
	
	private static Connection conn;
	
	protected static void startOrderManager(AutoPix ap) throws SQLException {
		FileConfiguration cfg = ap.getConfig();
		
		String host = cfg.getString("mysql.host").trim(), user = cfg.getString("mysql.user").trim(), 
			   pass = cfg.getString("mysql.pass").trim(), db = cfg.getString("mysql.db").trim();
		int port = cfg.getInt("mysql.port");
		
		String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?characterEncoding=utf8";
		conn = DriverManager.getConnection(url, user, pass);
		
		conn.prepareStatement("CREATE TABLE IF NOT EXISTS autopix_orders "
				+ "(id INT PRIMARY KEY AUTO_INCREMENT, player VARCHAR(16) NOT NULL,"
				+ "product VARCHAR(16) NOT NULL, price DECIMAL(10, 2) NOT NULL, "
				+ "created TIMESTAMP NOT NULL, pix VARCHAR(32) UNIQUE NULL);").executeUpdate();
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
		List<Order> orders = new ArrayList<>();
		
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM autopix_orders WHERE player = ? " + 
														 "ORDER BY created DESC LIMIT 10;");
			ps.setString(1, player);
			
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

}
