package cz.muni.fi.modelchecker.mpi.termination;

import cz.muni.fi.modelchecker.mpi.termination.SlaveTerminator;
import cz.muni.fi.modelchecker.mpi.termination.Token;
import cz.muni.fi.modelchecker.mpi.termination.TokenMessenger;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test class for slave terminator.
 */
public class SlaveTerminatorTest {


    @Rule
    public ExpectedException exception = ExpectedException.none();

    int flag;

    @Test
    public void manyForwardsMessagesReceivedAfter() {
        flag = 0;
        TokenMessenger messenger = new TokenMessenger() {
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                if (flag == 1 && token.flag < 1) {
                    throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                }
                if (token.count == -2) {
                    flag = 2;
                }
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                if (flag == 2) {
                    return new Token(2,0);
                } else {
                    return new Token(0,0);
                }
            }
        };
        final SlaveTerminator terminator = new SlaveTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageReceived();
                    terminator.messageReceived();
                    flag = 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }


    @Test
    public void manyForwardsMessagesSentAfter() {
        flag = 0;
        TokenMessenger messenger = new TokenMessenger() {
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                if (flag == 2 && token.flag != 0) {
                    throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                }
                if (token.count == 2) {
                    flag = 2;
                }
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                if (flag == 2) {
                    return new Token(2,0);
                } else {
                    return new Token(0,0);
                }
            }
        };
        final SlaveTerminator terminator = new SlaveTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageSent();
                    terminator.messageSent();
                    flag = 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesSentAndReceivedBefore() {
        TokenMessenger messenger = new TokenMessenger() {
            int counter = 0;
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != -1 || token.flag != 1) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageReceived();
        terminator.messageSent();
        terminator.messageReceived();
        terminator.messageSent();
        terminator.messageReceived();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesReceivedBefore() {
        TokenMessenger messenger = new TokenMessenger() {
            int counter = 0;
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != -3 || token.flag != 1) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageReceived();
        terminator.messageReceived();
        terminator.messageReceived();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesSentBefore() {
        TokenMessenger messenger = new TokenMessenger() {
            int counter = 0;
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != 2 || token.flag != 0) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageSent();
        terminator.messageSent();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardNoMessages() {
        TokenMessenger messenger = new TokenMessenger() {
            int counter = 0;
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != 0 || token.flag != 0) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.waitForTermination();
    }

    @Test
    public void wrongUse() {
        TokenMessenger messenger = new TokenMessenger() {
            @Override
            public int getProcessCount() {
                return 2;
            }

            @Override
            public int getMyId() {
                return 1;
            }

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                if (token.flag != 2 && destination != 0) {
                    throw new UnsupportedOperationException("Token was forwarded but no token was received");
                }
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                return new Token(2,0);
            }
        };
        SlaveTerminator terminator = new SlaveTerminator(messenger);
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
        new SlaveTerminator(new TokenMessenger() {
            @Override
            public int getProcessCount() {
                return 4;
            }

            @Override
            public int getMyId() {
                return 0;
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


}
