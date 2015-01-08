package cz.muni.fi.modelchecker.mpi.termination;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TerminatorIntegrationTest {

    List<BlockingQueue<Token>> queues = new ArrayList<>();
    int msgCount = 0;

    private class Messenger implements TokenMessenger {

        private int pCount;
        private int id;

        private Messenger(int pCount, int id) {
            this.pCount = pCount;
            this.id = id;
        }

        @Override
        public int getProcessCount() {
            return pCount;
        }

        @Override
        public int getMyId() {
            return id;
        }

        @Override
        public void sendTokenAsync(int destination, @NotNull Token token) {
            queues.get(destination).add(token);
            msgCount++;
        }

        @NotNull
        @Override
        public Token waitForToken(int source) {
            try {
                return queues.get(id).take();
            } catch (InterruptedException e) {
                return new Token(0,0);
            }
        }
    }

    @Test
    public void complexTest() throws InterruptedException {
        queues.clear();
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());

        final MasterTerminator masterTerminator = new MasterTerminator(new Messenger(5,0));
        final SlaveTerminator slaveTerminator1 = new SlaveTerminator(new Messenger(5,1));
        final SlaveTerminator slaveTerminator2 = new SlaveTerminator(new Messenger(5,2));
        final SlaveTerminator slaveTerminator3 = new SlaveTerminator(new Messenger(5,3));
        final SlaveTerminator slaveTerminator4 = new SlaveTerminator(new Messenger(5,4));

        slaveTerminator1.messageSent();
        slaveTerminator3.messageReceived();
        slaveTerminator3.setWorking(true);
        slaveTerminator1.messageSent();
        slaveTerminator2.messageSent();

        slaveTerminator3.messageReceived();
        slaveTerminator4.messageReceived();
        slaveTerminator4.setWorking(true);

        masterTerminator.messageSent();
        masterTerminator.messageSent();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                masterTerminator.waitForTermination();
            }
        });
        t1.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator1.waitForTermination();
            }
        });
        t2.start();

        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator2.waitForTermination();
            }
        });
        t3.start();

        Thread t4 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator3.waitForTermination();
            }
        });
        t4.start();

        Thread t5 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator4.waitForTermination();
            }
        });
        t5.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    slaveTerminator3.messageReceived();
                    slaveTerminator3.messageSent();
                    slaveTerminator1.messageSent();
                    Thread.sleep(150);
                    masterTerminator.messageReceived();
                    slaveTerminator4.messageSent();
                    slaveTerminator4.setWorking(false);
                    slaveTerminator2.messageReceived();
                    Thread.sleep(50);
                    slaveTerminator2.setWorking(true);
                    masterTerminator.messageReceived();
                    slaveTerminator1.messageReceived();
                    Thread.sleep(120);
                    slaveTerminator1.setWorking(true);
                    slaveTerminator3.setWorking(false);
                    slaveTerminator1.setWorking(false);
                    Thread.sleep(44);
                    slaveTerminator2.setWorking(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        System.out.println("Token message count: "+msgCount);
    }
}
