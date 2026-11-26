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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;

/** Native damage wands whose impact path accepts an explicit acting hero. */
public abstract class TargetedDamageWand extends DamageWand {
	@Override
	public void onZap(Ballistica bolt) {
		onZap(curUser, bolt);
	}

	public final Hero chargingHero() {
		return charger != null && charger.target instanceof Hero ? (Hero)charger.target : null;
	}

	public final boolean cast(Hero caster, Ballistica bolt) {
		if (caster != chargingHero() || !tryToZap(caster, bolt.collisionPos)) return false;
		caster.busy();
		applyBackupBarrier(caster);
		onZap(caster, bolt);
		wandUsed(caster);
		cursedKnown = true;
		return true;
	}

	public abstract void onZap(Hero caster, Ballistica bolt);
}
