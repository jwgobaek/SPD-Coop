#!/usr/bin/env python3
"""Run the existing durable, two-seat TCP fixture in independent temporary game profiles."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import runpy
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
migration = runpy.run_path(str(root / 'scripts/verify-alpha-migration.py'))
parser = argparse.ArgumentParser()
parser.add_argument('classpath', help='Absolute core test runtime classpath')
parser.add_argument('--sessions', type=int, default=50)
parser.add_argument('--long-sessions', type=int, default=10)
parser.add_argument('--workers', type=int, default=2)
args = parser.parse_args()
if not 0 <= args.long_sessions <= args.sessions or not 1 <= args.workers <= 4:
    parser.error('Invalid number of sessions or workers')


def sample(number):
    with TemporaryDirectory(prefix='coop-phase9-') as temp:
        profile = Path(temp)
        migration['run'](root, args.classpath, profile, 'new', client_timeout=180)
        identity = migration['generation'](profile)['session']
        if number < args.long_sessions:
            for stage, status in [('revive', 78), ('soak', 79), ('after', 0)]:
                (profile / 'kill-before-reply').unlink(missing_ok=True)
                try:
                    migration['run'](root, args.classpath, profile, stage, expected_exit=status, client_timeout=300)
                except Exception:
                    print((profile / 'host.log').read_text()[-1800:], flush=True)
                    raise
            assert migration['generation'](profile)['session'] == identity
        print(f'PASS session={number+1} soak_waits={1000 if number < args.long_sessions else 0}', flush=True)
        return identity


with ThreadPoolExecutor(max_workers=args.workers) as pool:
    sessions = list(pool.map(sample, range(args.sessions)))
assert len(set(sessions)) == args.sessions
print(f'PASS independent_rooms={len(sessions)} soak_waits={1000*args.long_sessions} '
      f'pair_reconnects_in_soak={13*args.long_sessions}', flush=True)
