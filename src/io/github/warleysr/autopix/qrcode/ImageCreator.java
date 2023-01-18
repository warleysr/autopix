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

public class ImageCreator {

    public static BufferedImage generateQR(String data) 
    		throws UnsupportedEncodingException, WriterException {
    	
    	BitMatrix matrix = new MultiFormatWriter()
        		.encode(new String(data.getBytes("UTF-8"), "UTF-8"), BarcodeFormat.QR_CODE, 128, 128);
    	
    	BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
    	
    	return image;
    }

    public static void generateMap(final BufferedImage image, Player p) {
        ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        MapView mapView = Bukkit.createMap(p.getWorld());
        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setUnlimitedTracking(true);
        mapView.getRenderers().clear();

        mapView.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                mapCanvas.drawImage(0, 0, image);
            }
        });

        mapMeta.setMapView(mapView);
        itemStack.setItemMeta(mapMeta);

        p.getInventory().setItemInMainHand(itemStack);
    }
}