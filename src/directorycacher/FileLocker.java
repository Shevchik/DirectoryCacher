package directorycacher;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jna.IntegerType;
import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class FileLocker {

	private final ConcurrentHashMap<String, LockedFileInfo> lockedFiles = new ConcurrentHashMap<>();

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
			fd = clibrary.open(absolutefilepath, CLibrary.O_RDONLY, 0);
			Pointer addr = clibrary.mmap(null, new SizeT(fileSize), CLibrary.PROT_READ, CLibrary.MAP_SHARED, fd, new OffT());
			clibrary.mlock(addr, new SizeT(fileSize));
			lockedFiles.put(absolutefilepath, new LockedFileInfo(fd, addr, fileSize));
			ExceptionShutdown.log("Locked "+absolutefilepath);
		} catch (LastErrorException e) {
			if (fd != -1) {
				clibrary.close(fd);
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
		clibrary.munlock(fileinfo.addr, new SizeT(fileinfo.length));
		clibrary.munmap(fileinfo.addr, new SizeT(fileinfo.length));
		clibrary.close(fileinfo.fd);
		ExceptionShutdown.log("Unlocked "+absolutefilepath);
	}

	public void relockFile(Path path) {
		String absolutefilepath = path.toAbsolutePath().toString();
		LockedFileInfo fileinfo = lockedFiles.get(absolutefilepath);
		if (fileinfo == null) {
			return;
		}
		try {
			clibrary.munlock(fileinfo.addr, new SizeT(fileinfo.length));
			clibrary.munmap(fileinfo.addr, new SizeT(fileinfo.length));
			long fileSize = path.toFile().length();
			if (fileSize == 0) {
				return;
			}
			fileinfo.addr = clibrary.mmap(null, new SizeT(fileSize), CLibrary.PROT_READ, CLibrary.MAP_SHARED, fileinfo.fd, new OffT());
			fileinfo.length = fileSize;
			clibrary.mlock(fileinfo.addr, new SizeT(fileinfo.length));
		} catch (LastErrorException e) {
			clibrary.close(fileinfo.fd);
			ExceptionShutdown.err("Failed to relock file "+absolutefilepath + ", err code: "+e.getErrorCode());
		}
	}

	private static final CLibrary clibrary = (CLibrary) Native.loadLibrary("c", CLibrary.class);

	public interface CLibrary extends Library {

		static final int O_RDONLY = 0;

		static final int PROT_READ = 1;

		static final int MAP_SHARED = 1;
		static final Pointer MAP_FAILED = new Pointer(-1);

		Pointer mmap(Pointer desiredAddress, SizeT length, int pageProtectingFlags, int mapFlags, int fileDescriptor, OffT offset) throws LastErrorException;

		int munmap(Pointer address, SizeT length) throws LastErrorException;

		int mlock(Pointer address, SizeT length) throws LastErrorException;

		int munlock(Pointer address, SizeT length) throws LastErrorException;

		int open(String path, int flags, int mode) throws LastErrorException;

		int close(int fileDescriptor) throws LastErrorException;
	}

	public static class SizeT extends IntegerType {
		private static final long serialVersionUID = 1L;

		public SizeT() {
			this(0);
		}

		public SizeT(long value) {
			super(Native.SIZE_T_SIZE, value, true);
		}
	}

	public static class OffT extends IntegerType {
		private static final long serialVersionUID = 1L;

		public OffT() {
			this(0);
		}

		public OffT(long value) {
			super(Pointer.SIZE, value);
		}
	}

}
