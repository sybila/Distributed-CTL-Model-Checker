package mpi;

import java.util.List;

public class MockComm extends Comm {

    private final int size;
    private final int rank;

    private final List<MockComm> other;

    public MockComm(int size, int rank, List<MockComm> other) {
        this.size = size;
        this.rank = rank;
        this.other = other;
    }

    @Override
    public int Size() throws MPIException {
        return size;
    }

    @Override
    public int Rank() throws MPIException {
        return rank;
    }

    @Override
    public void Send(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPIException {
        super.Send(buf, offset, count, datatype, dest, tag);
    }

    @Override
    public Request Isend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPIException {
        return super.Isend(buf, offset, count, datatype, dest, tag);
    }

    @Override
    public Status Recv(Object buf, int offset, int count, Datatype datatype, int source, int tag) throws MPIException {
        return super.Recv(buf, offset, count, datatype, source, tag);
    }

}
