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
parser.add_argument('--campaign-guide', action='store_true', help='Preserve food and close on visible partners near known exits')
parser.add_argument('--stair-strategy', choices=('scout', 'wait', 'meet'),
                    help='Use known stairs and public frontiers. Wait extends safe pauses. Meet commits to the known stair.')
parser.add_argument('--avoid-outnumbered', action='store_true',
                    help='Withdraw from multiple visible nearby enemies when hurt and separated.')
parser.add_argument('--defensive-pair', action='store_true', help='Conserve the last cloak charge and keep the follower near its last directly observed partner.')
parser.add_argument('--wash-ooze', action='store_true', help='Use visible adjacent water to clear the original ooze status during the sewer boss fight.')
parser.add_argument('--camp-goo', action='store_true', help='Spend up to 60 safe turns per seat recovering together before the sewer boss.')
parser.add_argument('--lure-goo', action='store_true', help='Draw visible Goo off healing water before returning to melee.')
parser.add_argument('--face-goo', action='store_true', help='Close on visible Goo for a melee attack and withdraw from its public charge warning.')
parser.add_argument('--hold-known-exit', action='store_true', help='Keep a personally observed exit as the destination while hungry, after handling food and enemies.')
parser.add_argument('--rest-together', action='store_true', help='Rest in sight of a living partner while fed and hurt, leaving as soon as an enemy appears.')
parser.add_argument('--save-floors', type=Path, help='Keep the first surviving arrival checkpoint on each new floor in this trial.')
parser.add_argument('--trace-stairs', action='store_true',
                    help='Record each active seat’s public stair observations after the first floor or at low health.')
parser.add_argument('--save-game', type=Path, help='Write one new isolated native game checkpoint after the trial.')
parser.add_argument('--resume-game', type=Path, help='Read a matching isolated native game checkpoint in a fresh JVM.')
args=parser.parse_args()
if (args.save_game or args.resume_game or args.save_floors) and (len(args.seeds)!=1 or args.policies!=['last']):
    parser.error('A campaign checkpoint requires a single seed and the last-hit policy.')
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
    guide_options = []
    if args.stair_strategy is not None:
        guide_options.extend(('-Dtrial.stairRendezvous=true', '-Dtrial.frontierGuide=true'))
        if args.stair_strategy in ('wait', 'meet'):
            guide_options.append('-Dtrial.waitAtVisibleExit=true')
        if args.stair_strategy == 'meet':
            guide_options.append('-Dtrial.meetOnStair=true')
    if args.avoid_outnumbered:
        guide_options.append('-Dtrial.avoidOutnumbered=true')
    if args.defensive_pair:
        guide_options.append('-Dtrial.defensivePair=true')
    if args.wash_ooze:
        guide_options.append('-Dtrial.washOoze=true')
    if args.camp_goo:
        guide_options.append('-Dtrial.campGoo=true')
    if args.lure_goo:
        guide_options.append('-Dtrial.lureGoo=true')
    if args.face_goo or args.lure_goo or args.camp_goo or args.wash_ooze:
        guide_options.append('-Dtrial.faceGoo=true')
    if args.hold_known_exit:
        guide_options.append('-Dtrial.holdKnownExit=true')
    if args.rest_together:
        guide_options.append('-Dtrial.restTogether=true')
    if args.save_floors:
        args.save_floors.mkdir(parents=True, exist_ok=True)
        guide_options.append('-Dtrial.saveFloors='+str(args.save_floors.resolve()))
    if args.trace_stairs:
        guide_options.extend(('-Dtrial.traceRecovery=true', '-Dtrial.traceStairs=true'))
    if args.resume_game:
        guide_options.append('-Dtrial.resumeGame='+str(args.resume_game.resolve()))
    if args.save_game:
        guide_options.append('-Dtrial.saveGame='+str(args.save_game.resolve()))
    for seed in args.seeds:
        for policy in args.policies:
            subprocess.run([java_bin/'java','-Xmx384m',*guide_options,'-Dtrial.campaignGuide='+str(args.campaign_guide or args.stair_strategy is not None).lower(),'-Dtrial.front='+args.front,'-Dtrial.partner='+args.partner,'-cp',tmp+os.pathsep+args.core_test_classpath,
                            trial,policy,str(seed),str(args.budget)],cwd=root/'core/src/main/assets',check=True)
