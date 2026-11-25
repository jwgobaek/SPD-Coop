#!/usr/bin/env python3
"""Race one delayed legitimate order and the other seat's current order over two TCP connections."""
import runpy
from pathlib import Path
from tempfile import TemporaryDirectory

root=Path(__file__).resolve().parents[1]
fixture=runpy.run_path(str(root/"scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as folder:
    temp=Path(folder)
    cp=fixture["classpath"](root,temp)
    for mine in ("gnoll","crystal"):
        room=temp/mine
        room.mkdir()
        fixture["run"](root,cp,room,"tengu-setup-mine-"+mine+"-race")
        fixture["run"](root,cp,room,"smith-order-race")
