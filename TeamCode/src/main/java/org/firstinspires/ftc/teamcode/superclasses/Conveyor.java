package org.firstinspires.ftc.teamcode.superclasses;

public interface Conveyor{

    void setConveyorPower(double power);

    void setForceReverse(boolean forceReverse);

    void setReverseAfterStop(boolean doReverseOnStop);

    void setAutomaticConveyorStopping(boolean doAutomaticConveyorStopping);
}