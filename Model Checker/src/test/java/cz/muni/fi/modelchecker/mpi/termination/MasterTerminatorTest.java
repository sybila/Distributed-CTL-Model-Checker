package cz.muni.fi.modelchecker.mpi.termination;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test class for master terminator.
 */
public class MasterTerminatorTest {

    /* We are not testing for situations when process count is 1 but terminator sends/receives messages,
       because in such cases, the behaviour is undefined. */

    @Rule
    public ExpectedException exception = ExpectedException.none();

    int flag;
    int count;

    @Test
    public void complexTest() {
        flag = 1;
        count = 0;
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count + count);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        final MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageSent();
        count++;    //message created in system
        terminator.messageReceived();   //message received
        terminator.setWorking(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    count--;    //pair for first message Sent
                    terminator.messageSent();
                    terminator.setWorking(false);
                    Thread.sleep(100);
                    count++; //message created in system
                    terminator.messageReceived();   //message received
                    terminator.setWorking(true);
                    Thread.sleep(100);
                    count--;    //pair for second message Sent
                    Thread.sleep(100);
                    flag = 0;
                    terminator.setWorking(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void receivedMessagesAfterStart() throws InterruptedException {
        flag = 1;
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count + 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        final MasterTerminator terminator = new MasterTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageReceived();
                    terminator.setWorking(true);
                    Thread.sleep(100);
                    terminator.messageReceived();
                    Thread.sleep(100);
                    flag = 0;
                    terminator.setWorking(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void sentMessagesAfterStart() throws InterruptedException {
        flag = 1;
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count - 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        final MasterTerminator terminator = new MasterTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                    terminator.messageSent();
                    Thread.sleep(50);
                    terminator.messageSent();
                    flag = 0;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void receivedMessagesBeforeStart() throws InterruptedException {
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(token.flag, token.count + 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        //receive and finish work before start
        MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageReceived();
        terminator.setWorking(true);
        Thread.sleep(100);
        terminator.setWorking(false);
        terminator.messageReceived();
        terminator.waitForTermination();
        //receive before start and finish after start
        final MasterTerminator terminator2 = new MasterTerminator(messenger);
        terminator2.messageReceived();
        terminator2.setWorking(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                    terminator2.setWorking(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Thread.sleep(100);
        terminator2.messageReceived();
        terminator2.waitForTermination();
    }

    @Test
    public void sentMessagesBeforeStart() throws InterruptedException {
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();
            int flag = 1;

            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    Token ret = new Token(Math.min(token.flag + flag, 1), token.count - 2);
                    flag = 0;
                    return ret;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageSent();
        Thread.sleep(100);
        terminator.messageSent();
        terminator.waitForTermination();
    }

    @Test
    public void wrongUse() {
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 1;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.waitForTermination();
        exception.expect(IllegalStateException.class);
        terminator.messageReceived();
        exception.expect(IllegalStateException.class);
        terminator.messageSent();
        exception.expect(IllegalStateException.class);
        terminator.setWorking(true);
        exception.expect(IllegalStateException.class);
        terminator.waitForTermination();
        exception.expect(IllegalStateException.class);
        new MasterTerminator(new TokenMessenger() {
            @Override
            public int getProcessCount() {
                return 4;
            }

            @Override
            public int getMyId() {
                return 2;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {

            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                return new Token(0,0);
            }
        });
    }

    @Test
    public void noMessages() {
        TokenMessenger messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 1;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.waitForTermination();
        messenger = new TokenMessenger() {

            BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public int getProcessCount() {
                return 3;
            }

            @Override
            public int getMyId() {
                return 0;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        terminator = new MasterTerminator(messenger);
        terminator.waitForTermination();
    }

}
