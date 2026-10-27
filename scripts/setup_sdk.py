#!/usr/bin/env python3
"""Install pinned official SDK archives, checking Google's repository checksums."""
import hashlib, os, pathlib, shutil, tempfile, urllib.request, xml.etree.ElementTree as ET, zipfile
root=pathlib.Path(os.environ.get('ANDROID_HOME','/workspace/tools/android-sdk'))
root.mkdir(parents=True,exist_ok=True)
base='https://dl.google.com/android/repository/'
manifest=ET.fromstring(urllib.request.urlopen(base+'repository2-1.xml').read())
for package,destination in [('platforms;android-33','platforms/android-33'),('build-tools;35.0.0','build-tools/35.0.0')]:
    target=root/destination
    if target.exists():
        print('Already installed:',target);continue
    candidates=[]
    for p in manifest.findall('remotePackage'):
        if p.get('path')==package:
            candidates.extend(a.find('complete') for a in p.findall('./archives/archive') if a.findtext('host-os') in (None,'linux'))
    archive=candidates[0]
    with tempfile.TemporaryDirectory(dir='/tmp') as tmp:
        path=pathlib.Path(tmp)/'sdk.zip'
        print('Downloading',archive.findtext('url'),flush=True)
        urllib.request.urlretrieve(base+archive.findtext('url'),path)
        expected=archive.findtext('checksum')
        if hashlib.sha1(path.read_bytes()).hexdigest()!=expected:
            raise RuntimeError('SDK archive checksum mismatch')
        with zipfile.ZipFile(path) as z:
            z.extractall(pathlib.Path(tmp)/'out')
            for entry in z.infolist():
                mode=entry.external_attr>>16
                if mode: (pathlib.Path(tmp)/'out'/entry.filename).chmod(mode)
        children=list((pathlib.Path(tmp)/'out').iterdir())
        if len(children)!=1:raise RuntimeError('Unexpected SDK archive layout')
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.move(str(children[0]),target)
        print('Installed:',target,flush=True)
