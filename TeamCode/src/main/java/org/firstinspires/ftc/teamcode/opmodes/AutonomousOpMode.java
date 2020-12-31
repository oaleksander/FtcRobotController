package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.MovementMacros;
import org.firstinspires.ftc.teamcode.math.Pose2D;
import org.firstinspires.ftc.teamcode.misc.AutoTransitioner;

import static org.firstinspires.ftc.teamcode.robot.WoENrobot.*;

public class AutonomousOpMode extends LinearOpMode {

    MovementMacros M;
    byte xSign = 1;
    byte sideSign = 1;
    boolean thereAreTwoGamepads;

    public void main() {
    }

    protected byte getXSign() {
        return xSign;
    }

    protected byte getSideSign() {
        return sideSign;
    }

    public Pose2D getStartPosition() {
        return new Pose2D(93.75 * getXSign() + 31.25 * getSideSign(), -156.5, 0);

    }

    @Override
    public void waitForStart() {
        while (!isStarted()) {
            start_loop();
        }
        super.waitForStart();
    }

    void start_loop() {

        telemetry.addLine("Use gamedad 1 X/B to select alliance color, dpad L/R to select alliance side");
        xSign = gamepad1.b ? 1 : gamepad1.x ? -1 : xSign;
        sideSign = gamepad1.dpad_right ? 1 : gamepad1.dpad_left ? -1 : sideSign;
        telemetry.addData("Alliance", getXSign() == 1 ? "RED" : "BLUE");
        telemetry.addData("SIDE", getSideSign() == 1 ? "RIGHT" : "LEFT");
        thereAreTwoGamepads = gamepad2.start||gamepad2.b||thereAreTwoGamepads;
        if(thereAreTwoGamepads) telemetry.addLine("Second gamepad detected");
        telemetry.update();
    }
    
    @Override
    public void runOpMode() throws InterruptedException {
        FullInitWithCV(this);
        startRobot();
        openCVNode.stopCam();
        new MovementMacros(getXSign(), getSideSign());
        odometry.setRobotCoordinates(getStartPosition());
        if(thereAreTwoGamepads)
        AutoTransitioner.transitionOnStop(this, "TeleOp COMPETITION");
        else
        AutoTransitioner.transitionOnStop(this, "TeleOp COMPETITION single");
        main();
        setLedColors(0, 128, 128);
        telemetry.addData("Status", "Program finished (" + getRuntime() + ")");
        telemetry.update();
    }

}
