package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.misc.CommandSender;
import org.firstinspires.ftc.teamcode.superclasses.RobotModule;

public class rpm implements RobotModule {
    private final ElapsedTime rpmTime = new ElapsedTime();
    private final double lowRpm = 3500;
    private final double highRpm = 3800;
    private final double timeRpm = 500;
    private LinearOpMode opMode;
    private DcMotorEx shooterMotor = null;
    private final CommandSender shooterVelocitySender = new CommandSender(p -> shooterMotor.setVelocity(p));
    private boolean on = false;
    private boolean powerShotB = false;

    private double time_ms = 1;
    private double x = 1;
    private double rpm = 6000;
    private double speed = 0;
    private double velocityTarget = 2400;

    public void setOpMode(LinearOpMode OpMode) {
        opMode = OpMode;
    }

    public void initialize() {

        shooterMotor = opMode.hardwareMap.get(DcMotorEx.class, "shooterMotor");
        MotorConfigurationType motorConfigurationType = shooterMotor.getMotorType().clone();
        motorConfigurationType.setAchieveableMaxRPMFraction(0.896476253);
        motorConfigurationType.setTicksPerRev(24);
        motorConfigurationType.setGearing(1);
        motorConfigurationType.setMaxRPM(6000);
        shooterMotor.setMotorType(motorConfigurationType);
        // PIDFCoefficients pidNew = new PIDFCoefficients(1, 1, 9, 0);
//       PIDFCoefficients pidNew = new PIDFCoefficients(3.628, 1.3, 5, 14.28);
        PIDFCoefficients pidNew = new PIDFCoefficients(25.5, 0.075, 16, 15.23);
        try {
            shooterMotor.setPIDFCoefficients(DcMotorEx.RunMode.RUN_USING_ENCODER, pidNew);
        } catch (UnsupportedOperationException e) {
            opMode.telemetry.addData("Drivetrain PIDF error ", e.getMessage());
        }
        shooterMotor.setDirection(DcMotorEx.Direction.FORWARD);
        shooterMotor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        shooterMotor.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        setShootersetings(highRpm, timeRpm);
    }

    public void reset() {
        shooterMotor.setVelocity(0);
        on = false;
    }

    public void update() {
        if (on) {
            if (time_ms > rpmTime.milliseconds()) {
                speed = rpmTime.milliseconds() * x * velocityTarget;
            }
            if (time_ms < rpmTime.milliseconds()) {
                speed = velocityTarget;
            }
        } else {
            speed = 0;
        }
        shooterVelocity(speed);
    }


    private void shooterVelocity(double velocity) {
        shooterVelocitySender.send(velocity);
    }

    public void onshooter(boolean On) {
        if (On != on) {
            rpmTime.reset();
            on = On;
        }
    }

    private void setShootersetings(double Rpm, double time) {
        if (Rpm != rpm || time != time_ms ) {
            rpm = Rpm;
            time_ms = time;
            x = rpm / Math.abs(time_ms) / 6000;
            velocityTarget = rpm * 0.4;
        }
    }

    public boolean isCorrectRpm() {
        return isCorrectRpm(25);
    }

    public double getRpmTarget() {
        return rpm;
    }

    public double getCurrentRpm() {
        return shooterMotor.getVelocity() * 2.5;
    }

    public void powerShot(boolean PowerShotB) {
        if (PowerShotB != powerShotB){
            powerShotB = PowerShotB;
            if (powerShotB) setShootersetings(lowRpm,timeRpm);
            else setShootersetings(highRpm, timeRpm);
        }
    }

    public boolean isCorrectRpm(double error) {
        return Math.abs(speed - shooterMotor.getVelocity()) < error;
    }
}
