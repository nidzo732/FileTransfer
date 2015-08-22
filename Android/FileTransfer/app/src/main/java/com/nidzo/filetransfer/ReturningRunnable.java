package com.nidzo.filetransfer;

public abstract class ReturningRunnable<ReturnedType> implements Runnable{
    private ReturnedType result;
    private boolean done;
    public ReturningRunnable()
    {
        done=false;
    }
    protected synchronized void setResult(ReturnedType result)
    {
        this.result=result;
        done=true;
        notify();
    }
    public synchronized ReturnedType getResult()
    {
        try {
            while (!done) {
                wait();
            }
        } catch (InterruptedException e) {
            return null;
        }
        return this.result;
    }
}
