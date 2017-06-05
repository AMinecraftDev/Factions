package com.massivecraft.factions.zcore.persist;

import com.massivecraft.factions.*;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author AMinecraftDev
 * @version 1.6.9.5-U0.1.21
 * @since 14-May-17
 */
public class MemoryAccess implements Access {

    private static FileConfiguration ACCESSYML;
    private static File ACCESSFILE;
    private FLocation fLocation;
    private Faction faction;
    private FPlayer fPlayer;

    public MemoryAccess(FLocation newFLocation, Faction newFaction, FPlayer fPlayer) {
        this.fLocation = newFLocation;
        this.faction = newFaction;
        this.fPlayer = fPlayer;
    }

    public MemoryAccess(FLocation fLocation) {
        this.fLocation = fLocation;
    }

    @Override
    public void msg(FPlayer fPlayer, TL tl, Object... args) {
        String msg = tl.toString();

        for(Object obj : args) {
            if(obj instanceof Faction) {
                msg = msg.replace("{faction}", ((Faction) obj).getTag());
            }

            if(obj instanceof OfflinePlayer) {
                msg = msg.replace("{player}", ((OfflinePlayer) obj).getName());
            }

            if(obj instanceof String) {
                msg = msg.replace("{s}", ((String) obj));
            }

            if(obj instanceof Integer) {
                msg = msg.replace("{i}", ""+((Integer) obj));
            }

            if(obj instanceof FLocation) {
                FLocation fLocation = (FLocation) obj;
                long x = fLocation.getX() * 16;
                long z = fLocation.getZ() * 16;
                String s = x + ", " + z;

                msg = msg.replace("{location}", s);
            }
        }

        fPlayer.sendMessage(msg);
    }

    @Override
    public boolean doesAccessExist() {
        return doesAccessExist(this.fLocation);
    }

    @Override
    public boolean doesAccessExist(FLocation fLocation) {
        return ACCESSYML.contains(fLocation.toString());
    }

    @Override
    public List<String> getFactionAccessList(FLocation fLocation) {
        return ACCESSYML.getStringList(fLocation.toString() + ".F");
    }

    @Override
    public List<String> getPlayerAccessList(FLocation fLocation) {
        return ACCESSYML.getStringList(fLocation.toString() + ".P");
    }

    @Override
    public void setFactionAccessList(FLocation fLocation, List<String> list) {
        ACCESSYML.set(fLocation.toString() + ".F", list);
    }

    @Override
    public void setFPlayerAccessList(FLocation fLocation, List<String> list) {
        ACCESSYML.set(fLocation.toString() + ".P", list);
    }

    @Override
    public Set<OfflinePlayer> getPlayersWithAccess() {
        Set<OfflinePlayer> fPlayerSet = new HashSet<OfflinePlayer>();

        if(!doesAccessExist()) return fPlayerSet;

        List<String> stringList = getPlayerAccessList(this.fLocation);

        for(String s : stringList) {
            fPlayerSet.add(Bukkit.getOfflinePlayer(UUID.fromString(s)));
        }

        return fPlayerSet;
    }

    @Override
    public Set<Faction> getFactionsWithAccess() {
        Set<Faction> fPlayerSet = new HashSet<Faction>();

        if(!doesAccessExist()) return fPlayerSet;

        List<String> stringList = getFactionAccessList(this.fLocation);

        for(String s : stringList) {
            fPlayerSet.add(Factions.getInstance().getFactionById(s));
        }

        return fPlayerSet;
    }

    @Override
    public void clearOnUnclaim() {
        ACCESSYML.set(fLocation.toString(), null);
        saveAccessFile();
    }

    @Override
    public void clearAccess() {
        if(!doesAccessExist()) {
            msg(this.fPlayer, TL.COMMAND_ACCESS_NOONEHASACCESS);
            return;
        }

        List<String> p = getPlayerAccessList(this.fLocation);
        List<String> f = getFactionAccessList(this.fLocation);

        if(p.isEmpty() && f.isEmpty()) {
            msg(this.fPlayer, TL.COMMAND_ACCESS_NOONEHASACCESS);
            return;
        }

        ACCESSYML.set(fLocation.toString(), null);
        saveAccessFile();
    }

    @Override
    public void removeAccess(Faction faction, boolean currentChunkOnly) {
        Set<FLocation> chunksTargetted = new HashSet<FLocation>();
        boolean alreadyDoesntHaveAccess = true;

        if(currentChunkOnly) {
            chunksTargetted.add(fLocation);
        } else {
            chunksTargetted.addAll(faction.getAllClaims());
        }

        for(FLocation fLocation : chunksTargetted) {
            List<String> f = getFactionAccessList(fLocation);

            if(!f.contains(faction.getId())) {
                alreadyDoesntHaveAccess = false;
                continue;
            }

            f.remove(faction.getId());
            setFactionAccessList(fLocation, f);
        }

        saveAccessFile();

        if(currentChunkOnly) {
            if(!alreadyDoesntHaveAccess) {
                msg(this.fPlayer, TL.COMMAND_ACCESS_NO_FACTIONNOACCESS);
                return;
            }

            for(FPlayer onlineMembers : faction.getFPlayersWhereOnline(true)) {
                msg(onlineMembers, TL.COMMAND_ACCESS_NO_RECEIVED, this.faction, this.fLocation);
            }

            msg(this.fPlayer, TL.COMMAND_ACCESS_NO_FACTIONREMOVED, this.fLocation, faction);
        } else {

            for(FPlayer onlineMembers : faction.getFPlayersWhereOnline(true)) {
                msg(onlineMembers, TL.COMMAND_ACCESS_NONE_RECEIVED, this.faction);
            }

            msg(this.fPlayer, TL.COMMAND_ACCESS_NONE_REMOVED, faction.getTag());
        }
    }

