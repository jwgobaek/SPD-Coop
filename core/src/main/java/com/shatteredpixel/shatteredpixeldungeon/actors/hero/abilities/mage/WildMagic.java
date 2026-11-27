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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WildMagic extends ArmorAbility {

	{
		baseChargeUse = 25f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null){
			return;
		}

		if (target == hero.pos){
			GLog.w(Messages.get(this, "self_target"));
			return;
		}

		ArrayList<Wand> wands = firingList(hero);
		if (wands.size() == 0){
			GLog.w(Messages.get(this, "no_wands"));
			return;
		}

		hero.busy();

		Random.shuffle(wands);

		Buff.affect(hero, WildMagicTracker.class, 0f);

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		zapWand(wands, hero, target);

	}

	private static float shotCost(Hero hero) {
		return 0.5f * (float)Math.pow(0.67f, hero.pointsInTalent(Talent.CONSERVED_MAGIC));
	}

	private static ArrayList<Wand> firingList(Hero hero) {
		ArrayList<Wand> wands = hero.belongings.getAllItems(Wand.class);
		Random.shuffle(wands);

		float chargeUsePerShot = shotCost(hero);

		for (Wand w : wands.toArray(new Wand[0])){
			if (w.curCharges < 1 && w.partialCharge < chargeUsePerShot){
				wands.remove(w);
			}
		}

		int maxWands = 4 + hero.pointsInTalent(Talent.FIRE_EVERYTHING);

		//second and third shots
		if (wands.size() < maxWands){
			ArrayList<Wand> seconds = new ArrayList<>(wands);
			ArrayList<Wand> thirds = new ArrayList<>(wands);

			for (Wand w : wands){
				float totalCharge = w.curCharges + w.partialCharge;
				if (totalCharge < 2*chargeUsePerShot){
					seconds.remove(w);
				}
				if (totalCharge < 3*chargeUsePerShot
					|| Random.Int(4) >= hero.pointsInTalent(Talent.FIRE_EVERYTHING)){
					thirds.remove(w);
				}
			}

			Random.shuffle(seconds);
			while (!seconds.isEmpty() && wands.size() < maxWands){
				wands.add(seconds.remove(0));
			}

			Random.shuffle(thirds);
			while (!thirds.isEmpty() && wands.size() < maxWands){
				wands.add(thirds.remove(0));
			}
		} else {
			while (wands.size() > maxWands){
				wands.remove(0);
			}
		}

		return wands;
	}

	/** Resolve the native volley on the host before its action receipt can be published. */
	public boolean useForSeat(ClassArmor armor, Hero hero, int cell) {
		if (hero.pos == cell || hero.belongings.getItem(WondrousResin.class) != null) return false;
		boolean usable = false;
		for (Wand wand : hero.belongings.getAllItems(Wand.class)) {
			if (wand.curCharges < 1 && wand.partialCharge < shotCost(hero)) continue;
			if (!com.shatteredpixel.shatteredpixeldungeon.CoopSession.supportedHandheldWand(wand)
					|| Dungeon.coop.ownership.owner(wand) != Dungeon.coop.playerOf(hero)
					|| ((com.shatteredpixel.shatteredpixeldungeon.items.wands.TargetedDamageWand)wand).chargingHero() != hero) return false;
			if (wand instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration
                    && !((com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration)wand).safeBeam(
                            hero,new Ballistica(hero.pos,cell,Ballistica.WONT_STOP),true)) return false;
            if (wand instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost) {
				// Earlier shots can kill or move a blocker. Check each possible frost impact.
				Ballistica path = new Ballistica(hero.pos, cell, wand.collisionProperties(cell) & ~Ballistica.STOP_CHARS);
				for (int i = 1; i <= path.dist; i++) {
					int pos = path.path.get(i);
					if ((i == path.dist || Dungeon.level.heaps.get(pos) != null)
							&& !com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost.safeImpact(pos)) return false;
				}
			}
			usable = true;
		}
		if (!usable) return false;

		ArrayList<Wand> shots = firingList(hero);
		hero.busy();
		Random.shuffle(shots);
		Buff.affect(hero, WildMagicTracker.class, 0f);
		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		for (Wand wand : shots) {
			Ballistica aim = new Ballistica(hero.pos, cell, wand.collisionProperties(cell));
			if (wand.tryToZap(hero,cell)) {
				((com.shatteredpixel.shatteredpixeldungeon.items.wands.TargetedDamageWand)wand).onZap(hero,aim);
				Random.Float(); // Native extra curse check, with no Wondrous Resin equipped.
			}
			spendShot(wand,hero);
			if (!hero.isAlive()) break;
		}
		finishVolley(hero);
		return true;
	}

	public static class WildMagicTracker extends FlavourBuff{};

	Actor wildMagicActor = null;

	private void zapWand( ArrayList<Wand> wands, Hero hero, int cell){
		Wand cur = wands.remove(0);

		Ballistica aim = new Ballistica(hero.pos, cell, cur.collisionProperties(cell));

		hero.sprite.zap(cell);

		float startTime = Game.timeTotal;
		if (cur.tryToZap(hero, cell)) {
			if (!cur.cursed) {
				cur.fx(aim, new Callback() {
					@Override
					public void call() {
						cur.onZap(aim);
						boolean alsoCursedZap = Random.Float() < WondrousResin.extraCurseEffectChance();
						if (Game.timeTotal - startTime < 0.33f) {
							hero.sprite.parent.add(new Delayer(0.33f - (Game.timeTotal - startTime)) {
								@Override
								protected void onComplete() {
									if (alsoCursedZap){
										WondrousResin.forcePositive = true;
										CursedWand.cursedZap(cur,
												hero,
												new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT),
												new Callback() {
													@Override
													public void call() {
														WondrousResin.forcePositive = false;
														afterZap(cur, wands, hero, cell);
													}
												});
									} else {
										afterZap(cur, wands, hero, cell);
									}
								}
							});
						} else {
							if (alsoCursedZap){
								WondrousResin.forcePositive = true;
								CursedWand.cursedZap(cur,
										hero,
										new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT),
										new Callback() {
											@Override
											public void call() {
												WondrousResin.forcePositive = false;
												afterZap(cur, wands, hero, cell);
											}
										});
							} else {
								afterZap(cur, wands, hero, cell);
							}
						}
					}
				});

			} else {
				CursedWand.cursedZap(cur,
						hero,
						new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT),
						new Callback() {
							@Override
							public void call() {
								if (Game.timeTotal - startTime < 0.33f) {
									hero.sprite.parent.add(new Delayer(0.33f - (Game.timeTotal - startTime)) {
										@Override
										protected void onComplete() {
											afterZap(cur, wands, hero, cell);
										}
									});
								} else {
									afterZap(cur, wands, hero, cell);
								}
							}
						});
			}
		} else {
			afterZap(cur, wands, hero, cell);
		}
	}

	private void afterZap( Wand cur, ArrayList<Wand> wands, Hero hero, int target){
		spendShot(cur,hero);
		if (wildMagicActor != null){
			wildMagicActor.next();
			wildMagicActor = null;
		}

		Char ch = Actor.findChar(target);
		if (!wands.isEmpty() && hero.isAlive()) {
			Actor.add(new Actor() {
				{
					actPriority = VFX_PRIO-1;
				}

				@Override
				protected boolean act() {
					wildMagicActor = this;
					zapWand(wands, hero, ch == null ? target : ch.pos);
					Actor.remove(this);
					return false;
				}
			});
			hero.next();
		} else {
			finishVolley(hero);
		}
	}

	private static void spendShot(Wand cur, Hero hero) {
		cur.partialCharge -= shotCost(hero);
		if (cur.partialCharge < 0) {
			cur.partialCharge++;
			cur.curCharges--;
		}
	}

	private static void finishVolley(Hero hero) {
		if (hero.buff(WildMagicTracker.class) != null) {
			hero.buff(WildMagicTracker.class).detach();
		}
		Item.updateQuickslot();
		Invisibility.dispel(hero);
		if (Random.Int(4) >= hero.pointsInTalent(Talent.CONSERVED_MAGIC)) {
			hero.spendAndNext(Actor.TICK);
		} else {
			hero.next();
		}
	}

	@Override
	public int icon() {
		return HeroIcon.WILD_MAGIC;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.WILD_POWER, Talent.FIRE_EVERYTHING, Talent.CONSERVED_MAGIC, Talent.HEROIC_ENERGY};
	}
}
