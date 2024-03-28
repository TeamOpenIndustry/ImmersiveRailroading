package com.goldenfield192.ire.util;

import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.custom.ITickable;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import com.goldenfield192.ire.IRE;

import java.util.ArrayList;
import java.util.List;

//Based on I/O ratio
public class IREEnergy implements IEnergy{
    @TagField("input")
    private int currentInput;
    @TagField("output")
    private int currentOutput;
    private transient int exacted;
    private transient int stored;

    public Status getStatus() {
        return status;
    }

    private transient Status status;

    public IREEnergy() {
            stored = 0;
            exacted = 0;
    }

    @Override
    public int receive(int maxReceive, boolean simulate) {
        if (!simulate && maxReceive != 0) {
            this.stored += maxReceive;
        }
        return maxReceive;
    }

    @Override
    public int extract(int maxExtract, boolean simulate) {
        if (!simulate && maxExtract != 0) {
            this.exacted += maxExtract;
        }
        return maxExtract;
    }

    //Called every tick to update status
    public void update(long tick){
        this.currentInput = stored;
        this.currentOutput = exacted;
        stored = 0;
        exacted = 0;
        if(currentInput == 0){
            status = Status.EMPTY;
        }else if(currentInput <= currentOutput){
            status = Status.OVERLOAD;
        } else {
            status = Status.VALID;
        }
    }

    public int getCurrentInput() {
        return currentInput;
    }

    @Override
    public int getCurrent() {
        return 0;
    }

    @Override
    public int getMax() {
        return 0;
    }

    public int getCurrentOutput() {
        return currentOutput;
    }

    public enum Status{
        EMPTY,
        VALID,
        OVERLOAD;
    }
}
