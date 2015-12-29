package directorycacher;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.IntegerType;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class CLibrary {

	public static final int O_RDONLY = 0;

	public static final int PROT_READ = 1;

	public static final int MAP_SHARED = 1;
	public static final Pointer MAP_FAILED = new Pointer(-1);

	public static final int RLIMIT_NOFILE = 7;

	static {
		Native.register(Platform.C_LIBRARY_NAME);
	}

	public static native Pointer mmap(Pointer desiredAddress, SizeT length, int pageProtectingFlags, int mapFlags, int fileDescriptor, OffT offset) throws LastErrorException;

	public static native int munmap(Pointer address, SizeT length) throws LastErrorException;

	public static native int mlock(Pointer address, SizeT length) throws LastErrorException;

	public static native int munlock(Pointer address, SizeT length) throws LastErrorException;

	public static native int open(String path, int flags, int mode) throws LastErrorException;

	public static native int close(int fileDescriptor) throws LastErrorException;

	public static native int setrlimit(int resource, ResourceLimit limit) throws LastErrorException;

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

	public static class ResourceLimit extends Structure {
		public long rlim_cur;
		public long rlim_max;

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("rlim_cur", "rlim_max");
		}
	}

}