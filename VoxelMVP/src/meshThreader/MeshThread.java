package meshThreader;


public class MeshThread implements Runnable {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
            MeshTask task = MeshQueue.meshInputQueue.poll();
            if (task != null) {
                
            } else {
            	
            }
        }
	}

}
