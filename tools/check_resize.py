"""Check responsive widget geometry against a locally compiled release."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work-directory', type=Path, default=Path('build'))
    parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    args = parser.parse_args()
    builds = list(args.work_directory.glob('liwidget-*/aligned.apk'))
    if not builds or not args.sdk:
        parser.error('Build the app first and provide the Android SDK.')
    classes = max(builds, key=lambda file: file.stat().st_mtime).parent / 'classes'
    classpath = os.pathsep.join(map(str, (classes, Path(args.sdk) / 'platforms/android-36/android.jar')))
    with tempfile.TemporaryDirectory() as compiled:
        subprocess.run(['javac', '-cp', classpath, '-d', compiled, Path(__file__).with_name('ResizeCheck.java')], check=True)
        subprocess.run(['java', '-cp', compiled + os.pathsep + classpath, 'dev.li.widget.ResizeCheck'], check=True)


if __name__ == '__main__':
    main()
