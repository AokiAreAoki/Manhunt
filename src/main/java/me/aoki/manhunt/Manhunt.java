package me.aoki.manhunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Manhunt extends JavaPlugin {
	static TargetFinder tf;

	@Override
	public void onEnable(){
		tf = new TargetFinder( this );
		getLogger().info( "Enabled" );
	}

	@Override
	public void onDisable(){
		tf = null;
		getLogger().info( "Disabled" );
	}

	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args ){
		return tf.onCommand( sender, cmd, args );
	}
}