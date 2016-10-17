import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;

public class Playground {

    public class LongEvent {
        private long value;

        public void set(long value) {
            this.value = value;
        }

        public long get() {
            return value;
        }
    }

    public class LongEventFactory implements EventFactory<LongEvent> {

        @Override
        public LongEvent newInstance() {
            return new LongEvent();
        }
    }

    public class LongEventHandler implements EventHandler<LongEvent> {

        @Override
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println(">> Consuming event: " + event.get());
            Thread.currentThread().sleep(1000);
        }

    }

    public static void main(String[] args) {
        Playground outer = new Playground();

        int bufferSize = 32;
        LongEventFactory factory = outer.new LongEventFactory();

        Disruptor<LongEvent> disruptor =
                new Disruptor<>(factory, bufferSize, Executors.defaultThreadFactory());
        LongEventHandler eventHandler = outer.new LongEventHandler();
        disruptor.handleEventsWith(eventHandler);

        System.out.println("Start");
        System.out.println("Starting disruptor");
        disruptor.start();


        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        int i = 0;
        int MAX = 100;
        while (i < MAX) {
            try {
                long sequence = ringBuffer.tryNext();
                LongEvent event = ringBuffer.get(sequence);
                event.set(i);
                ringBuffer.publish(sequence);
                System.out.println("-- Produced event: " + i);
                i = i + 1;
            } catch (InsufficientCapacityException e) {
                System.out.println("ERROR: Hit capacity of buffer");
                continue;
            }

            if (i < MAX) {
                System.out.println("Next event value: " + i);
            }

            try {
                Thread.currentThread().sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Initiating shutdown");
        try {
            disruptor.halt();
            System.out.println("Invoke halt");
            disruptor.shutdown(2, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            e.printStackTrace();
            disruptor.halt();
            System.out.println("Invoke halt");
        }
        System.out.println("Done");
    }
}