package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Relation;

/**
 * @author AMinecraftDev
 * @version 1.6.9.5-U0.1.21
 * @since 15-May-17
 */
public class CmdRelationTruce extends FRelationCommand {

    public CmdRelationTruce() {
        aliases.add("truce");
        targetRelation = Relation.TRUCE;
    }
}