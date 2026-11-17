"""Focused release-boundary checks. Standard library only; not a security audit."""
from pathlib import Path
import argparse
import os
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[1]
NS = '{http://schemas.android.com/apk/res/android}'
AVATAR_ATLAS = 'res/drawable-nodpi/vehicle_avatar_atlas.png'
PACKAGED_ATLAS = 'res/drawable-nodpi-v4/vehicle_avatar_atlas.png'
EXCLUDED = {'.git', '.local', 'build', 'dist', '__pycache__', '.idea', '.gradle'}
FORBIDDEN_SUFFIXES = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.apk',
                      '.aab', '.zip', '.idsig', '.dex', '.class', '.pyc', '.log'}
REMOTE_VIEW_TAGS = {'AdapterViewFlipper', 'FrameLayout', 'GridLayout', 'GridView',
                    'LinearLayout', 'ListView', 'RelativeLayout', 'StackView', 'ViewFlipper',
                    'AnalogClock', 'Button', 'Chronometer', 'ImageButton', 'ImageView',
                    'ProgressBar', 'TextClock', 'TextView', 'RadioButton', 'RadioGroup',
                    'CheckBox', 'Switch', 'include', 'merge'}
PRIVATE = re.compile(
    r'[A-Za-z]:[\\/](?:Users|Documents and Settings)[\\/]'
    r'|/(?:Users|home)/[A-Za-z0-9_-]+/'
    r'|-----BEGIN [A-Z ]*PRIVATE KEY-----'
    r'|\bgh[pousr]_[A-Za-z0-9]{25,}\b'
    r'|\bgithub_pat_[A-Za-z0-9_]{30,}\b'
    r'|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'
)


def check(condition, message):
    if not condition:
        raise ValueError(message)


def source_files():
    for directory, subdirs, files in os.walk(ROOT, followlinks=False):
        subdirs[:] = sorted(d for d in subdirs if d not in EXCLUDED)
        for name in sorted(files):
            path = Path(directory) / name
            check(not path.is_symlink(), 'Unexpected symlink: ' + str(path.relative_to(ROOT)))
            yield path


def check_png(data, label):
    check(data.startswith(b'\x89PNG\r\n\x1a\n'), 'Invalid PNG: ' + label)
    offset = 8
    while offset < len(data):
        size = struct.unpack_from('>I', data, offset)[0]
        kind = data[offset + 4:offset + 8]
        check(kind not in (b'tEXt', b'zTXt', b'iTXt', b'eXIf'), 'Image metadata: ' + label)
        offset += 12 + size
    check(offset == len(data), 'Unexpected PNG content: ' + label)


def translations(path):
    nodes = ET.parse(path).getroot().findall('string')
    values = {node.get('name'): ''.join(node.itertext()) for node in nodes}
    check(len(values) == len(nodes), 'Duplicate translation keys: ' + str(path))
    return values


def dex_strings(data):
    check(data[:4] == b'dex\n', 'Invalid DEX header')
    count, start = struct.unpack_from('<II', data, 56)
    for index in range(count):
        offset = struct.unpack_from('<I', data, start + 4 * index)[0]
        while data[offset] & 0x80:
            offset += 1
        offset += 1
        end = data.index(0, offset)
        yield data[offset:end].decode('utf-8', errors='replace')


