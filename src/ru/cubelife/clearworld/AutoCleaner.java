package ru.cubelife.clearworld;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class AutoCleaner extends Thread {
	
	/** Выполняется в новом потоке */
	public void run() {
		while(ClearWorld.enabled) {
			cleanAll(); // Вызываем чистку
			try {
				Thread.sleep(3600000); // После выполнения чистки ждем 1 час
			}
			catch (Exception e) { }
		}
	}
	
	/** Проверяет все регионы и чистит при необходимости */
	private void cleanAll() {
		WorldGuardPlugin wg = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard"); //Берем WorldGuard
		for(World w : Bukkit.getWorlds()) {
			RegionManager m = wg.getRegionManager(w);
			for(ProtectedRegion rg : m.getRegions().values()) {
				DefaultDomain dd = rg.getOwners();
				int i = 0;
				for(String pName : dd.getPlayers()) {
					OfflinePlayer p = Bukkit.getOfflinePlayer(pName);
					if(!p.hasPlayedBefore()) {
						continue;
					}
					Player pl = p.getPlayer();
					if(pl != null) {
						if(pl.hasPermission("clearworld.antidel")) {
							break; // Выходим из цикла, если игрок имеет право на иммунитет к удалению своих регионов
						}
					}
					List<String> ignPl = new ArrayList<String>();
					BufferedReader sr;
					try {
						sr = new BufferedReader(new InputStreamReader(new FileInputStream(ClearWorld.ign)));
						while(true) {
							String line = null;
							line = sr.readLine();
							if(line == null)
								break;
							ignPl.add(line);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					if(ignPl.contains(p.getName())) {
						break; // Выходим из цикла, если игрок в списке тех, кого трогать не нужно
					}
					long lastPlayed = p.getLastPlayed();
					long now = System.currentTimeMillis();
					if(now - ClearWorld.time >= lastPlayed) {
						i++;
					}
				}
				if(i >= dd.getPlayers().size()) {
					m.removeRegion(rg.getId());
					try {
						m.save();
					} catch (ProtectionDatabaseException e) {
						e.printStackTrace();
					}
					
					if(ClearWorld.regen) {
						LocalWorld lw = new BukkitWorld(w);
						lw.regenerate(new CuboidRegion(lw, rg.getMinimumPoint(), rg.getMaximumPoint()), new EditSession(lw, Integer.MAX_VALUE)); // Регенирируем
					}
					
					if(ClearWorld.lwc) {
						LWCPlugin lwc = (LWCPlugin) Bukkit.getPluginManager().getPlugin("LWC");
						int x = rg.getMinimumPoint().getBlockX();
						int y = rg.getMinimumPoint().getBlockY();
						int z = rg.getMinimumPoint().getBlockZ();
						int maxX = rg.getMaximumPoint().getBlockX();
						int maxY = rg.getMaximumPoint().getBlockY();
						int maxZ = rg.getMaximumPoint().getBlockZ();
						for(int ix = x;ix <= maxX; ix++) {
							for(int iy = y; iy <= maxY; iy++) {
								for(int iz = z; iz <= maxZ; iz++) {
									Protection pr = lwc.getLWC().findProtection(w, ix, iy, iz);
									if(pr != null) {
										pr.remove();
									}
								}
							}
						}
					}

				}
			}
		}
	}

}
