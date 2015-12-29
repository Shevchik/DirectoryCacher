package directorycacher;

import java.io.File;
import java.nio.file.Files;

import com.sun.jna.Platform;

import directorycacher.CLibrary.ResourceLimit;

public class DirectoryCacher {

	public static void main(String[] args) {
		try {
			if (!Platform.isLinux()) {
				throw new IllegalStateException("Not a linux platform");
			}
			if (!Platform.is64Bit()) {
				throw new IllegalStateException("Not a 64bit platform");
			}

			int procMax = Integer.parseInt(Files.readAllLines(new File("/proc/sys/fs/nr_open").toPath()).get(0));
			ResourceLimit rlimit = new ResourceLimit();
			rlimit.rlim_cur = procMax;
			rlimit.rlim_max = procMax;
			CLibrary.INSTANCE.setrlimit(CLibrary.RLIMIT_NOFILE, rlimit);

			new InfiniteSleeperThread().start();

			DirectoryList list = new DirectoryList();
			list.load("dirlist");
			for (File directory : list.getList()) {
				new DirectoryWatchService(directory).start();
			}
			for (String arg : args) {
				new DirectoryWatchService(new File(arg)).start();
			}
		} catch (Throwable t) {
			ExceptionShutdown.shutdown("Exception while initializing", t);
		}
	}

}
