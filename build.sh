#!/bin/sh
set -eu
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/workspace/tools/android-sdk}
BT=$SDK/build-tools/35.0.0
JAR=$SDK/platforms/android-33/android.jar
[ -f "$JAR" ] && [ -x "$BT/aapt2" ] || { echo 'Run python3 scripts/setup_sdk.py first'; exit 1; }
# Only generated outputs in build are removed.
rm -rf build
mkdir -p build/classes build/gen build/dex .local
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 24 --target-sdk-version 28 --java build/gen -A assets -o build/unsigned.apk build/res.zip
find src build/gen -name '*.java' > build/sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -encoding UTF-8 -bootclasspath "$JAR" -d build/classes @build/sources.txt
python3 - <<'PY'
import pathlib,zipfile
with zipfile.ZipFile('build/classes.jar','w') as z:
 for p in pathlib.Path('build/classes').rglob('*.class'):z.write(p,p.relative_to('build/classes'))
PY
"$BT/d8" --lib "$JAR" --min-api 24 --output build/dex build/classes.jar
python3 - <<'PY'
import zipfile,pathlib
with zipfile.ZipFile('build/unsigned.apk','a') as z:
 for p in pathlib.Path('build/dex').glob('*.dex'):z.write(p,p.name)
PY
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f .local/debug.keystore ]; then
 keytool -genkeypair -keystore .local/debug.keystore -storepass android -keypass android -alias skyncar -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=SkyNCar Development' </dev/null
fi
"$BT/apksigner" sign --ks .local/debug.keystore --ks-key-alias skyncar --ks-pass pass:android --key-pass pass:android --out build/SkyNCar.apk build/aligned.apk
"$BT/apksigner" verify --verbose build/SkyNCar.apk