    @Override
    public void removeAccess(OfflinePlayer fPlayer, boolean currentChunkOnly) {
        Set<FLocation> chunksTargetted = new HashSet<FLocation>();
        boolean alreadyDoesntHaveAccess = true;
        UUID uuid = fPlayer.getUniqueId();

        if(currentChunkOnly) {
            chunksTargetted.add(fLocation);
        } else {
            chunksTargetted.addAll(faction.getAllClaims());
        }

        for(FLocation fLocation : chunksTargetted) {
            List<String> p = getPlayerAccessList(fLocation);

            if(!p.contains(uuid.toString())) {
                alreadyDoesntHaveAccess = false;
                continue;
            }

            p.remove(uuid.toString());
            setFPlayerAccessList(fLocation, p);
        }

        saveAccessFile();

        if(currentChunkOnly) {
            if(!alreadyDoesntHaveAccess) {
                msg(this.fPlayer, TL.COMMAND_ACCESS_NO_PLAYERNOACCESS);
                return;
            }

            if(fPlayer.isOnline()) msg(FPlayers.getInstance().getByOfflinePlayer(fPlayer), TL.COMMAND_ACCESS_NO_RECEIVED, this.faction, this.fLocation);
            msg(this.fPlayer, TL.COMMAND_ACCESS_NO_PLAYERREMOVED, this.fLocation, fPlayer);
        } else {
            if(fPlayer.isOnline()) msg(FPlayers.getInstance().getByOfflinePlayer(fPlayer), TL.COMMAND_ACCESS_NONE_RECEIVED, this.faction);
            msg(this.fPlayer, TL.COMMAND_ACCESS_NONE_REMOVED, fPlayer.getName());
        }
    }

    @Override
    public void addAccess(Faction faction, boolean currentChunkOnly) {
        Set<FLocation> chunksTargetted = new HashSet<FLocation>();
        boolean alreadyHasAccess = false;

        if(currentChunkOnly) {
            chunksTargetted.add(fLocation);
        } else {
            chunksTargetted.addAll(faction.getAllClaims());
        }

        for(FLocation fLocation : chunksTargetted) {
            List<String> f = getFactionAccessList(fLocation);

            if(f.contains(faction.getId())) {
                alreadyHasAccess = true;
                continue;
            }

            f.add(faction.getId());
            setFactionAccessList(fLocation, f);
        }

        saveAccessFile();

        if(currentChunkOnly) {
            if(alreadyHasAccess) {
                msg(this.fPlayer, TL.COMMAND_ACCESS_YES_FACTIONALREADY);
                return;
            }

            for(FPlayer onlineMembers : faction.getFPlayersWhereOnline(true)) {
                msg(onlineMembers, TL.COMMAND_ACCESS_YES_RECEIVED, this.faction, this.fLocation);
            }

            msg(this.fPlayer, TL.COMMAND_ACCESS_YES_FACTIONADDED, this.fLocation, faction);
        } else {
            for(FPlayer onlineMembers : faction.getFPlayersWhereOnline(true)) {
                msg(onlineMembers, TL.COMMAND_ACCESS_ALL_RECEIVED, this.faction);
            }

            msg(this.fPlayer, TL.COMMAND_ACCESS_ALL_FACTION, faction);
        }
    }

    @Override
    public void addAccess(OfflinePlayer fPlayer, boolean currentChunkOnly) {
        Set<FLocation> chunksTargetted = new HashSet<FLocation>();
        String uuid = fPlayer.getUniqueId().toString();
        boolean alreadyHasAccess = false;

        if(currentChunkOnly) {
            chunksTargetted.add(fLocation);
        } else {
            chunksTargetted.addAll(faction.getAllClaims());
        }

        for(FLocation fLocation : chunksTargetted) {
            List<String> p = getPlayerAccessList(fLocation);

            if(p.contains(uuid)) {
                alreadyHasAccess = true;
                continue;
            }

            p.add(uuid);
            setFPlayerAccessList(fLocation, p);
        }

        saveAccessFile();

        if(currentChunkOnly) {
            if(alreadyHasAccess) {
                msg(this.fPlayer, TL.COMMAND_ACCESS_YES_PLAYERALREADY);
                return;
            }

            if(fPlayer.isOnline()) msg(FPlayers.getInstance().getByOfflinePlayer(fPlayer), TL.COMMAND_ACCESS_YES_RECEIVED, this.faction, this.fLocation);
            msg(this.fPlayer, TL.COMMAND_ACCESS_YES_PLAYERADDED, this.fLocation, fPlayer);
        } else {

            if(fPlayer.isOnline()) msg(FPlayers.getInstance().getByOfflinePlayer(fPlayer), TL.COMMAND_ACCESS_ALL_RECEIVED, this.faction);
            msg(this.fPlayer, TL.COMMAND_ACCESS_ALL_PLAYER, fPlayer);
        }
    }

    public static void setupAccessFile(JavaPlugin javaPlugin) {
        ACCESSFILE = new File(javaPlugin.getDataFolder(), "access.yml");

        if(!ACCESSFILE.exists()) javaPlugin.saveResource("access.yml", false);

        ACCESSYML = YamlConfiguration.loadConfiguration(ACCESSFILE);
    }

    public static void saveAccessFile() {
        try {
            ACCESSYML.save(ACCESSFILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
