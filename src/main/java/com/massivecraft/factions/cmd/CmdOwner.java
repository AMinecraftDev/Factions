package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.SpiralTask;
import com.massivecraft.factions.zcore.util.TL;

import java.util.HashMap;
import java.util.Map;


public class CmdOwner extends FCommand {

    public CmdOwner() {
        super();
        this.aliases.add("owner");

        //this.requiredArgs.add("");
        this.optionalArgs.put("radius", "1");
        this.optionalArgs.put("player name", "you");

        this.permission = Permission.OWNER.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    // TODO: Fix colors!

    @Override
    public void perform() {
        boolean hasBypass = fme.isAdminBypassing();

        if (!hasBypass && !assertHasFaction()) {
            return;
        }

        if (!Conf.ownedAreasEnabled) {
            fme.msg(TL.COMMAND_OWNER_DISABLED);
            return;
        }

        if (!hasBypass && Conf.ownedAreasLimitPerFaction > 0 && myFaction.getCountOfClaimsWithOwners() >= Conf.ownedAreasLimitPerFaction) {
            fme.msg(TL.COMMAND_OWNER_LIMIT, Conf.ownedAreasLimitPerFaction);
            return;
        }

        if (!hasBypass && !assertMinRole(Conf.ownedAreasModeratorsCanSet ? Role.MODERATOR : Role.ADMIN)) {
            return;
        }

        int radius = this.argAsInt(0, 1); // Default to 1
        final FPlayer target = this.argAsBestFPlayerMatch(1, fme);

        if (target == null) {
            return;
        }

        String playerName = target.getName();

        if (target.getFaction() != myFaction) {
            fme.msg(TL.COMMAND_OWNER_NOTMEMBER, playerName);
            return;
        }

        if(radius < 1) {
            msg(TL.COMMAND_CLAIM_INVALIDRADIUS);
            return;
        }

        if(radius < 2) {
            // single chunk
            FLocation flocation = new FLocation(fme);
            Faction factionHere = Board.getInstance().getFactionAt(flocation);

            if (factionHere != myFaction) {
                if (!factionHere.isNormal()) {
                    fme.msg(TL.COMMAND_OWNER_NOTCLAIMED);
                    return;
                }

                if (!hasBypass) {
                    fme.msg(TL.COMMAND_OWNER_WRONGFACTION);
                    return;
                }
            }

            // if no player name was passed, and this claim does already have owners set, clear them
            if (args.isEmpty() && myFaction.doesLocationHaveOwnersSet(flocation)) {
                myFaction.clearClaimOwnership(flocation);
                fme.msg(TL.COMMAND_OWNER_CLEARED);
                return;
            }

            if (myFaction.isPlayerInOwnerList(target, flocation)) {
                myFaction.removePlayerAsOwner(target, flocation);
                fme.msg(TL.COMMAND_OWNER_REMOVED, playerName);
                return;
            }

            // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
            if (!payForCommand(Conf.econCostOwner, TL.COMMAND_OWNER_TOSET, TL.COMMAND_OWNER_FORSET)) {
                return;
            }

            myFaction.setPlayerAsOwner(target, flocation);
            fme.msg(TL.COMMAND_OWNER_ADDED, playerName);
            return;
        } else {
            // radius claim
            if (!Permission.CLAIM_RADIUS.has(sender, false)) {
                msg(TL.COMMAND_CLAIM_DENIED);
                return;
            }

            final boolean removeFromChunk = myFaction.isPlayerInOwnerList(target, new FLocation(fme));

            if(removeFromChunk) {
                fme.msg(TL.COMMAND_OWNER_REMOVEDRADIUS.toString()
                        .replace("{i}", ""+radius)
                        .replace("{p}", playerName));
            } else {
                fme.msg(TL.COMMAND_OWNER_ADDEDRADIUS.toString()
                        .replace("{i}", ""+radius)
                        .replace("{p}", playerName));
            }

            new SpiralTask(new FLocation(me), radius) {

                private int failCount = 0;
                private final int limit = Conf.radiusOwnerFailureLimit - 1;

                @Override
                public boolean work() {
                    FLocation currentLoc = this.currentFLocation();
                    Faction factionHere = Board.getInstance().getFactionAt(currentLoc);

                    if(factionHere == myFaction) {
                        if (removeFromChunk) {
                            if (myFaction.isPlayerInOwnerList(target, currentLoc)) {
                                myFaction.removePlayerAsOwner(target, currentLoc);
                            }
                        } else {
                            if (!myFaction.isPlayerInOwnerList(target, currentLoc)) {
                                myFaction.setPlayerAsOwner(target, currentLoc);
                            }
                        }
                    } else if (failCount++ >= limit) {
                        this.stop();
                        return false;
                    }

                    return true;
                }
            };
        }


    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_OWNER_DESCRIPTION;
    }
}
