package directorycacher;

public class InfiniteSleeperThread extends Thread {

	@Override
	public void run() {
		try {
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
		}
	}

}
