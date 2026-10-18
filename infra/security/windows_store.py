"""Windows-only filesystem primitives. No shell ACL commands or elevation.

Handles pin the complete namespace without delete sharing. Security is checked
on opened objects, not on an earlier pathname lookup. The developer account,
SYSTEM and administrators are trusted; this is not same-account isolation.
"""
import contextlib
import ctypes as C
from ctypes import wintypes as W
from dataclasses import dataclass, asdict
import errno
import os
from pathlib import Path
import re
import struct


class SecurityError(RuntimeError):
    """Messages are fixed reason codes: never include paths, bytes or OS text."""


@dataclass(frozen=True)
class Presence:
    """Metadata-only snapshot, never a lease across a later pathname operation."""
    state: str
    category: str
    code: int | None = None

    def record(self):
        return asdict(self)


def query_failure(error, parent_verified):
    """Only narrowly classified not-found beneath verified ancestors proves absence."""
    winerror = getattr(error, 'winerror', None)
    code = winerror if winerror is not None else error.errno
    not_found = winerror in (2, 3) if winerror is not None else (
        isinstance(error, FileNotFoundError) and error.errno == errno.ENOENT)
    if not_found:
        return Presence('ABSENT', 'VERIFIED_NOT_FOUND', code) if parent_verified else Presence('UNKNOWN', 'UNVERIFIED_ROOT', code)
    if winerror is not None:
        category = {5: 'ACCESS_DENIED', 32: 'SHARING_VIOLATION', 33: 'SHARING_VIOLATION',
                    21: 'IO_ERROR', 23: 'IO_ERROR', 1117: 'IO_ERROR'}.get(winerror, 'OS_ERROR')
    else:
        category = {errno.EACCES: 'ACCESS_DENIED', errno.EPERM: 'ACCESS_DENIED',
                    errno.EIO: 'IO_ERROR'}.get(error.errno, 'OS_ERROR')
    return Presence('UNKNOWN', category, code)


def verified_absent(path, backend):
    result = backend.presence(path)
    require(result.state in ('PRESENT', 'ABSENT'), 'PRESENCE_UNKNOWN_' + result.category +
            (f'_{result.code}' if result.code is not None else ''))
    return result.state == 'ABSENT'


READ = 0x120089
DIRECTORY = 0x1200AF
METADATA = 0x13019F
RECOVERY = 0x60000
TRUSTED = {'S-1-5-18', 'S-1-5-32-544',
           'S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464'}
MUTATION = 0xD0156  # write/append/EA/attributes, delete-child/delete/DACL/owner


def require(value, reason):
    if not value:
        raise SecurityError(reason)


def namespace_safe(owner, aces, developer, volume_root=False):
    """Applicable Allow entries must not confer untrusted mutation rights.

    Unknown/deny/object/callback ACEs fail closed rather than approximating
    their effective-access behavior. Broad directory creation on a volume root
    cannot rename an existing protected descendant and is allowed there only.
    """
    require(owner in TRUSTED | {developer}, 'UNTRUSTED_ANCESTOR_OWNER')
    for sid, mask, flags, kind in aces:
        require(kind == 0, 'UNSUPPORTED_ACE')
        if flags & 8:  # INHERIT_ONLY_ACE is not effective on this object
            continue
        if sid not in TRUSTED | {developer}:
            forbidden = MUTATION & (~4 if volume_root else 0xFFFFFFFF)
            require(not mask & (forbidden | 0x50000000), 'UNSAFE_ANCESTOR_ACCESS')


