package ru.cubelife.clearworld;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
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
					Player pl = (Player) p;
					if(pl.hasPermission("clearworld.antidel")) {
						continue; // Приступаем к следующему этапу цикла, если игрок имеет право на иммунитет к удалению своих регионов
					}
					long lastPlayed = p.getLastPlayed();
					long now = System.currentTimeMillis();
					if(now - ClearWorld.time >= lastPlayed) {
						i++;
					}
				}
				if(i >= dd.getPlayers().size()) {
					m.removeRegion(rg.getId());
					if(!ClearWorld.regen) {
						continue; // Приступаем к следующему этапу цикла, если не нужна регенерация
					}

					LocalWorld lw = new BukkitWorld(w);
					lw.regenerate((Region) rg, new EditSession(lw, Integer.MAX_VALUE)); // Регенирируем
				}
			}
		}
	}

}
