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

package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import java.util.HashSet;

public abstract class Actor implements Bundlable {
	
	public static final float TICK	= 1f;

	private float time;

	private int id = 0;

	//default priority values for general actor categories
	//note that some specific actors pick more specific values
	//e.g. a buff acting before all normal buffs might have priority BUFF_PRIO + 1
	protected static final int VFX_PRIO    = 100;   //visual effects take priority
	protected static final int HERO_PRIO   = 0;     //positive is before hero, negative after
	protected static final int BLOB_PRIO   = -10;   //blobs act after hero, before mobs
	protected static final int MOB_PRIO    = -20;   //mobs act between buffs and blobs
	protected static final int BUFF_PRIO   = -30;   //buffs act last in a turn
	private static final int   DEFAULT     = -100;  //if no priority is given, act after all else

	//used to determine what order actors act in if their time is equal. Higher values act earlier.
	protected int actPriority = DEFAULT;

	protected abstract boolean act();

	//Always spends exactly the specified amount of time, regardless of time-influencing factors
	protected void spendConstant( float time ){
		this.time += time;
		//if time is very close to a whole number, round to a whole number to fix errors
		float ex = Math.abs(this.time % 1f);
		if (ex < .001f){
			this.time = Math.round(this.time);
		}
	}

	//sends time, but the amount can be influenced
	protected void spend( float time ) {
		spendConstant( time );
	}

	public void spendToWhole(){
		time = (float)Math.ceil(time);
	}
	
	protected void postpone( float time ) {
		if (this.time < now + time) {
			this.time = now + time;
			//if time is very close to a whole number, round to a whole number to fix errors
			float ex = Math.abs(this.time % 1f);
			if (ex < .001f){
				this.time = Math.round(this.time);
			}
		}
	}
	
    private boolean coopFrozen;

    /** Keep the body, actor ID and buff target links; store remaining time relative to suspension. */
    public static void freezeCoop(Char ch, boolean freeze) {
        ch.setCoopFrozen(freeze);
        for (Buff buff : ch.buffs()) ((Actor)buff).setCoopFrozen(freeze);
    }

    final void setCoopFrozen(boolean freeze) {
        if (coopFrozen == freeze) return;
        time = freeze ? Math.max(0, time - now) : now + time;
        coopFrozen = freeze;
    }

	public float cooldown() {
		return coopFrozen ? time : time - now;
	}

	public void clearTime() {
		spendConstant(-Actor.now());
		if (this instanceof Char){
			for (Buff b : ((Char) this).buffs()){
				b.spendConstant(-Actor.now());
			}
		}
	}

	public void timeToNow() {
		time = now;
	}

	//used when now is being cleared as a part of static fixTime()
	public void fixTime(float decrement){
		time -= decrement;
	}
	
	protected void diactivate() {
		time = Float.MAX_VALUE;
	}
	
	protected void onAdd() {}
	
	protected void onRemove() {}

