#!/usr/bin/env python3
"""Build the managed pair locally from an inspected build and curated public verification evidence."""
import argparse
import hashlib
import os
from pathlib import Path, PurePosixPath
import re
import runpy
import shutil
import subprocess
import tarfile
import tempfile
from xml.etree.ElementTree import parse
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERIFY = runpy.run_path(str(ROOT / 'scripts/verify-final-deliverables.py'))
APK = VERIFY['APK']
ZIP = VERIFY['BACKUP']
ALLOWED = VERIFY['allowed']
SOURCE_BASE = '76ed0506c9589c17dcbdaaca7b78a886f3fb0e92'
SIGNER = '0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53'
SENSITIVE = re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|Bearer [A-Za-z0-9._-]{30,}|[A-Za-z]:\\Users\\(?!Public\\|Default\\)[A-Za-z0-9_-]+\\', re.I)
PRIVATE_NAMES = {'key0', 'key1', 'private0', 'private1', 'boss-command', 'epoch', 'HEAD', 'BACKUP'}

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def scan(name, data):
    if not ALLOWED(name) or PurePosixPath(name).name in PRIVATE_NAMES or SENSITIVE.search(data):
        raise ValueError('Review private or prohibited data in ' + name)

def add_tree(z, commit, prefix, include):
    with subprocess.Popen(['git','archive','--format=tar',commit], cwd=ROOT, stdout=subprocess.PIPE) as process:
        with tarfile.open(fileobj=process.stdout,mode='r|') as files:
            for member in files:
                if not member.isfile() or not ALLOWED(member.name) or not include(member.name):
                    continue
                data=files.extractfile(member).read()
                name=prefix+'/'+member.name
                scan(name,data)
                entry=zipfile.ZipInfo(name,(2026,11,25,0,0,0))
                entry.external_attr=(0o100000|member.mode)<<16
                z.writestr(entry,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
        if process.wait():
            raise RuntimeError('git archive failed')

def package(builds, evidence, out):
    build=(builds/'commit.txt').read_text().strip()
    verification=git('rev-parse','HEAD').decode().strip()
    git('merge-base','--is-ancestor',build,verification)
    apk=builds/'android-debug.apk'
    binary=apk.read_bytes()
    digest=sha(binary)
    assert (builds/'SHA256SUMS').read_text().split()[0]==digest
    inspection=(builds/'apk.txt').read_text()
    version=re.search(r"versionName='([^']+)'",inspection)[1]
    mines = version.endswith('-INDEV-mines-city')
    source_base = 'df645fc08d0b33dbd5e2d7a847f075dc26d4b020' if mines else SOURCE_BASE
    code=re.search(r"versionCode='(\d+)'",inspection)[1]
    assert "name='org.spdcoop.dungeon.indev'" in inspection
    assert f'certificate SHA-256 digest: {SIGNER}' in inspection
    for flag in ('v1 scheme (JAR signing)','v2 scheme (APK Signature Scheme v2)'):
        assert f'Verified using {flag}: true' in inspection
    junit=sorted((builds/'junit').glob('TEST-*.xml'))
    cases=[case for path in junit for case in parse(path).getroot().findall('testcase')]
    assert cases and not any(case.find(tag) is not None for case in cases for tag in ('error','failure','skipped'))
    out.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(dir=out) as temp:
        staging=Path(temp)
        shutil.copyfile(apk,staging/APK)
        with zipfile.ZipFile(staging/ZIP,'w',compression=zipfile.ZIP_DEFLATED) as z:
            add_tree(z,build,'source',lambda name: True)
            def final_doc(name):
                path=PurePosixPath(name)
                return ((len(path.parts)==1 and path.suffix=='.md') or
                        (name.startswith('docs/coop/') and path.suffix in {'.md','.tsv','.txt'}) or
                        name.startswith('scripts/') or name.endswith('/SewerJourney.java'))
            add_tree(z,verification,'documentation',final_doc)
            extras={p:'evidence/junit/'+p.name for p in junit}
            extras[builds/'build.log']='evidence/build.log'
            extras[builds/'apk.txt']='evidence/package-signature.txt'
            for p in sorted(evidence.rglob('*')):
                if p.is_file():
                    extras[p]=p.relative_to(evidence).as_posix()
            for path,name in extras.items():
                if name in z.namelist(): raise ValueError('Duplicate member '+name)
                raw=path.read_bytes()
                scan(name,raw)
                z.writestr(name,raw)
            info=f"""SPD-Coop {version}
Application ID       org.spdcoop.dungeon.indev
Version              {version}
versionCode          {code}
APK                  {APK}
Build commit         {build}
Verification commit  {verification}
APK bytes            {len(binary)}
APK SHA-256          {digest}
Certificate SHA-256  {SIGNER}
Signatures           v1 and v2 verified
JUnit                {len(cases)} passed; no errors, failures or skipped cases
Scope                {"Branch 0 through 19 and Blacksmith mines; save schemas 1–10, SYNC22, LAN v1." if mines else "Branch 0 through 15; save schemas 1–9, SYNC21, LAN v1."}
Checkpoint           {"See documentation/docs/coop/mines-city-results.md for the fixture, seed and network test boundaries." if mines else "12/12 staged cave victories. Mine branch and city 16 closed."}
Normal starts        581 and 1105 reached 6. No complete normal-start DM-300 victory.
Platforms            Original JVM engine, separate TCP JVMs, real filesystem, KO/EN Desktop, Android package inspection.
Android runtime      See evidence/android/result.txt.
Source               source/ is the APK tree. documentation/ contains subsequent reports and verification scripts.
"""
            z.writestr('BUILD-INFO.txt',info)
            z.writestr('evidence/changed-files.txt',git('diff','--name-status',source_base,verification))
            z.writestr('evidence/commits.txt',git('log','--reverse','--format=%H %s',source_base+'..'+verification))
            lines=[digest+'  '+APK]+[sha(z.read(n))+'  '+n for n in sorted(z.namelist())]
            z.writestr('SHA256SUMS.txt','\n'.join(lines)+'\n')
        VERIFY['verify'](staging,build)
        for name in (APK,ZIP):
            os.replace(staging/name,out/name)
            print(name,(out/name).stat().st_size,sha((out/name).read_bytes()))

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('build_reports','curated_evidence','output'):
        parser.add_argument(name,type=Path)
    args=parser.parse_args()
    package(args.build_reports,args.curated_evidence,args.output)
