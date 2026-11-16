"""Build Li Widget with the Android SDK and JDK; no Gradle or network required."""
from pathlib import Path
import argparse
import os
import secrets
import shutil
import subprocess
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', default=os.getenv('ANDROID_SDK_ROOT') or os.getenv('ANDROID_HOME'))
    parser.add_argument('--build-tools', default='36.1.0')
    parser.add_argument('--work-directory', type=Path, default=ROOT / 'build')
    parser.add_argument('--output', type=Path, default=ROOT / 'dist' / 'Li-Widget-1.0.apk')
    parser.add_argument('--keystore', type=Path)
    parser.add_argument('--alias', default='liwidget')
    parser.add_argument('--password-env', default='LI_WIDGET_STOREPASS')
    args = parser.parse_args()
    if not args.sdk:
        parser.error('Set ANDROID_SDK_ROOT or pass --sdk PATH.')
    sdk = Path(args.sdk).resolve()
    build_tools = sdk / 'build-tools' / args.build_tools
    android = sdk / 'platforms' / 'android-36' / 'android.jar'
    windows = os.name == 'nt'

    def sdk_tool(name):
        suffix = ('.bat' if name in ('d8', 'apksigner') else '.exe') if windows else ''
        path = build_tools / (name + suffix)
        if not path.is_file():
            parser.error('Missing SDK tool: ' + str(path))
        return path

    def java_tool(name):
        found = shutil.which(name)
        if not found and os.getenv('JAVA_HOME'):
            found = str(Path(os.environ['JAVA_HOME']) / 'bin' / (name + ('.exe' if windows else '')))
        if not found or not Path(found).is_file():
            parser.error('Missing JDK tool: ' + name)
        return found

    if not android.is_file():
        parser.error('Install Android SDK Platform 36: ' + str(android))
    env = os.environ.copy()
    key = args.keystore
    if key is None:
        private = ROOT / '.local'
        private.mkdir(exist_ok=True)
        key = private / 'liwidget.jks'
        password_file = private / 'keystore-password.txt'
        if key.exists() and not password_file.exists():
            parser.error('Local keystore password is missing; restore .local/keystore-password.txt.')
        if not password_file.exists():
            password_file.write_text(secrets.token_urlsafe(32), encoding='utf-8')
        env[args.password_env] = password_file.read_text(encoding='utf-8').strip()
    elif not key.is_file():
        parser.error('Keystore does not exist: ' + str(key))
    if not env.get(args.password_env):
        parser.error('Set the password environment variable: ' + args.password_env)

    def run(command):
        print('Running ' + Path(command[0]).name, flush=True)
        subprocess.run([str(part) for part in command], env=env, check=True)

    if not key.exists():
        run([java_tool('keytool'), '-genkeypair', '-keystore', key, '-alias', args.alias,
             '-storepass:env', args.password_env, '-keypass:env', args.password_env,
             '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
             '-dname', 'CN=Li Widget Local Build', '-noprompt'])
    args.work_directory.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='liwidget-', dir=args.work_directory.resolve()))
    for folder in ('gen', 'classes', 'dex'):
        (work / folder).mkdir()
    run([sdk_tool('aapt2'), 'compile', '--dir', ROOT / 'res', '-o', work / 'resources.zip'])
    run([sdk_tool('aapt2'), 'link', '-I', android, '--manifest', ROOT / 'AndroidManifest.xml',
         '--java', work / 'gen', '-o', work / 'base.apk', work / 'resources.zip'])
    java_sources = sorted((ROOT / 'src').rglob('*.java')) + sorted((work / 'gen').rglob('*.java'))
    run([java_tool('javac'), '-encoding', 'UTF-8', '--release', '8', '-g:none', '-Xlint:-options',
         '-classpath', android, '-d', work / 'classes', *java_sources])
    run([sdk_tool('d8'), '--release', '--lib', android, '--min-api', '31', '--output', work / 'dex',
         *sorted((work / 'classes').rglob('*.class'))])
    with zipfile.ZipFile(work / 'base.apk', 'a') as archive:
        for dex in sorted((work / 'dex').glob('*.dex')):
            archive.write(dex, dex.name)
    run([sdk_tool('zipalign'), '-f', '4', work / 'base.apk', work / 'aligned.apk'])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    run([sdk_tool('apksigner'), 'sign', '--ks', key, '--ks-key-alias', args.alias,
         '--ks-pass', 'env:' + args.password_env, '--out', args.output, work / 'aligned.apk'])
    run([sdk_tool('apksigner'), 'verify', '--verbose', args.output])
    run([sdk_tool('zipalign'), '-c', '4', args.output])
    print('APK: ' + str(args.output.resolve()))


if __name__ == '__main__':
    main()
