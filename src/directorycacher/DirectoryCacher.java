package directorycacher;

import java.io.File;

import com.sun.jna.Platform;

public class DirectoryCacher {

	public static void main(String[] args) {
		try {
			if (!Platform.isLinux()) {
				throw new IllegalStateException("Not a linux platform");
			}
			if (!Platform.is64Bit()) {
				throw new IllegalStateException("Not a 64bit platform");
			}
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
