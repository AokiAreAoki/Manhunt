package me.aoki.manhunt;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

final class ManhuntCommand {
	interface CommandCallback {
		void run( Player ply, Command cmd, String[] args );
	}

	public static HashMap<String, ManhuntCommand> commands = new HashMap<>();
	private final CommandCallback callback;
	private final String description;

	public ManhuntCommand( String description, CommandCallback callback ){
		this.description = description;
		this.callback = callback;
	}

	public String getDescription(){
		return description;
	}

	public void run( Player player, Command command, String[] args ){
		callback.run( player, command, args );
	}
	
	// Statics
	public static void add( String name, String args, String description, CommandCallback callback ){
		String usage = String.format(
			"%s/mh %s%s%s: %s",
			ChatColor.GOLD,
			name,
			args == null || args.length() == 0 ? "" : " " + args,
			ChatColor.RESET,
			description
		);
		
		commands.put( name, new ManhuntCommand( usage, callback ) );
	}
	
	public static ManhuntCommand getCommand( String name ){
		return commands.get( name );
	}
	
	public static boolean run( String name, Player player, Command command, String[] args ){
		ManhuntCommand cmd = getCommand( name );
		
		if( cmd == null )
			return false;
		
		cmd.run( player, command, args );
		return true;
	}
	
	public static String getDescription( String name ){
		return getCommand( name ).getDescription();
	}
}

public final class TargetFinder implements Listener {
    final String prefix = "" + ChatColor.GRAY + '[' + ChatColor.RED + "Manhunt" + ChatColor.GRAY + ']' + ChatColor.WHITE;
    final JavaPlugin plugin;
	Location lastLoc;
	Player target;

	// Init
	public TargetFinder( JavaPlugin plugin ){
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents( this, plugin );
        plugin.getConfig().addDefault( "commandsisoponly", true );

        ManhuntCommand.add(
            "set",
            "<player>",
	        "sets a target",
	        ( ply, cmd, args ) -> {
		        if( !plugin.getConfig().getBoolean( "commandsisoponly" ) || ply.isOp() ){
			        if( args.length == 1 )
				        ply.sendMessage( ChatColor.RED + "Provide a player name!" );
			        else {
				        for( Player pl : plugin.getServer().getOnlinePlayers() )
					        if( pl.getName().equalsIgnoreCase( args[1] ) ){
						        target = pl;
						        broadcastMessage( ChatColor.GREEN + "" + ChatColor.BOLD + ply.getName() + ChatColor.RESET + " is now a target." );
						        giveCompassToEveryone();
						        return;
					        }
				
				        ply.sendMessage( ChatColor.RED + "Player " + ChatColor.BOLD + args[1] + ChatColor.RESET + ChatColor.RED + " not found!" );
			        }
		        } else
			        ply.sendMessage( ChatColor.RED + "You don't have permission to use this command." );
	        }
        );
		
		ManhuntCommand.add(
			"unset",
			null,
			"unsets a target",
			( ply, cmd, args ) -> {
				if( !plugin.getConfig().getBoolean( "commandsisoponly" ) || ply.isOp() ){
					if( target == null )
						ply.sendMessage( "" + ChatColor.RED + "Target is not set." );
					else {
						target = null;
						broadcastMessage( ChatColor.GREEN + "Target" + ChatColor.WHITE + " unset." );
					}
				} else
					ply.sendMessage( ChatColor.RED + "You don't have permission to use this command." );
			}
		);
		
		ManhuntCommand.add(
			"isoponly",
			"<true/false>",
			String.format( "/mh isoponly <%1$strue%3$s/%2$sfalse%3$s>", ChatColor.GREEN, ChatColor.RED, ChatColor.GOLD ),
			( ply, cmd, args ) -> {
				if( ply.isOp() ){
					if( args.length < 2 ) {
						ply.sendMessage( String.format( "%3$s/mh isoponly <%1$strue%3$s/%2$sfalse%3$s>", ChatColor.GREEN, ChatColor.RED, ChatColor.GOLD ) );
						return;
					}
					
					String value = args[1].toLowerCase();
					boolean bValue;
					
					if( value.equals( "true" ) )
						bValue = true;
					else if( value.equals( "false" ) )
						bValue = false;
					else {
						ply.sendMessage( ChatColor.RED + "Invalid value!\n" + ChatColor.GREEN + "true" + ChatColor.WHITE + " and " + ChatColor.RED + "false" + ChatColor.WHITE + " is only acceptable values." );
						return;
					}
					
					plugin.getConfig().set( "commandsisoponly", bValue );
					plugin.saveConfig();
					
					broadcastMessage( ChatColor.GRAY + ply.getName()
						+ ChatColor.RESET + " changed value of "
						+ ChatColor.YELLOW + "isoponly"
						+ ChatColor.RESET + " parameter to "
						+ ( bValue ? ChatColor.GREEN : ChatColor.RED ) + value
					);
				} else
					ply.sendMessage( ChatColor.RED + "You don't have permission to use this command." );
			}
		);
	}
	
