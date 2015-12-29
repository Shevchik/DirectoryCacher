package directorycacher;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.IntegerType;
import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface CLibrary extends Library {

	public static final int O_RDONLY = 0;

	public static final int PROT_READ = 1;

	public static final int MAP_SHARED = 1;
	public static final Pointer MAP_FAILED = new Pointer(-1);

	public static final int RLIMIT_NOFILE = 7;

	CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);

	Pointer mmap(Pointer desiredAddress, SizeT length, int pageProtectingFlags, int mapFlags, int fileDescriptor, OffT offset) throws LastErrorException;

	int munmap(Pointer address, SizeT length) throws LastErrorException;

	int mlock(Pointer address, SizeT length) throws LastErrorException;

	int munlock(Pointer address, SizeT length) throws LastErrorException;

	int open(String path, int flags, int mode) throws LastErrorException;

	int close(int fileDescriptor) throws LastErrorException;

	int setrlimit(int resource, ResourceLimit limit) throws LastErrorException;

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