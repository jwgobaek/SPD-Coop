#!/usr/bin/env python3
"""Time actual receipt-bearing gameplay requests in independent temporary TCP profiles."""
import argparse
import os
from pathlib import Path
import runpy
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('classpath')
parser.add_argument('--pairs', type=int, default=5)
args = parser.parse_args()
runner = runpy.run_path(str(root / 'scripts/verify-alpha-migration.py'))['run']
os.environ['JAVA_TOOL_OPTIONS'] = os.environ.get('JAVA_TOOL_OPTIONS', '') + ' -Dspd.timePlayerAction=true'
for pair in range(args.pairs):
    for seat in range(2):
        for action in ('dart', 'talent', 'owner', 'right'):
            with TemporaryDirectory(prefix='coop-player-cost-') as tmp:
                directory = Path(tmp)
                if action == 'dart':
                    runner(root, args.classpath, directory, f'dart{seat}')
                else:
                    for stage in ('setup', 'commit'):
                        runner(root, args.classpath, directory, f'tengu-{stage}-growth-{action}{seat}')