	// Some Methods
	public void displayHelp( Player ply ){
		StringBuilder help = new StringBuilder( prefix + " options:" );
		
		ManhuntCommand.commands.forEach( ( name, cmd ) -> {
			String description = cmd.getDescription();
			
			if( description == null || description.length() == 0 )
				description = "No description set :(";
			
			help.append( '\n' ).append( description );
		});
		
		ply.sendMessage( help.toString() );
	}
	
	public void broadcastMessage( String msg ){
    	msg = prefix + ' ' + msg;
    	
    	plugin.getServer().getConsoleSender().sendMessage( msg );
    	
    	for( Player ply : plugin.getServer().getOnlinePlayers() )
    		ply.sendMessage( msg );
    }
		
	public void giveCompass( Player ply ){
    	if( target != null && ply != target && !ply.getInventory().contains( Material.COMPASS ) ){
    		if( ply.getInventory().getItem( 8 ) == null )
    			ply.getInventory().setItem( 8, new ItemStack( Material.COMPASS, 1 ) );
    		else
    			ply.getInventory().addItem( new ItemStack( Material.COMPASS, 1 ) );
    	}
    }
    
    public void giveCompassToEveryone(){
    	if( target != null )
    		for( Player ply : plugin.getServer().getOnlinePlayers() )
    		    giveCompass( ply );
    }
    
    // Events
	@EventHandler
    public void PlayerCompass( PlayerInteractEvent e ){
    	if( target == null ) return;
    	
    	Player ply = e.getPlayer();
    	Action act = e.getAction();
    	ItemStack item = e.getItem();
    	
    	if( act.equals( Action.RIGHT_CLICK_BLOCK ) || act.equals( Action.RIGHT_CLICK_AIR ) ){
    		if( item != null && item.getType() == Material.COMPASS && ply.getWorld().getEnvironment().equals( World.Environment.NORMAL ) ){
    			ply.sendMessage( prefix + " Compass is now pointing at " + ChatColor.GREEN + ChatColor.BOLD + target.getName() );
    			ply.setCompassTarget( target.getWorld().getEnvironment().equals( World.Environment.NETHER ) ? lastLoc : target.getLocation() );
    		}
		}
    }
    
    @EventHandler
    public void PlayerRespawn( PlayerRespawnEvent e ){
    	giveCompass( e.getPlayer() );
    }
    
    @EventHandler
    public void PlayerDied( EntityDeathEvent e ){
    	if( target != null && e.getEntity() instanceof Player ){
    		Player ply = (Player) e.getEntity();
    		
    		if( ply == target ){
    			broadcastMessage( "" + ChatColor.GREEN + ChatColor.BOLD + ply.getName() + ChatColor.RESET + " was " + ChatColor.RED + ChatColor.BOLD + "killed!" );
    			target = null;
    		} else
			    e.getDrops().removeIf( item -> item.getType() == Material.COMPASS );
    	}
    }
    
    @EventHandler
    public void PlayerChangeWorld( PlayerTeleportEvent e ){
    	if( target == null )
    		return;
    	
    	if( !e.getFrom().getWorld().equals( e.getTo().getWorld() ) )
    		if( !e.getTo().getWorld().getEnvironment().equals( World.Environment.NORMAL ) )
    		    lastLoc = e.getPlayer().getLocation();
    }

    // Commands
    public boolean onCommand( CommandSender sender, Command cmd, String[] args ){
		if( cmd.getName().equalsIgnoreCase( "mh" ) && sender instanceof Player ){
			Player ply = (Player) sender;
			
			if( args.length == 0 ){
				displayHelp( ply );
				return true;
			}

			String name = args[0].toLowerCase();
			
			if( !ManhuntCommand.run( name, ply, cmd, args ) )
				ply.sendMessage( String.format( "%sUnknown option \"%s\". /mh - for help", ChatColor.RED, name ) );
			
			return true;
		}

		return false;
	}
}