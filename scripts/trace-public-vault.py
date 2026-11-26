#!/usr/bin/env python3
"""Attach a read-only fatal-hit observer to the vault guide's isolated test JVM."""
import argparse
import os
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory

parser=argparse.ArgumentParser()
parser.add_argument('core_test_classpath')
parser.add_argument('--seed',type=int,default=572)
parser.add_argument('--budget',type=int,default=650)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
bin_dir=Path(os.environ['JAVA_HOME'])/'bin'
with TemporaryDirectory() as tmp:
    source=(root/'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java').read_text()
    marker='Dungeon.coop.fallen((Hero)this, beforeHit);'
    if source.count(marker)!=1:
        raise ValueError('Check native fatal-hit hook')
    # Never expose this observer, the room type or the damage source to the decision function.
    audited="""{
                if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) {
                    com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel level =
                            (com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) Dungeon.level;
                    Object room = level.room(pos);
                    System.out.printf("FATAL_HIT\\t%d\\t%d\\t%d\\t%s\\t%s%n", Dungeon.coop.hero(0)==this ? 0 : 1,
                            pos, beforeHit, src instanceof Class ? ((Class<?>)src).getSimpleName() :
                            src==null ? "unknown" : src.getClass().getSimpleName(),
                            room==null ? "corridor" : room.getClass().getSimpleName());
                }
                Dungeon.coop.fallen((Hero)this, beforeHit);
            }"""
    char=Path(tmp)/'Char.java'
    char.write_text(source.replace(marker,audited))
    subprocess.run([bin_dir/'javac','-cp',args.core_test_classpath,'-d',tmp,char,
                    root/'scripts/verification/VaultPublicRoute.java'],check=True)
    subprocess.run([bin_dir/'java','-Xmx384m','-cp',tmp+os.pathsep+args.core_test_classpath,
                    'com.shatteredpixel.shatteredpixeldungeon.lan.VaultPublicRoute',str(args.seed),str(args.budget)],
                    cwd=root/'core/src/main/assets',check=True)
