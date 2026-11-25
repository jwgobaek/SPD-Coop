#!/usr/bin/env python3
"""Match committed TCP replies with the native skeleton in an independently inspected generation."""
from pathlib import Path
from tempfile import TemporaryDirectory
import math
import runpy
import shutil
import statistics
import subprocess
import sys
import time

scripts = Path(__file__).resolve().parent
repo = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else scripts.parent
engine = runpy.run_path(str(repo / 'scripts/verify-alpha-migration.py'))
assets = repo / 'core/src/main/assets'
samples = []
with TemporaryDirectory() as name:
    work = Path(name)
    cp = engine['classpath'](repo, work)
    java = engine['JAVA']
    probe = scripts / 'verification/SummonResponseProbe.java'
    subprocess.run([str(java.with_name('javac')), '-cp', cp, '-d', name, str(probe)], check=True)
    prefix = [str(java), '-Xmx256m', '-cp', name + ':' + cp]
    for target in range(2):
        initial = work / f'seat-{target}'
        initial.mkdir()
        engine['run'](repo, cp, initial, f'necro-setup-{target}')
        caster = int((initial / 'caster').read_text())
        for trial in range(6):
            profile = work / f'run-{target}-{trial}'
            shutil.copytree(initial, profile)
            for marker in ('endpoint', 'stop'):
                (profile / marker).unlink(missing_ok=True)
            with (work / 'host.log').open('w') as log, subprocess.Popen(
                    prefix + [engine['PACKAGE'] + 'RestartHost', str(profile), 'measure-native-summon'],
                    cwd=assets, stdout=log, stderr=subprocess.STDOUT) as host:
                try:
                    deadline = time.monotonic() + 20
                    while not (profile / 'endpoint').exists():
                        if host.poll() is not None or time.monotonic() >= deadline:
                            raise RuntimeError('Host failed to open the room')
                        time.sleep(0.02)
                    with subprocess.Popen(prefix + [engine['PACKAGE'] + 'SummonResponseProbe', str(profile)],
                                          cwd=assets, text=True, stdin=subprocess.PIPE, stdout=subprocess.PIPE) as client:
                        try:
                            for step in range(8):
                                client.stdin.write('WAIT\n')
                                client.stdin.flush()
                                micros = int(client.stdout.readline())
                                image = engine['generation'](profile)['state']['engine']
                                mobs = image['floor']['mobs']
                                owner = next(mob for mob in mobs if int(mob['id']) == caster)
                                minion = int(owner.get('my_skeleton', 0))
                                if minion and any(int(mob['id']) == minion for mob in mobs):
                                    samples.append(micros)
                                    print(f'target={target} trial={trial+1} wait={step+1} summon_ack_us={micros}', flush=True)
                                    break
                            else:
                                raise AssertionError('No native summon generation')
                        finally:
                            client.stdin.close()
                            client.wait(timeout=15)
                        assert client.returncode == 0
                    (profile / 'stop').touch()
                    assert host.wait(timeout=15) == 0
                finally:
                    if host.poll() is None:
                        host.kill()
                        host.wait()
            shutil.rmtree(profile)
assert len(samples) == 12
print(f'summon_ack n={len(samples)} median_us={statistics.median(samples)} p95_us={sorted(samples)[math.ceil(.95*len(samples))-1]}')
