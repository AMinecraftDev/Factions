package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.persist.MemoryAccess;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

/**
 * @author AMinecraftDev
 * @version 1.6.9.5-U0.1.21
 * @since 13-May-17
 */
public class CmdAccess extends FCommand {

    public CmdAccess() {
        super();

        this.aliases.add("access");

        //this.requiredArgs.add("");
        this.requiredArgs.add("f/p/list/clear");
        this.optionalArgs.put("name", "");
        this.optionalArgs.put("yes/no/all/none", "");

        this.permission = Permission.ACCESS.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_ACCESS_DESCRIPTION;
    }

    @Override
    public void perform() {
        Location location = fme.getPlayer().getLocation();
        FLocation fLocation = new FLocation(location);
        Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
        Faction myFaction = fme.getFaction();

        if(myFaction != factionAtLocation) {
            msg(TL.COMMAND_ACCESS_NOTYOURLAND);
            return;
        }



        String target = this.argAsString(0);
        Access access = new MemoryAccess(fLocation, myFaction, fme);

        if(target.equalsIgnoreCase("f")) {
            Faction faction = this.argAsFaction(1);
            String arg = this.argAsString(2);

            if(faction == null) return;
            if(arg.equalsIgnoreCase("yes")) access.addAccess(faction, true);
            if(arg.equalsIgnoreCase("no")) access.removeAccess(faction, true);
            if(arg.equalsIgnoreCase("all")) access.addAccess(faction, false);
            if(arg.equalsIgnoreCase("none")) access.removeAccess(faction, false);

        } else if(target.equalsIgnoreCase("p")) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args.get(1));
            String arg = this.argAsString(2);

            if(!offlinePlayer.hasPlayedBefore()) return;
            if(arg.equalsIgnoreCase("yes")) access.addAccess(offlinePlayer, true);
            if(arg.equalsIgnoreCase("no")) access.removeAccess(offlinePlayer, true);
            if(arg.equalsIgnoreCase("all")) access.addAccess(offlinePlayer, false);
            if(arg.equalsIgnoreCase("none")) access.removeAccess(offlinePlayer, false);

        } else if(target.equalsIgnoreCase("list")) {
            List<String> p = access.getPlayerAccessList(fLocation);
            List<String> f = access.getFactionAccessList(fLocation);
            String fList = "", pList = "";
            int pAmount = 0, fAmount = 0;

            if(p.isEmpty() && f.isEmpty()) {
                msg(TL.COMMAND_ACCESS_NOONEHASACCESS);
                return;
            }

            if(p.isEmpty()) {
                pList = "N/A.";
            } else {
                for(String s : p) {
                    pList += FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(UUID.fromString(s))).getName();
                    pAmount++;

                    if(p.size() == pAmount) {
                        pList += ".";
                    } else {
                        pList += ", ";
                    }
                }
            }

            if(f.isEmpty()) {
                fList = "N/A.";
            } else {
                for(String s : f) {
                    fList += Factions.getInstance().getFactionById(s).getTag();
                    fAmount++;

                    if(f.size() == fAmount) {
                        fList += ".";
                    } else {
                        fList += ", ";
                    }
                }
            }

            String message = TL.COMMAND_ACCESS_LIST.toString();

            message = message.replace("{pamount}", ""+pAmount);
            message = message.replace("{famount}", "" + fAmount);
            message = message.replace("{plist}", pList);
            message = message.replace("{flist}", fList);

            if(message.contains("\n")) {
                for(String s : message.split("\n")) {
                    me.sendMessage(s);
                }
            } else {
                me.sendMessage(message);
            }

            return;
        } else if(target.equalsIgnoreCase("clear")) {
            msg(TL.COMMAND_ACCESS_CLEAR);
            access.clearAccess();
            return;
        }
    }
}
