#!/usr/bin/env python3
"""Run independent actual normal starts with one test-only Mob class per JVM."""
import argparse
import os
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory

parser=argparse.ArgumentParser()
parser.add_argument('core_test_classpath', help='Absolute core test runtime classpath')
parser.add_argument('--budget', type=int, default=750)
parser.add_argument('--seeds', type=int, nargs='+', default=[581,1105])
parser.add_argument('--front', choices=('WARRIOR','MAGE','HUNTRESS','ROGUE','DUELIST','CLERIC'), default='WARRIOR')
parser.add_argument('--partner', choices=('WARRIOR','MAGE','HUNTRESS','ROGUE','DUELIST','CLERIC'), default='HUNTRESS')
parser.add_argument('--policies', nargs='+', choices=('last','split','each'), default=['last','split','each'])
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
java_bin=Path(os.environ['JAVA_HOME'])/'bin'
trial='com.shatteredpixel.shatteredpixeldungeon.lan.ExperienceProgress'
with TemporaryDirectory() as tmp:
    work=Path(tmp)
    mob=(root/'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Mob.java').read_text()
    change='recipient.earnExp(exp, getClass());'
    if mob.count(change)!=1:
        raise ValueError('Review the current native XP award site')
    source=work/'Mob.java'
    source.write_text(mob.replace(change,trial+'.award(this, recipient, exp);'))
    verify=root/'scripts/verification'
    subprocess.run([java_bin/'javac','-cp',args.core_test_classpath,'-d',tmp,source,
                    verify/'VaultPublicRoute.java',verify/'ExperienceProgress.java'],check=True)
    for seed in args.seeds:
        for policy in args.policies:
            subprocess.run([java_bin/'java','-Xmx384m','-Dtrial.front='+args.front,'-Dtrial.partner='+args.partner,'-cp',tmp+os.pathsep+args.core_test_classpath,
                            trial,policy,str(seed),str(args.budget)],cwd=root/'core/src/main/assets',check=True)
