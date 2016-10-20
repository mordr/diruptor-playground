import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;

public class DisruptorQueuePlayground {

    public class Producer implements Runnable {

        private final DisruptorBlockingQueue<Integer> queue;

        public Producer(final DisruptorBlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            int i = 0;
            while (true) {
                try {
                    queue.put(i);
                    System.out.println("Produced event: " + i);
                    ++i;
                    Thread.currentThread().sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    public class Consumer implements Runnable {

        private final DisruptorBlockingQueue<Integer> queue;
        private final int num;

        public Consumer(final DisruptorBlockingQueue<Integer> queue, final int num) {
            this.queue = queue;
            this.num = num;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Integer i = (Integer) queue.take();
                    System.out.println("Consumer " + num + " consumed event: " + i);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        DisruptorQueuePlayground outer = new DisruptorQueuePlayground();

        int bufferSize = 1024;
        DisruptorBlockingQueue<Integer> queue = new DisruptorBlockingQueue<>(bufferSize);

        Producer producer = outer.new Producer(queue);
        Consumer consumer1 = outer.new Consumer(queue, 1);
        Consumer consumer2 = outer.new Consumer(queue, 2);
        Thread producerThread = new Thread(producer);
        Thread consumer1Thread = new Thread(consumer1);
        Thread consumer2Thread = new Thread(consumer2);

        producerThread.start();
        consumer1Thread.start();
        consumer2Thread.start();

    }

}
