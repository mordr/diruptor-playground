import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

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

        private final int ordinal;
        private final int numConsumers;

        public LongEventHandler(final int ordinal, final int numConsumers) {
            this.ordinal = ordinal;
            this.numConsumers = numConsumers;
        }

        @Override
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
            if (sequence % numConsumers == ordinal) {
                System.out.println(">> Consumer: " + ordinal + " Consuming event: " + event.get());
                Thread.currentThread().sleep(600);
            }
        }

    }

    public static void main(String[] args) {
        Playground outer = new Playground();

        int MAX = 100;
        int bufferSize = 32;

        LongEventFactory factory = outer.new LongEventFactory();

        Disruptor<LongEvent> disruptor =
                new Disruptor<>(factory, bufferSize, Executors.defaultThreadFactory());
        int numConsumers = 2;
        LongEventHandler oddSlotEventHandler = outer.new LongEventHandler(0, numConsumers);
        LongEventHandler evenSlotEventHandler = outer.new LongEventHandler(1, numConsumers);
        disruptor.handleEventsWith(oddSlotEventHandler, evenSlotEventHandler);

        System.out.println("Start");
        System.out.println("Starting disruptor");
        disruptor.start();


        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        int i = 0;
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
            disruptor.shutdown(1, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            e.printStackTrace();
            disruptor.halt();
            System.out.println("Invoke halt because of timeout");
        }
        System.out.println("Done");
    }
}