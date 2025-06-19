package io.github.warleysr.autopix.qrcode;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import io.github.warleysr.autopix.AutoPix;
import io.github.warleysr.autopix.domain.OrderProduct;
import io.github.warleysr.autopix.inventory.InventoryManager;

public class ImageCreator {

    public static BufferedImage generateQR(String data) 
    		throws UnsupportedEncodingException, WriterException {
    	
    	BitMatrix matrix = new MultiFormatWriter()
        		.encode(new String(data.getBytes("UTF-8"), "UTF-8"), BarcodeFormat.QR_CODE, 128, 128);
    	
    	BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
    	
    	return image;
    }

    @SuppressWarnings("deprecation")
	public static void generateMap(final BufferedImage image, Player p, OrderProduct op) {	
    	
    	String mapMaterial = Material.getMaterial("FILLED_MAP") != null ? "FILLED_MAP" : "MAP";
    	
        ItemStack itemStack = new ItemStack(Material.getMaterial(mapMaterial));
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        MapView mapView = Bukkit.createMap(p.getWorld());
        mapView.setScale(MapView.Scale.CLOSEST);
        try {
        	MapView.class.getMethod("setUnlimitedTracking", boolean.class);
        	mapView.setUnlimitedTracking(true);
        } catch (NoSuchMethodException e) {}
        
        mapView.getRenderers().clear();

        mapView.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                mapCanvas.drawImage(0, 0, image);
            }
        });
        
        try {
        	MapMeta.class.getMethod("setMapView", MapView.class);
        	mapMeta.setMapView(mapView);
        } catch (NoSuchMethodException e) {
        	itemStack.setDurability(getMapID(mapView));
        }
        
        InventoryManager.updateMapMeta(AutoPix.getInstance(), mapMeta, p, op);
        
        itemStack.setItemMeta(mapMeta);

        p.getInventory().setItemInHand(itemStack);
    }
    
    public static Class<?> getMapNMS(String name) {
        try {
            return Class.forName("org.bukkit.map." + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static short getMapID(MapView view) {
      try {
            return (short) view.getId();
        } catch (NoSuchMethodError e) {
            try {
                Class<?> MapView = getMapNMS("MapView");
                Object mapID = MapView.getMethod("getId").invoke(view);
                return (short)mapID;
            } catch (Exception ex) { return 1; }
        }
    }
}