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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class SoulMark extends FlavourBuff {
	private int casterId;

	public static void applyFrom(Hero caster, Char target, float duration) {
		if (Dungeon.coop == null) {
			prolong(target, SoulMark.class, duration);
			return;
		}
		SoulMark existing = target.buff(SoulMark.class);
		// One native mark per enemy. A second caster cannot steal or extend the first caster's mark.
		if (existing != null && existing.casterId != caster.id()) return;
		SoulMark mark = prolong(target, SoulMark.class, duration);
		if (mark != null) mark.casterId = caster.id();
	}

	public static Hero source(Char target) {
		SoulMark mark = target.buff(SoulMark.class);
		if (mark == null) return null;
		if (Dungeon.coop == null) return Dungeon.hero;
		for (int seat = 0; seat < 2; seat++) {
			Hero hero = Dungeon.coop.hero(seat);
			if (hero != null && hero.id() == mark.casterId && hero.isAlive()) return hero;
		}
		return null;
	}

	@Override public void storeInBundle(Bundle data) {
		super.storeInBundle(data);
		if (casterId != 0) data.put("caster_id",casterId);
	}

	@Override public void restoreFromBundle(Bundle data) {
		super.restoreFromBundle(data);
		casterId = data.getInt("caster_id");
	}


	public static final float DURATION	= 10f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.INVERT_MARK;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 0.2f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (Dungeon.coop != null) return;
		if (on) target.sprite.add(CharSprite.State.MARKED);
		else target.sprite.remove(CharSprite.State.MARKED);
	}
}
