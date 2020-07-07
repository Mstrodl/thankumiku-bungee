package tech.coolmathgames.thankumiku;

public class TimeoutThread extends Thread {
    private boolean stopped;
    private Server server;

    TimeoutThread(String name, Server server) {
        super(name);
        this.server = server;
    }

    public void cancel() {
        this.stopped = true;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.server.shutdownTime);
            if (!this.stopped && this.server.queue.size() == 0) {
                this.server.timeout = null;
                this.server.shutdown();
            }
        } catch(InterruptedException e) {
//            This is fine
        }
    }
}
