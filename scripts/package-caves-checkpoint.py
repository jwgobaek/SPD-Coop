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
    frost = version.endswith('-INDEV-frost-senses')
    wand_arts = version.endswith('-INDEV-wand-arts')
    upper = version.endswith('-INDEV-upper-growth')
    combo = version.endswith('-INDEV-combo')
    growth = version.endswith('-INDEV-dart-growth')
    play = version.endswith('-INDEV-vault-play')
    ascent = version.endswith('-INDEV-vault-ascent')
    yog = version.endswith('-INDEV-yog-victory')
    halls = version.endswith('-INDEV-imp-halls')
    king = version.endswith('-INDEV-dwarf-king')
    mines = king or version.endswith('-INDEV-mines-city')
    source_base = '7743e2cb217fa9c1c38e0d0f1f78aac59afbc2f1' if frost else '17bfa4a158c68bcdd2a11c4570d958f0f97f24b7' if wand_arts else '9ae5eff26a5fe78dc27264a54790166b56b65ed4' if upper else '2d780947c03e2d2eecea227474e75d3f2d832b89' if combo else '6cee7f3db92dff8b7c7c2b37b51980e13e16a1aa' if growth else 'f4232290d8050d2c938bd927d95c8003f7e27f3f' if play else 'f239c06a638b20f2729579f5ee7a4c72f7aa911c' if ascent else 'ce70d083eaa29c31bd704dab9595c2e59da1d1d3' if yog else '960e4bc9ad4b94fdb4de7959c41c17827066ff39' if halls else 'df645fc08d0b33dbd5e2d7a847f075dc26d4b020' if mines else SOURCE_BASE
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
            evidence_changes = set(git('diff','--name-only','-z',build,verification).decode().split('\0'))
            def final_doc(name):
                path=PurePosixPath(name)
                return ((len(path.parts)==1 and path.suffix=='.md') or
                        (name.startswith('docs/coop/') and path.suffix in {'.md','.tsv','.txt'}) or
                        name.startswith('scripts/') or
                        (name in evidence_changes and name.startswith(('core/src/test/', 'desktop/src/test/'))))
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
Scope                {"Six lobby classes, owner-scoped sensing and growth. Schemas 1-20, SYNC31, LAN v1." if frost else "Six lobby classes; schema 19 for embedded wand identities and new rosters; SYNC30, LAN v1." if wand_arts else "Rogue rooms: schema 18, floor schemas 1-15. SYNC29, LAN v1. See the upper-growth capability table." if upper else "Verified main floors through 26 and branches; schemas 1-15, mask trial 16 and crown trial 17; SYNC28, LAN v1." if combo else "Branch 0 through 26, mines and vault. Schemas 1-15 plus mask trial schema 16, SYNC27, LAN v1." if growth else "Branch 0 through 26, the mines and Imp vault. Save schemas 1-15, SYNC26, LAN v1." if play else "Branch 0 through 26 on admitted rolls, mines and vault, ordinary and Ascent endings, schemas 1-15, SYNC25, LAN v1." if ascent else "Branch 0 through 26 on admitted rolls, cooperative ending, schemas 1-14, SYNC24." if yog else "Branch 0 through 24 on admitted native rolls, mines and Imp vault; schemas 1-13, SYNC23, LAN v1." if halls else "Branch 0 through 20, both Blacksmith mines; save schemas 1–11, SYNC22, LAN v1." if king else "Branch 0 through 19 and Blacksmith mines; save schemas 1–10, SYNC22, LAN v1." if mines else "Branch 0 through 15; save schemas 1–9, SYNC21, LAN v1."}
Checkpoint           {"See documentation/docs/coop/frost-senses-results.md." if frost else "See documentation/docs/coop/wand-arts-results.md." if wand_arts else "See documentation/docs/coop/upper-growth-results.md." if upper else "See documentation/docs/coop/combo-results.md for actions and staged trials." if combo else "See documentation/docs/coop/dart-growth-results.md for item, growth and recovery trials." if growth else "See documentation/docs/coop/vault-play-results.md for twelve staged victories, the vault entrance trials and Android results." if play else "See documentation/docs/coop/vault-ascent-results.md for native fights, staged heroes and recovery." if ascent else "See documentation/docs/coop/yog-victory-results.md for native fights, staged heroes and recovery." if yog else "See documentation/docs/coop/imp-halls-results.md for staged and normal-start limits." if halls else "See documentation/docs/coop/mines-city-results.md for the fixture, seed and network test boundaries." if mines else "12/12 staged cave victories. Mine branch and city 16 closed."}
Normal starts        {"Normal-start Yog and Ascension remain unverified. See development report." if yog or ascent or play or growth or combo or upper or wand_arts or frost else "Eight seeds. 581 reached 6. 1105 reached 5 within this guide budget. No Yog victory." if halls else "581 and 1105 reached 6. No complete normal-start DM-300 victory."}
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