class Windows:
    def __init__(self):
        require(os.name == 'nt', 'WINDOWS_REQUIRED')
        self.k = C.WinDLL('kernel32', use_last_error=True)
        self.a = C.WinDLL('advapi32', use_last_error=True)
        self.shell = C.WinDLL('shell32', use_last_error=True)
        self.ole = C.WinDLL('ole32')
        signatures = [
            (self.k, 'CreateFileW', [W.LPCWSTR, W.DWORD, W.DWORD, W.LPVOID, W.DWORD, W.DWORD, W.HANDLE], W.HANDLE),
            (self.k, 'CloseHandle', [W.HANDLE], W.BOOL),
            (self.k, 'GetCurrentProcess', [], W.HANDLE),
            (self.k, 'LocalFree', [W.LPVOID], W.LPVOID),
            (self.k, 'CreateDirectoryW', [W.LPCWSTR, W.LPVOID], W.BOOL),
            (self.k, 'GetFinalPathNameByHandleW', [W.HANDLE, W.LPWSTR, W.DWORD, W.DWORD], W.DWORD),
            (self.k, 'GetFileInformationByHandle', [W.HANDLE, W.LPVOID], W.BOOL),
            (self.k, 'ReadFile', [W.HANDLE, W.LPVOID, W.DWORD, W.LPVOID, W.LPVOID], W.BOOL),
            (self.k, 'WriteFile', [W.HANDLE, W.LPVOID, W.DWORD, W.LPVOID, W.LPVOID], W.BOOL),
            (self.k, 'FlushFileBuffers', [W.HANDLE], W.BOOL),
            (self.k, 'ReplaceFileW', [W.LPCWSTR, W.LPCWSTR, W.LPCWSTR, W.DWORD, W.LPVOID, W.LPVOID], W.BOOL),
            (self.k, 'DeleteFileW', [W.LPCWSTR], W.BOOL),
            (self.k, 'RemoveDirectoryW', [W.LPCWSTR], W.BOOL),
            (self.a, 'OpenProcessToken', [W.HANDLE, W.DWORD, W.LPVOID], W.BOOL),
            (self.a, 'GetTokenInformation', [W.HANDLE, C.c_int, W.LPVOID, W.DWORD, W.LPVOID], W.BOOL),
            (self.a, 'ConvertSidToStringSidW', [W.LPVOID, W.LPVOID], W.BOOL),
            (self.a, 'ConvertStringSecurityDescriptorToSecurityDescriptorW', [W.LPCWSTR, W.DWORD, W.LPVOID, W.LPVOID], W.BOOL),
            (self.a, 'GetSecurityInfo', [W.HANDLE, C.c_int, W.DWORD, W.LPVOID, W.LPVOID, W.LPVOID, W.LPVOID, W.LPVOID], W.DWORD),
            (self.a, 'GetSecurityDescriptorControl', [W.LPVOID, W.LPVOID, W.LPVOID], W.BOOL),
            (self.a, 'GetAce', [W.LPVOID, W.DWORD, W.LPVOID], W.BOOL),
            (self.a, 'SetSecurityInfo', [W.HANDLE, C.c_int, W.DWORD, W.LPVOID, W.LPVOID, W.LPVOID, W.LPVOID], W.DWORD),
            (self.a, 'GetSecurityDescriptorDacl', [W.LPVOID, W.LPVOID, W.LPVOID, W.LPVOID], W.BOOL),
            (self.shell, 'SHGetKnownFolderPath', [W.LPVOID, W.DWORD, W.HANDLE, W.LPVOID], C.c_long),
            (self.ole, 'CoTaskMemFree', [W.LPVOID], None),
        ]
        for dll, name, args, result in signatures:
            function = getattr(dll, name)
            function.argtypes, function.restype = args, result
        self.sid = self._current_sid()

    def _sid(self, pointer):
        out = W.LPWSTR()
        require(self.a.ConvertSidToStringSidW(pointer, C.byref(out)), 'SID_QUERY_FAILED')
        try:
            return out.value
        finally:
            self.k.LocalFree(C.cast(out, W.LPVOID))

    def _current_sid(self):
        token = W.HANDLE()
        require(self.a.OpenProcessToken(self.k.GetCurrentProcess(), 8, C.byref(token)), 'TOKEN_QUERY_FAILED')
        try:
            length = W.DWORD()
            self.a.GetTokenInformation(token, 1, None, 0, C.byref(length))
            buffer = C.create_string_buffer(length.value)
            require(self.a.GetTokenInformation(token, 1, buffer, length, C.byref(length)), 'TOKEN_QUERY_FAILED')
            return self._sid(C.c_void_p.from_buffer(buffer).value)
        finally:
            self.k.CloseHandle(token)

    def known_folder(self):
        import uuid
        guid = C.create_string_buffer(uuid.UUID('f1b32785-6fba-4fcf-9d55-7b8e7f157091').bytes_le)
        pointer = W.LPWSTR()
        require(self.shell.SHGetKnownFolderPath(guid, 0, None, C.byref(pointer)) == 0, 'KNOWN_FOLDER_FAILED')
        try:
            return Path(pointer.value)
        finally:
            self.ole.CoTaskMemFree(C.cast(pointer, W.LPVOID))

    def _presence_open(self, path):
        """Existence-query boundary: OPEN_EXISTING, metadata access, no content read."""
        h = self.k.CreateFileW(str(path), 0x20080, 3, None, 3, 0x02200000, None)
        if h == W.HANDLE(-1).value:
            code = C.get_last_error()  # Capture before any other native call.
            error = OSError(code, 'PRESENCE_QUERY_FAILED')
            error.winerror = code
            raise error
        return h

    def presence(self, path):
        path = Path(path).absolute()
        if not re.fullmatch(r'[A-Za-z]:\\', path.anchor):
            return Presence('UNKNOWN', 'UNSAFE_NAMESPACE')
        parent_verified = False
        with contextlib.ExitStack() as stack:
            for node in list(reversed(path.parents)) + [path]:
                try:
                    h = self._presence_open(node)
                except OSError as error:
                    return query_failure(error, parent_verified)
                stack.callback(self.k.CloseHandle, h)
                try:
                    info = self.info(h, node)  # Reject redirects/hardlinks on the opened object.
                except OSError as error:
                    return query_failure(error, False)  # Open success never makes failed inspection ABSENT.
                except SecurityError as error:
                    category = 'UNSAFE_OBJECT' if str(error) in (
                        'REPARSE_REJECTED', 'HARDLINK_REJECTED', 'CANONICAL_PATH_MISMATCH') else 'OBJECT_VERIFICATION_FAILED'
                    return Presence('UNKNOWN', category)
                if node == path:
                    return Presence('PRESENT', 'VERIFIED_OBJECT')
                try:
                    require(info['directory'], 'DIRECTORY_REQUIRED')
                    sec = self.security(h)
                    namespace_safe(sec['owner'], sec['aces'], self.sid, node == Path(node.anchor))
                except OSError as error:
                    return query_failure(error, False)
                except SecurityError as error:
                    category = 'UNSAFE_NAMESPACE' if str(error) in (
                        'UNTRUSTED_ANCESTOR_OWNER', 'UNSAFE_ANCESTOR_ACCESS', 'UNSUPPORTED_ACE',
                        'NULL_DACL_REJECTED', 'DIRECTORY_REQUIRED') else 'NAMESPACE_VERIFICATION_FAILED'
                    return Presence('UNKNOWN', category)
                parent_verified = True

    @contextlib.contextmanager
    def descriptor(self, kind):
        mask = {'directory': DIRECTORY, 'credential': READ, 'metadata': METADATA,
                'cleanup': READ | 0x10000}[kind]
        inheritance = f'(A;CIIO;0x{DIRECTORY:x};;;{self.sid})(A;OIIO;FR;;;{self.sid})' if kind == 'directory' else ''
        sddl = f'O:{self.sid}G:{self.sid}D:P(A;;0x{mask:x};;;{self.sid}){inheritance}(A;{"OICI" if kind == "directory" else ""};RCWD;;;BA)'
        pointer = W.LPVOID()
        require(self.a.ConvertStringSecurityDescriptorToSecurityDescriptorW(sddl, 1, C.byref(pointer), None), 'DESCRIPTOR_FAILED')
        try:
            yield pointer
        finally:
            self.k.LocalFree(pointer)

    @contextlib.contextmanager
    def handle(self, path, access=0x20080):
        h = self.k.CreateFileW(str(path), access, 3, None, 3, 0x02200000, None)
        require(h != W.HANDLE(-1).value, 'OPEN_FAILED')
        try:
            self.info(h, path)
            yield h
        finally:
            self.k.CloseHandle(h)

    def info(self, h, path):
        # BY_HANDLE_FILE_INFORMATION, stable layout of thirteen DWORDs.
        buffer = (W.DWORD * 13)()
        require(self.k.GetFileInformationByHandle(h, buffer), 'IDENTITY_FAILED')
        require(not buffer[0] & 0x400, 'REPARSE_REJECTED')
        require(bool(buffer[0] & 16) or buffer[10] == 1, 'HARDLINK_REJECTED')
        name = C.create_unicode_buffer(32768)
        require(self.k.GetFinalPathNameByHandleW(h, name, len(name), 0), 'CANONICAL_PATH_FAILED')
        require(os.path.normcase(name.value.removeprefix('\\\\?\\')) == os.path.normcase(str(Path(path).absolute())), 'CANONICAL_PATH_MISMATCH')
        return {'volume': buffer[7], 'file': (buffer[11] << 32) | buffer[12],
                'directory': bool(buffer[0] & 16), 'size': (buffer[8] << 32) | buffer[9],
                'write': (buffer[6] << 32) | buffer[5]}

    def security(self, h):
        owner, dacl, sd = W.LPVOID(), W.LPVOID(), W.LPVOID()
        require(self.a.GetSecurityInfo(h, 1, 5, C.byref(owner), None, C.byref(dacl), None, C.byref(sd)) == 0, 'SECURITY_QUERY_FAILED')
        try:
            require(dacl.value, 'NULL_DACL_REJECTED')
            count = C.c_ushort.from_address(dacl.value + 4).value
            aces = []
            for index in range(count):
                ace = W.LPVOID()
                require(self.a.GetAce(dacl, index, C.byref(ace)), 'ACE_QUERY_FAILED')
                header = C.string_at(ace, 8)
                kind, flags, _, mask = struct.unpack('<BBHI', header)
                require(kind in (0, 1), 'UNSUPPORTED_ACE')
                aces.append((self._sid(ace.value + 8), mask, flags, kind))
            control, revision = W.WORD(), W.DWORD()
            require(self.a.GetSecurityDescriptorControl(sd, C.byref(control), C.byref(revision)), 'SECURITY_QUERY_FAILED')
            return {'owner': self._sid(owner), 'protected': bool(control.value & 0x1000), 'aces': aces}
        finally:
            self.k.LocalFree(sd)

    @contextlib.contextmanager
    def pin(self, path):
        require(bool(re.fullmatch(r'[A-Za-z]:\\', path.anchor)), 'LOCAL_VOLUME_REQUIRED')
        with contextlib.ExitStack() as stack:
            for ancestor in list(reversed(path.parents)) + [path]:
                h = stack.enter_context(self.handle(ancestor))
                sec = self.security(h)
                namespace_safe(sec['owner'], sec['aces'], self.sid, ancestor == Path(ancestor.anchor))
                require(self.info(h, ancestor)['directory'], 'DIRECTORY_REQUIRED')
            yield

    def validate(self, path, kind):
        with self.handle(path) as h:
            return self.validate_handle(h, path, kind)

    def validate_handle(self, h, path, kind):
        sec = self.security(h)
        expected = {'directory': DIRECTORY, 'credential': READ, 'metadata': METADATA}[kind]
        require(sec['owner'] == self.sid and sec['protected'], 'PRIVATE_POLICY_REQUIRED')
        effective = [(sid, mask, flags & ~16, typ) for sid, mask, flags, typ in sec['aces'] if not flags & 8]
        wanted = [(self.sid, expected, 0, 0), ('S-1-5-32-544', RECOVERY, 3 if kind == 'directory' else 0, 0)]
        require(sorted(effective) == sorted(wanted), 'PRIVATE_POLICY_MISMATCH')
        if kind == 'directory':
            require(sorted(sec['aces']) == sorted([(self.sid, DIRECTORY, 0, 0),
                (self.sid, DIRECTORY, 10, 0), (self.sid, READ, 9, 0),
                ('S-1-5-32-544', RECOVERY, 3, 0)]), 'INHERITANCE_POLICY_MISMATCH')
        require(self.info(h, path)['directory'] == (kind == 'directory'), 'OBJECT_TYPE_MISMATCH')
        return self.info(h, path)

    @contextlib.contextmanager
    def attributes(self, kind):
        class Attributes(C.Structure):
            _fields_ = [('length', W.DWORD), ('descriptor', W.LPVOID), ('inherit', W.BOOL)]
        with self.descriptor(kind) as sd:
            value = Attributes(C.sizeof(Attributes), sd, False)
            yield C.byref(value)

    def mkdir(self, path):
        with self.pin(path.parent), self.attributes('directory') as attributes:
            require(self.k.CreateDirectoryW(str(path), attributes), 'DIRECTORY_COLLISION_OR_CREATE_FAILED')
            self.validate(path, 'directory')

    def create(self, path, data, kind):
        with self.pin(path.parent), self.attributes(kind) as attributes:
            h = self.k.CreateFileW(str(path), 0x40000000, 0, attributes, 1, 0x00200080, None)
            require(h != W.HANDLE(-1).value, 'FILE_COLLISION_OR_CREATE_FAILED')
            try:
                self.validate_handle(h, path, kind)  # Verify the opened policy BEFORE bytes.
                count = W.DWORD()
                buffer = C.create_string_buffer(data)
                require(self.k.WriteFile(h, buffer, len(data), C.byref(count), None) and count.value == len(data), 'WRITE_FAILED')
                require(self.k.FlushFileBuffers(h), 'FLUSH_FAILED')
            finally:
                self.k.CloseHandle(h)
            self.validate(path, kind)

    def read(self, path, kind):
        with self.pin(path.parent), self.handle(path, READ) as h:
            identity = self.validate_handle(h, path, kind)
            require(identity['size'] <= 1024 * 1024, 'FILE_TOO_LARGE')
            count = W.DWORD()
            buffer = C.create_string_buffer(identity['size'] + 1)
            require(self.k.ReadFile(h, buffer, identity['size'], C.byref(count), None)
                    and count.value == identity['size'], 'READ_FAILED')
            require(self.info(h, path) == identity, 'IDENTITY_CHANGED')
            return buffer.raw[:count.value]

    @contextlib.contextmanager
    def hold_lock(self, path):
        """Exclusive live-process claim. Only explicit reconciliation reopens stale locks."""
        with self.pin(path.parent):
            h = self.k.CreateFileW(str(path), 0xC0020000, 0, None, 3, 0x00200080, None)
            require(h != W.HANDLE(-1).value, 'LOCK_BUSY_OR_INVALID')
            try:
                yield self.validate_handle(h, path, 'metadata')
            finally:
                self.k.CloseHandle(h)

    def replace(self, target, temporary, backup):
        with self.pin(target.parent):
            self.validate(target, 'metadata')
            self.validate(temporary, 'metadata')
            require(verified_absent(backup, self), 'BACKUP_COLLISION')
            require(self.k.ReplaceFileW(str(target), str(temporary), str(backup), 0, None, None), 'REPLACE_FAILED')
            self.validate(target, 'metadata')
            self.validate(backup, 'metadata')

    def inspect(self, path):
        with self.handle(path) as h:
            return {**self.info(h, path), **self.security(h)}

    def remove_owned(self, path, identity):
        """Identity-checked cleanup; final pathname delete is not same-user race isolation."""
        with self.pin(path.parent), self.handle(path, 0x60080) as h:
            require(self.info(h, path) == identity, 'CLEANUP_IDENTITY_CHANGED')
            require(self.security(h)['owner'] == self.sid, 'CLEANUP_OWNER_CHANGED')
            with self.descriptor('cleanup') as sd:
                present, defaulted, acl = W.BOOL(), W.BOOL(), W.LPVOID()
                require(self.a.GetSecurityDescriptorDacl(sd, C.byref(present), C.byref(acl), C.byref(defaulted)), 'CLEANUP_POLICY_FAILED')
                # READ+DELETE only; never give a credential metadata write rights.
                require(self.a.SetSecurityInfo(h, 1, 0x80000004, None, None, acl, None) == 0, 'CLEANUP_POLICY_FAILED')
        operation = self.k.RemoveDirectoryW if identity['directory'] else self.k.DeleteFileW
        require(operation(str(path)), 'OWNED_CLEANUP_FAILED')