def check_apk(path):
    with zipfile.ZipFile(path) as archive:
        check(archive.testzip() is None, 'Corrupt APK')
        entries = archive.namelist()
        check(len(entries) == len(set(entries)), 'Duplicate APK entries')
        for name in entries:
            suffix = Path(name).suffix.lower()
            if name in {AVATAR_ATLAS, PACKAGED_ATLAS}:
                check_png(archive.read(name), name)
                continue
            check(suffix not in {'.jks', '.keystore', '.pem', '.py', '.java', '.png', '.webp', '.jpg'},
                  'Unexpected APK content: ' + name)
        dex = [name for name in entries if name.endswith('.dex')]
        check(bool(dex), 'No application code in APK')
        for name in dex:
            for value in dex_strings(archive.read(name)):
                check(not PRIVATE.search(value), 'Private marker in DEX (value omitted)')
                check('Fixture' not in value and 'DemoSettings' not in value, 'Test class in APK')
                check('android/util/Log;' not in value, 'Production logging in APK')
                check(not value.endswith('.java'), 'Java debug metadata in APK')
    print('PASS: APK archive, approved avatar atlas, no private markers, test code or debug metadata')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    files = list(source_files())
    for path in files:
        label = path.relative_to(ROOT).as_posix()
        check(path.suffix.lower() not in FORBIDDEN_SUFFIXES and not path.name.startswith('.env'),
              'Private/generated file in source tree: ' + label)
        if path.suffix == '.png':
            check(path.parent == ROOT / 'docs' / 'images' or label == AVATAR_ATLAS,
                  'Unexpected raster resource: ' + label)
            check_png(path.read_bytes(), label)
            continue
        content = path.read_text(encoding='utf-8')
        check(not PRIVATE.search(content), 'Private marker in ' + label + ' (value omitted)')
        if path.suffix == '.xml':
            document = ET.fromstring(content)
            if path.parent == ROOT / 'res' / 'layout':
                for view in document.iter():
                    check(view.tag in REMOTE_VIEW_TAGS,
                          'Unsupported RemoteViews class in ' + label + ': ' + view.tag)
        if path.suffix == '.md':
            links = re.findall(r'\]\(([^\s)]+)(?:\s+[^)]*)?\)', content)
            links += re.findall(r'(?:src|href)="([^"]+)"', content)
            for link in links:
                if '://' in link or link.startswith('#'):
                    continue
                target = link.split('#')[0]
                if target:
                    check((path.parent / target).exists(), 'Broken local link in ' + label + ': ' + link)

    english = translations(ROOT / 'res/values/strings.xml')
    russian = translations(ROOT / 'res/values-ru/strings.xml')
    check(english.keys() == russian.keys(), 'Language keys differ')
    for key in english:
        check(re.findall(r'%\d+\$[ds]', english[key]) == re.findall(r'%\d+\$[ds]', russian[key]),
              'Format arguments differ: ' + key)

    manifest = ET.parse(ROOT / 'AndroidManifest.xml').getroot()
    check(manifest.get('package') == 'dev.li.widget', 'Unexpected application ID')
    app = manifest.find('application')
    check(app.get(NS + 'debuggable') == 'false', 'Debugging enabled')
    check(app.get(NS + 'allowBackup') == 'false', 'App backup enabled')
    check(app.get(NS + 'usesCleartextTraffic') == 'false', 'Cleartext traffic enabled')
    actual = {node.get(NS + 'name') for node in manifest.findall('uses-permission')}
    expected = {'android.permission.' + name for name in (
        'BIND_APPWIDGET', 'FOREGROUND_SERVICE', 'FOREGROUND_SERVICE_SPECIAL_USE',
        'POST_NOTIFICATIONS', 'RECEIVE_BOOT_COMPLETED')}
    check(actual == expected, 'Permission set changed: review the privacy notice and release boundary')
    for node in app:
        if node.tag in ('receiver', 'service'):
            check(node.get(NS + 'exported') == 'false', 'Exported background component')
        if node.get(NS + 'name') == '.AvatarGalleryActivity':
            check(node.get(NS + 'exported') == 'false', 'Avatar picker exported')
    check(not app.findall('provider'), 'Unexpected content provider')
    check('MIT License' in (ROOT / 'LICENSE').read_text(), 'Missing MIT license')
    print('PASS: {} source files, {} strings per language, local links, XML, privacy boundary'.format(
        len(files), len(english)))
    if args.apk:
        check_apk(args.apk)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, ET.ParseError, zipfile.BadZipFile) as error:
        print('FAIL: ' + str(error), file=sys.stderr)
        sys.exit(1)
