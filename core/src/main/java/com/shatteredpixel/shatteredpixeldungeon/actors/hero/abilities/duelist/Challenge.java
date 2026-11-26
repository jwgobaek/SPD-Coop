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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

public class Challenge extends ArmorAbility {

	{
		baseChargeUse = 50;
	}

	@Override
	public int icon() {
		return HeroIcon.CHALLENGE;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	@Override
	public float chargeUse( Hero hero ) {
		float chargeUse = super.chargeUse(hero);
		if (hero.buff(EliminationMatchTracker.class) != null){
			//reduced charge use by 16%/30%/41%/50%
			chargeUse *= Math.pow(0.84, hero.pointsInTalent(Talent.ELIMINATION_MATCH));
		}
		return chargeUse;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		challenge(armor,hero,target);
	}

	public boolean challenge(ClassArmor armor, Hero hero, Integer target){
		if (target == null){
			return false;
		}

		Char targetCh = Actor.findChar(target);
		if (targetCh == null || !(Dungeon.coop==null ? Dungeon.level.heroFOV[target] : Dungeon.coop.knowledge(hero).visible(target))){
			GLog.w(Messages.get(this, "no_target"));
			return false;
		}

		if (Dungeon.coop != null) {
			for (Char c : Actor.chars()) if (c.buff(DuelParticipant.class)!=null) return false;
		}
		if (hero.buff(DuelParticipant.class) != null){
			GLog.w(Messages.get(this, "already_dueling"));
			return false;
		}

		if (targetCh.alignment != Char.Alignment.ENEMY
				&& !(targetCh instanceof Mimic && targetCh.alignment == Char.Alignment.NEUTRAL)){
			GLog.w(Messages.get(this, "ally_target"));
			return false;
		}

		boolean[] passable = BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null);
		for (Char c : Actor.chars()) {
			if (c != hero) passable[c.pos] = false;
		}
		PathFinder.buildDistanceMap(targetCh.pos, passable);
		int[] reachable = PathFinder.distance.clone();

		int blinkpos = hero.pos;
		if (hero.hasTalent(Talent.CLOSE_THE_GAP) && !hero.rooted){

			int blinkrange = 1 + hero.pointsInTalent(Talent.CLOSE_THE_GAP);
			PathFinder.buildDistanceMap(hero.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), blinkrange);

			for (int i = 0; i < PathFinder.distance.length; i++){
				if (PathFinder.distance[i] == Integer.MAX_VALUE
						|| reachable[i] == Integer.MAX_VALUE
						|| (!Dungeon.level.passable[i] && !(hero.flying && Dungeon.level.avoid[i]))
						|| i == targetCh.pos || Dungeon.coop!=null && (Actor.findChar(i)!=null && i!=hero.pos || !com.shatteredpixel.shatteredpixeldungeon.CoopSession.supportedLanding(i))){
					continue;
				}

				if (Dungeon.level.distance(i, targetCh.pos) < Dungeon.level.distance(blinkpos, targetCh.pos)){
					blinkpos = i;
				} else if (Dungeon.level.distance(i, targetCh.pos) == Dungeon.level.distance(blinkpos, targetCh.pos)){
					if (Dungeon.level.trueDistance(i, hero.pos) < Dungeon.level.trueDistance(blinkpos, hero.pos)){
						blinkpos = i;
					}
				}
			}
		}

		if (reachable[blinkpos] == Integer.MAX_VALUE){
			GLog.w(Messages.get(this, "unreachable_target"));
			if (hero.rooted) PixelScene.shake( 1, 1f );
			return false;
		}

		if (Dungeon.level.distance(blinkpos, targetCh.pos) > 5){
			GLog.w(Messages.get(this, "distant_target"));
			if (hero.rooted) PixelScene.shake( 1, 1f );
			return false;
		}

		if (blinkpos != hero.pos){
			hero.pos = blinkpos;
			Dungeon.level.occupyCell(hero);
			//prevents the hero from being interrupted by seeing new enemies
			if (Dungeon.coop==null) Dungeon.observe();
			if (Dungeon.coop==null) GameScene.updateFog();
			hero.checkVisibleMobs();

			if (Dungeon.coop==null) hero.sprite.place(hero.pos);
			if (Dungeon.coop==null) CellEmitter.get(hero.pos).burst( Speck.factory( Speck.WOOL ), 6 );
			Sample.INSTANCE.play( Assets.Sounds.PUFF );
		}

		boolean bossTarget = Char.hasProp(targetCh, Char.Property.BOSS);
		for (Char toFreeze : Actor.chars()){
			if (toFreeze != targetCh && toFreeze.alignment != Char.Alignment.ALLY && !(toFreeze instanceof NPC)
				&& (!bossTarget || !(Char.hasProp(targetCh, Char.Property.BOSS) || Char.hasProp(targetCh, Char.Property.BOSS_MINION)))) {
				Actor.delayChar(toFreeze, DuelParticipant.DURATION);
				Buff.affect(toFreeze, SpectatorFreeze.class, DuelParticipant.DURATION);
			}
		}

		Buff.affect(targetCh, DuelParticipant.class).bind(hero,hero);
		Buff.affect(hero, DuelParticipant.class).bind(hero,targetCh);
		if (targetCh instanceof Mob){
			((Mob) targetCh).aggro(hero);
		}

