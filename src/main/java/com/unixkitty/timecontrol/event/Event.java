package com.unixkitty.timecontrol.event;

public abstract class Event
{
    private boolean canceled = false;

    public boolean execute()
    {
        return isCancelable() || !isCanceled();
    }

    //inverted
    public boolean isCancelable()
    {
        return false;
    }

    public boolean isCanceled()
    {
        return this.canceled;
    }

    public void setCanceled(boolean cancel)
    {
        if (isCancelable())
        {
            throw new UnsupportedOperationException("Attempted to call Event#setCanceled() on a non-cancelable event of type: " + this.getClass().getCanonicalName());
        }

        this.canceled = cancel;
    }
}
