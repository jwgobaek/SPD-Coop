/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PhantomPiranhaSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PhantomPiranha extends Piranha {

	{
		spriteClass = PhantomPiranhaSprite.class;

		loot = PhantomMeat.class;
		lootChance = 1f;
	}

	@Override
	public void damage(int dmg, Object src) {
		Char dmgSource = null;
		if (src instanceof Char) dmgSource = (Char)src;
		if (src instanceof Wand || src instanceof ClericSpell) {
			if (Dungeon.coop == null) dmgSource=Dungeon.hero;
			else if (src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile)
				dmgSource=((com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile)src).chargingHero();
		}

		if (dmgSource == null || !Dungeon.level.adjacent(pos, dmgSource.pos)){
			dmg = Math.round(dmg/2f); //halve damage taken if we are going to teleport
		}
		super.damage(dmg, src);

		if (isAlive() && !(src instanceof Corruption)) {
			if (dmgSource != null) {
				if (!Dungeon.level.adjacent(pos, dmgSource.pos)) {
					ArrayList<Integer> candidates = new ArrayList<>();
					for (int i : PathFinder.NEIGHBOURS8) {
						if (Dungeon.level.water[dmgSource.pos + i] && Actor.findChar(dmgSource.pos + i) == null) {
							candidates.add(dmgSource.pos + i);
						}
					}
					if (!candidates.isEmpty()) {
						ScrollOfTeleportation.appear(this, Random.element(candidates));
						aggro(dmgSource);
					} else {
						teleportAway();
					}
				}
			} else {
				teleportAway();
			}
		}
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		return super.defenseProc(enemy, damage);
	}

	@Override
	public void dieOnLand() {
		if (!teleportAway()){
			super.dieOnLand();
		}
	}

	private boolean teleportAway(){

		if (flying){
			return false;
		}

		ArrayList<Integer> inFOVCandidates = new ArrayList<>();
		ArrayList<Integer> outFOVCandidates = new ArrayList<>();
		boolean[] seen=Dungeon.level.heroFOV;
		if (Dungeon.coop!=null) {
			seen=new boolean[Dungeon.level.length()];
			boolean[] direct=new boolean[seen.length];
			for (int seat=0;seat<2;seat++) {
				if (!Dungeon.coop.hero(seat).isAlive()) continue;
				Dungeon.level.updateDirectSight(Dungeon.coop.hero(seat),direct);
				for (int p=0;p<seen.length;p++) seen[p] |= direct[p];
			}
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.water[i] && Actor.findChar(i) == null){
				if (seen[i]){
					inFOVCandidates.add(i);
				} else {
					outFOVCandidates.add(i);
				}
			}
		}

		if (!outFOVCandidates.isEmpty()){
			if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "teleport_away"));
			ScrollOfTeleportation.appear(this, Random.element(outFOVCandidates));
			return true;
		} else if (!inFOVCandidates.isEmpty()){
			ScrollOfTeleportation.appear(this, Random.element(inFOVCandidates));
			return true;
		}

		return false;

	}
}
