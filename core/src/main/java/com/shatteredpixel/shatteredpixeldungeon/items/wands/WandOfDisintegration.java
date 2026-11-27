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

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfDisintegration extends TargetedDamageWand {

	{
		image = ItemSpriteSheet.WAND_DISINTEGRATION;

		collisionProperties = Ballistica.WONT_STOP;
	}


	public int min(int lvl){
		return 2+lvl;
	}

	public int max(int lvl){
		return 8+4*lvl;
	}
	
	@Override
	public int targetingPos(Hero user, int dst) {
		if (!cursed || !cursedKnown) {
			return dst;
		} else {
			return super.targetingPos(user, dst);
		}
	}

	@Override
	public void onZap(Hero caster, Ballistica beam) {
		
		boolean terrainAffected = false;
		
		int level = buffedLvl();
		
		int maxDistance = Math.min(distance(), beam.dist);
		
		ArrayList<Char> chars = new ArrayList<>();

		com.shatteredpixel.shatteredpixeldungeon.FloorKnowledge knowledge = Dungeon.coop == null ? null : Dungeon.coop.knowledge(caster);

		int terrainPassed = 2, terrainBonus = 0;
		for (int c : beam.subPath(1, maxDistance)) {
			
			Char ch;
			if ((ch = Actor.findChar( c )) != null) {

				//we don't want to count passed terrain after the last enemy hit. That would be a lot of bonus levels.
				//terrainPassed starts at 2, equivalent of rounding up when /3 for integer arithmetic.
				terrainBonus += terrainPassed/3;
				terrainPassed = terrainPassed%3;

				if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE
						&& !(knowledge == null ? Dungeon.level.mapped[c] || Dungeon.level.visited[c] : knowledge.mapped(c) || knowledge.visited(c))){
					//avoid harming undiscovered passive chars
				} else {
					chars.add(ch);
				}
			}

			if (Dungeon.level.solid[c]) {
				terrainPassed++;
			}

			if (Dungeon.level.flamable[c]) {

				Dungeon.level.destroy( c );
				GameScene.updateMap( c );
				terrainAffected = true;
				
			}
			
			if (Dungeon.coop == null) CellEmitter.center( c ).burst( PurpleParticle.BURST, Random.IntRange( 1, 2 ) );
		}
		
		if (terrainAffected && Dungeon.coop == null) {
			Dungeon.observe();
		}
		
		int lvl = level + (chars.size()-1) + terrainBonus;
		for (Char ch : chars) {
			wandProc(caster, ch, buffedLvl(), chargesPerCast());
			ch.damage( damageRoll(caster, lvl), this );
			if (Dungeon.coop == null) {
				ch.sprite.centerEmitter().burst( PurpleParticle.BURST, Random.IntRange( 1, 2 ) );
				ch.sprite.flash();
			}
		}
	}

    public boolean safeBeam(Hero caster, Ballistica ray, boolean volley) {
        // A volley can empower a later wand or move a blocker. Do not roll its level during validation.
        int end = volley ? ray.dist : Math.min(ray.dist, distance());
        return safeBeamTo(caster, ray, end);
    }

    public boolean safeFormBeam(Hero caster, Ballistica ray, int formLevel) {
        return safeBeamTo(caster, ray, Math.min(ray.dist, distanceAtLevel(formLevel)));
    }

    private boolean safeBeamTo(Hero caster, Ballistica ray, int end) {
        Hero teammate = Dungeon.coop.hero(caster == Dungeon.coop.hero(0) ? 1 : 0);
        return teammate == null || !ray.subPath(1, end).contains(teammate.pos);
    }

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no direct effect, see magesStaff.reachfactor
	}

	private int distance() {
		return distanceAtLevel(buffedLvl());
	}

    private int distanceAtLevel(int level) { return 2 * level + 6; }

	@Override
	public String upgradeStat2(int level) {
		return Integer.toString(6 + level*2);
	}

	@Override
	public void fx(Ballistica beam, Callback callback) {
		
		int cell = beam.path.get(Math.min(beam.dist, distance()));
		curUser.sprite.parent.add(new Beam.DeathRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( cell )));
		Sample.INSTANCE.play( Assets.Sounds.RAY );
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x220022);
		particle.am = 0.6f;
		particle.setLifespan(1f);
		particle.acc.set(10, -10);
		particle.setSize( 0.5f, 3f);
		particle.shuffleXY(1f);
	}

}
