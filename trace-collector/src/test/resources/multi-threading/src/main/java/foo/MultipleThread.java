package foo;

public class MultipleThread {
    public static void main(String[] args) {
        int n = 4; // Number of threads
        for (int i = 0; i < n; i++) {
            OneThread object = new OneThread(i);
            object.start();
        }
    }
}

class OneThread extends Thread {
    final int threadIndex;
    long threadId;

    OneThread(int threadIndex) {
        this.threadIndex = threadIndex;
    }

    public void run() {
        try {
            // Displaying the thread that is running
            threadId = Thread.currentThread().getId();
            System.out.println("Thread " + threadId + " is running which was assigned at index " + threadIndex);
        }
        catch (Exception e) {
            // Throwing an exception
            System.out.println("Exception is caught");
        }
    }
}
