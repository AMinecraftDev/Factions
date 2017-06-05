package com.massivecraft.factions;

import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Set;

/**
 * @author AMinecraftDev
 * @version 1.6.9.5-U0.1.21
 * @since 14-May-17
 */
public interface Access {

    void msg(FPlayer fPlayer, TL tl, Object... args);

    boolean doesAccessExist();

    boolean doesAccessExist(FLocation fLocation);

    List<String> getFactionAccessList(FLocation fLocation);

    List<String> getPlayerAccessList(FLocation fLocation);

    void setFactionAccessList(FLocation fLocation, List<String> list);

    void setFPlayerAccessList(FLocation fLocation, List<String> list);

    Set<OfflinePlayer> getPlayersWithAccess();

    Set<Faction> getFactionsWithAccess();

    void clearOnUnclaim();

    void clearAccess();

    void removeAccess(Faction faction, boolean currentChunkOnly);

    void removeAccess(OfflinePlayer fPlayer, boolean currentChunkOnly);

    void addAccess(Faction faction, boolean currentChunkOnly);

    void addAccess(OfflinePlayer fPlayer, boolean currentChunkOnly);

}
