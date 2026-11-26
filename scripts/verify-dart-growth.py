#!/usr/bin/env python3
"""Exercise both seats against a real host that exits after publishing the successful action."""
import argparse
import runpy
from pathlib import Path
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('core_test_classpath', help='Absolute core test runtime classpath')
parser.add_argument('--actions', nargs='+', default=['dart','talent','owner','grant','right'])
parser.add_argument('--seats', nargs='+', type=int, choices=[0,1], default=[0,1])
args = parser.parse_args()
host = runpy.run_path(str(root / 'scripts/verify-alpha-migration.py'))['run']
with TemporaryDirectory() as temp:
    for action in args.actions:
        for seat in args.seats:
            stage = f'{action}{seat}'
            directory = Path(temp) / stage
            directory.mkdir()
            if action == 'dart':
                operations = [(stage, 78), ('pin-receipt', 0)]
            else:
                operations = [(f'tengu-{step}-growth-{stage}', code)
                              for step, code in [('setup', 0), ('commit', 78), ('read', 0)]]
            for mode, code in operations:
                try:
                    host(root, args.core_test_classpath, directory, mode, expected_exit=code)
                except Exception:
                    print((directory / 'host.log').read_text()[-2000:])
                    raise
            print('PASS', stage, flush=True)