	private static final String TIME    = "time";
	private static final String ID      = "id";

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( TIME, time );
		bundle.put( ID, id );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		time = bundle.getFloat( TIME );
		int incomingID = bundle.getInt( ID );
		if (Actor.findById(incomingID) == null){
			id = incomingID;
		} else {
			id = nextID++;
		}
	}

	public int id() {
		if (id > 0) {
			return id;
		} else {
			return (id = nextID++);
		}
	}

	// **********************
	// *** Static members ***
	// **********************
	
	private static HashSet<Actor> all = new HashSet<>();
	private static HashSet<Char> chars = new HashSet<>();
	private static volatile Actor current;

	private static SparseArray<Actor> ids = new SparseArray<>();
	private static int nextID = 1;

	private static float now = 0;
	
    public static Bundle schedulerState() {
        Bundle b = new Bundle();
        b.put("now",now);
        b.put("count",all.size());
        for (Actor a : all) {
            Bundle entry = new Bundle();
            entry.put("type",a.getClass().getName());
            entry.put("time",a.time);
            entry.put("priority",a.actPriority);
            entry.put("frozen",a.coopFrozen);
            b.put("a"+a.id(),entry);
        }
        return b;
    }

    public static void restoreSchedule(Bundle b, Actor input) {
        float clock=b.getFloat("now");
        if (!(Math.abs(clock) <= Float.MAX_VALUE) || all.size()!=b.getInt("count") || all.size()>8192)
            throw new IllegalArgumentException("Scheduler size or clock");
        HashSet<Integer> unique = new HashSet<>();
        for (Actor a : all) {
            Bundle e=b.getBundle("a"+a.id());
            if (!unique.add(a.id()) || a.id()>=nextID || e.isNull() || !e.getString("type").equals(a.getClass().getName()))
                throw new IllegalArgumentException("Actor identity");
            a.time=e.getFloat("time");
            a.coopFrozen=e.getBoolean("frozen");
            a.actPriority=e.getInt("priority");
            if (!(Math.abs(a.time) <= Float.MAX_VALUE) || a.time < (a.coopFrozen ? 0 : clock))
                throw new IllegalArgumentException("Actor time");
        }
        if (input!=null && (input.coopFrozen || input.time!=clock || !all.contains(input)))
            throw new IllegalArgumentException("Invalid input offer");
        now=clock;
        current=input;
    }

    public boolean frozenInCoop() { return coopFrozen; }

	public static float now(){
		return now;
	}
	
	public static synchronized void clear() {
		
		now = 0;
		if (Dungeon.coop != null) current = null;

		all.clear();
		chars.clear();

		ids.clear();
	}

	public static synchronized void fixTime() {
		
		if (all.isEmpty()) return;
		
		float min = Float.MAX_VALUE;
		for (Actor a : all) {
			if (!a.coopFrozen && a.time < min) {
				min = a.time;
			}
		}

		if (Dungeon.coop != null && min == Float.MAX_VALUE) return;
		//Only pull everything back by whole numbers
		//So that turns always align with a whole number
		min = (int)min;
		for (Actor a : all) {
			if (!a.coopFrozen) a.fixTime(min);
		}
		TargetedCell.fixTime(min);

		if (Dungeon.hero != null && all.contains( Dungeon.hero )) {
			Statistics.duration += min;
		}
		now -= min;
	}
	
	public static void init() {
		
		add( Dungeon.hero );
		
		for (Mob mob : Dungeon.level.mobs) {
			add( mob );
		}

		//mobs need to remember their targets after every actor is added
		for (Mob mob : Dungeon.level.mobs) {
			mob.restoreEnemy();
		}
		
		for (Blob blob : Dungeon.level.blobs.values()) {
			add( blob );
		}
		
		current = null;
	}

	private static final String NEXTID = "nextid";

	public static void storeNextID( Bundle bundle){
		bundle.put( NEXTID, nextID );
	}

	public static void restoreNextID( Bundle bundle){
		nextID = bundle.getInt( NEXTID );
	}

	public static void resetNextID(){
		nextID = 1;
	}

	/*protected*/public void next() {
		if (current == this) {
			current = null;
		}
	}

	public static boolean processing(){
		return current != null;
	}

	public static int curActorPriority() {
		return current != null ? current.actPriority : HERO_PRIO;
	}
	
	public static boolean keepActorThreadAlive = true;
	
	public static void process() {
		
		boolean doNext;
		boolean interrupted = false;
		int steps = 0;
		if (Dungeon.coop != null) Dungeon.coop.checkCanAdvance();

		do {
			
			if (Dungeon.coop != null) {
				if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Engine interrupted");
				if (++steps > 10000) throw new IllegalStateException("No player turn within 10000 actor steps");
			}
			current = null;
			if (!interrupted && (Dungeon.coop != null || !Game.switchingScene())) {
				float earliest = Float.MAX_VALUE;

				synchronized (Actor.class) {
					for (Actor actor : all) {
						if (actor.coopFrozen) continue;

						//some actors will always go before others if time is equal.
						if (actor.time < earliest ||
								actor.time == earliest && (current == null || actor.actPriority > current.actPriority
									|| (Dungeon.coop != null && actor.actPriority == current.actPriority
									&& Dungeon.coop.precedes(actor, current)))) {
							earliest = actor.time;
							current = actor;
						}

					}
				}
			}

			if  (current != null) {

				now = current.time;
				Actor acting = current;
				if (Dungeon.coop != null && Dungeon.coop.offerTurn(acting)) return;

				if (Dungeon.coop == null && acting instanceof Char && ((Char) acting).sprite != null) {
					// If it's character's turn to act, but its sprite
					// is moving, wait till the movement is over
					try {
						synchronized (((Char)acting).sprite) {
							if (((Char)acting).sprite.isMoving) {
								((Char) acting).sprite.wait();
							}
						}
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
				
				interrupted = interrupted || Thread.interrupted();
				
				if (interrupted){
					doNext = false;
					current = null;
				} else {
					doNext = acting.act();
					if (Dungeon.coop == null && doNext && (Dungeon.hero == null || !Dungeon.hero.isAlive())) {
						doNext = false;
						current = null;
					}
				}
			} else {
				if (Dungeon.coop != null) throw new IllegalStateException("Empty co-op scheduler");
				doNext = false;
			}

			if (Dungeon.coop != null) {
				if (Dungeon.coop.stopped() || Dungeon.coop.defeated()) { current = null; return; }
				if (interrupted || (!doNext && current != null)) throw new IllegalStateException("Unexpected visual wait");
				continue;
			}
			if (!doNext){
				synchronized (Thread.currentThread()) {
					
					interrupted = interrupted || Thread.interrupted();
					
					if (interrupted){
						current = null;
						interrupted = false;
					}

					//signals to the gamescene that actor processing is finished for now
					Thread.currentThread().notify();
					
					try {
						Thread.currentThread().wait();
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
			}

		} while (Dungeon.coop != null || keepActorThreadAlive);
	}
	
	public static void add( Actor actor ) {
		add( actor, now );
	}
	
	public static void addDelayed( Actor actor, float delay ) {
		add( actor, now + Math.max(delay, 0) );
	}
	
	private static synchronized void add( Actor actor, float time ) {
		
		if (all.contains( actor )) {
			return;
		}

		ids.put( actor.id(),  actor );

		all.add( actor );
		actor.time += time;
		actor.onAdd();
		
		if (actor instanceof Char) {
			Char ch = (Char)actor;
			chars.add( ch );
			for (Buff buff : ch.buffs()) {
				add(buff);
			}
		}
	}
	
	public static synchronized void remove( Actor actor ) {
		
		if (actor != null) {
			all.remove( actor );
			chars.remove( actor );
			actor.onRemove();

			if (actor.id > 0) {
				ids.remove( actor.id );
			}
		}
	}

	//'freezes' a character in time for a specified amount of time
	//USE CAREFULLY! Manipulating time like this is useful for some gameplay effects but is tricky
	public static void delayChar( Char ch, float time ){
		ch.spendConstant(time);
		for (Buff b : ch.buffs()){
			b.spendConstant(time);
		}
	}
	
	public static synchronized Char findChar( int pos ) {
		for (Char ch : chars){
			if (ch.pos == pos)
				return ch;
		}
		return null;
	}

	public static synchronized Actor findById( int id ) {
		return ids.get( id );
	}

	public static synchronized HashSet<Actor> all() {
		return new HashSet<>(all);
	}

	public static synchronized HashSet<Char> chars() { return new HashSet<>(chars); }
}