		if (Dungeon.coop==null) GameScene.flash(0x80FFFFFF);
		Sample.INSTANCE.play(Assets.Sounds.DESCEND);

		armor.charge -= chargeUse( hero );
		armor.updateQuickslot();
		Invisibility.dispel(hero);
		if (Dungeon.coop==null) hero.sprite.zap(target);

		hero.next();

		if (hero.buff(EliminationMatchTracker.class) != null){
			hero.buff(EliminationMatchTracker.class).detach();
		}
		return true;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.CLOSE_THE_GAP, Talent.INVIGORATING_VICTORY, Talent.ELIMINATION_MATCH, Talent.HEROIC_ENERGY};
	}

	public static class EliminationMatchTracker extends FlavourBuff{};

	public static class DuelParticipant extends Buff {

		public static float DURATION = 10f;

		private int left = (int)DURATION;
		private int takenDmg = 0;
		private int ownerId, opponentId;

		private void bind(Hero hero, Char opponent) {
			ownerId=hero.id();
			opponentId=opponent.id();
		}

		@Override
		public int icon() {
			return BuffIndicator.CHALLENGE;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - left) / DURATION);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		public void addDamage(int dmg){
			takenDmg += dmg;
		}

		@Override
		public boolean act() {

			left--;
			if (left == 0) {
				detach();
			} else {
				Char other = null;
				for (Char ch : Actor.chars()){
					if (ch != target && ch.buff(DuelParticipant.class) != null && (Dungeon.coop==null || ch.id()==opponentId)){
						other = ch;
					}
				}

				if (other == null || Dungeon.coop!=null && (!other.isAlive() || !target.isAlive())
					|| target.alignment == other.alignment
					|| Dungeon.level.distance(target.pos, other.pos) > 5) {
					detach();
				}
			}

			spend(TICK);
			return true;
		}

		@Override
		public void detach() {
			Hero owner=Dungeon.hero;
			if (Dungeon.coop!=null) {
				owner=Dungeon.coop.hero(0).id()==ownerId ? Dungeon.coop.hero(0) : Dungeon.coop.hero(1);
				if (owner.id()!=ownerId) throw new IllegalStateException("Missing Challenge owner");
			}
			super.detach();
			if (target != owner){
				if (!target.isAlive() || target.alignment == owner.alignment){
					Sample.INSTANCE.play(Assets.Sounds.BOSS);

					if (owner.hasTalent(Talent.INVIGORATING_VICTORY)){
						DuelParticipant heroBuff = owner.buff(DuelParticipant.class);

						int hpToHeal = 0;
						if (heroBuff != null){
							hpToHeal = heroBuff.takenDmg;
						}

						//heals for 30%/50%/65%/75% of taken damage plus 5/10/15/20 bonus, based on talent points
						hpToHeal = (int)Math.round(hpToHeal * (1f - Math.pow(0.707f, owner.pointsInTalent(Talent.INVIGORATING_VICTORY))));
						hpToHeal += 5*owner.pointsInTalent(Talent.INVIGORATING_VICTORY);
						hpToHeal = Math.min(hpToHeal, owner.HT - owner.HP);
						if (hpToHeal > 0){
							owner.HP += hpToHeal;
							if (Dungeon.coop==null) owner.sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.33f, 6 );
							if (Dungeon.coop==null) owner.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hpToHeal), FloatingText.HEALING );
						}
					}
				}

			} else {
				if (owner.isAlive()) {
					if (Dungeon.coop==null) GameScene.flash(0x80FFFFFF);

					if (owner.hasTalent(Talent.ELIMINATION_MATCH)){
						Buff.affect(target, EliminationMatchTracker.class, 3);
					}
				}
			}

			for (Char ch : Actor.chars()) {
				if (ch.buff(SpectatorFreeze.class) != null) {
					ch.buff(SpectatorFreeze.class).detach();
				}
				if (ch.buff(DuelParticipant.class) != null && ch != target) {
					ch.buff(DuelParticipant.class).detach();
				}
			}
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", left);
		}

		private static final String LEFT = "left";
		private static final String TAKEN_DMG = "taken_dmg";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(TAKEN_DMG, takenDmg);
			if (Dungeon.coop!=null) { bundle.put("duelist", ownerId); bundle.put("opponent", opponentId); }
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			takenDmg = bundle.getInt(TAKEN_DMG);
			ownerId=bundle.getInt("duelist"); opponentId=bundle.getInt("opponent");
		}
	}

	public static class SpectatorFreeze extends FlavourBuff {

		@Override
		public void fx(boolean on) {
			if (on) {
				target.sprite.add(CharSprite.State.DARKENED);
				target.sprite.add(CharSprite.State.PARALYSED);
			} else {
				//allies can't be spectator frozen, so just check doom
				if (target.buff(Doom.class) == null) target.sprite.remove(CharSprite.State.DARKENED);
				if (target.paralysed == 0) target.sprite.remove(CharSprite.State.PARALYSED);
			}
		}

		@Override
		public void detach(){
			super.detach();
			if (cooldown() > 0) {
				Actor.delayChar(target, -cooldown());
			}
		}

		{
			immunities.addAll(new BlobImmunity().immunities());
		}

	}
}
