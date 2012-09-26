package ru.cubelife.clearworld;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearWorld extends JavaPlugin {
	
	/** Логгер */
	private Logger log;
	/** Конфиг */
	private FileConfiguration cfg;
	/** Файл конфига */
	private File cfgFile;
	/** Время в днях */
	private long t;
	
	/** Путь к файлу игроков, которые игнорируются */
	public static String ign;
	/** Включено ли? */
	public static boolean enabled;
	/** Время в миллисекундах */
	public static long time;
	/** Регенирировать ли? */
	public static boolean regen;
	/** Удалять ли приваты LWC? */
	public static boolean lwc; 
	
	/** Вызывается при включении */
	public void onEnable() {
		enabled = true;
		log = Logger.getLogger("Minecraft");
		cfgFile = new File(getDataFolder(), "config.yml");
		cfg = YamlConfiguration.loadConfiguration(cfgFile);
		loadCfg(); // Загружаем настройки
		saveCfg(); // Сохраняем настройки
		ign = getDataFolder() + "\\ignored-players.yml";
		File f = new File(ign);
		if(!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		PluginManager pm = getServer().getPluginManager();
		
		if(pm.getPlugin("WorldGuard") != null) {
			log("Using WorldGuard!");
		} else {
			log("WorldGuard not founded! Disabling..");
			this.setEnabled(false);
		}
		
		if(regen) {
			if(pm.getPlugin("WorldEdit") != null) {
				log("Using WorldEdit!");
			} else {
				log("WorldEdit not founded! Disabling regeneration..");
				regen = false;
				saveCfg();
			}
		}
		
		if(lwc) {
			if(pm.getPlugin("LWC") != null) {
				log("Using LWC!");
			} else {
				log("LWC not founded! Disabling LWC privates removing..");
				lwc = false;
				saveCfg();
			}
		}
		
		new AutoCleaner().start(); // Запускает в отдельном потоке AutoCleaner
		log("Enabled!");
	}
	
	/** Вызывается при выключении */
	public void onDisable() {
		enabled = false;
		log("Disabled!");
	}
	
	/** Логирует в консоль */
	private void log(String msg) {
		log.info("[ClearWorld] " + msg);
	}
	
	/** Загружает конфиг */
	private void loadCfg() {
		t = cfg.getInt("time", 30);
		time = (t * 24 * 3600 * 1000);
		regen = cfg.getBoolean("regen", false);
		lwc = cfg.getBoolean("lwc", false);
	}
	
	/** Сохраняет конфиг */
	private void saveCfg() {
		cfg.set("time", t);
		cfg.set("regen", regen);
		cfg.set("lwc", lwc);
		try {
			cfg.save(cfgFile);
		} catch (Exception e) { }
	}

}
