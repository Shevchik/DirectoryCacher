package directorycacher;

import java.nio.file.Path;
import java.util.HashMap;

import com.sun.jna.LastErrorException;
import com.sun.jna.Pointer;

import directorycacher.CLibrary.SizeT;

public class FileLocker {

	private final HashMap<String, LockedFileInfo> lockedFiles = new HashMap<>();

	private static class LockedFileInfo {
		int fd;
		Pointer addr;
		long length;
		public LockedFileInfo(int fd, Pointer addr, long length) {
			this.fd = fd;
			this.addr = addr;
			this.length = length;
		}
	}

	public void lockFile(Path path) {
		String absolutefilepath = path.toAbsolutePath().toString();
		long fileSize = path.toFile().length();
		if (fileSize == 0) {
			return;
		}
		int fd = -1;
		try {
			fd = CLibrary.INSTANCE.open(absolutefilepath, CLibrary.O_RDONLY, 0);
			Pointer addr = CLibrary.INSTANCE.mmap(null, new SizeT(fileSize), CLibrary.PROT_READ, CLibrary.MAP_SHARED, fd, new CLibrary.OffT());
			CLibrary.INSTANCE.mlock(addr, new SizeT(fileSize));
			lockedFiles.put(absolutefilepath, new LockedFileInfo(fd, addr, fileSize));
			ExceptionShutdown.log("Locked "+absolutefilepath);
		} catch (LastErrorException e) {
			if (fd != -1) {
				CLibrary.INSTANCE.close(fd);
			}
			ExceptionShutdown.err("Failed to lock file "+absolutefilepath + ", err code: "+e.getErrorCode());
		}
	}

	public void unlockFile(Path path) {
		String absolutefilepath = path.toAbsolutePath().toString();
		LockedFileInfo fileinfo = lockedFiles.remove(absolutefilepath);
		if (fileinfo == null) {
			return;
		}
		CLibrary.INSTANCE.munlock(fileinfo.addr, new SizeT(fileinfo.length));
		CLibrary.INSTANCE.munmap(fileinfo.addr, new SizeT(fileinfo.length));
		CLibrary.INSTANCE.close(fileinfo.fd);
		ExceptionShutdown.log("Unlocked "+absolutefilepath);
	}

	public void relockFile(Path path) {
		String absolutefilepath = path.toAbsolutePath().toString();
		LockedFileInfo fileinfo = lockedFiles.get(absolutefilepath);
		if (fileinfo == null) {
			return;
		}
		try {
			CLibrary.INSTANCE.munlock(fileinfo.addr, new SizeT(fileinfo.length));
			CLibrary.INSTANCE.munmap(fileinfo.addr, new SizeT(fileinfo.length));
			long fileSize = path.toFile().length();
			if (fileSize == 0) {
				return;
			}
			fileinfo.addr = CLibrary.INSTANCE.mmap(null, new SizeT(fileSize), CLibrary.PROT_READ, CLibrary.MAP_SHARED, fileinfo.fd, new CLibrary.OffT());
			fileinfo.length = fileSize;
			CLibrary.INSTANCE.mlock(fileinfo.addr, new SizeT(fileinfo.length));
		} catch (LastErrorException e) {
			CLibrary.INSTANCE.close(fileinfo.fd);
			ExceptionShutdown.err("Failed to relock file "+absolutefilepath + ", err code: "+e.getErrorCode());
		}
	}

}
