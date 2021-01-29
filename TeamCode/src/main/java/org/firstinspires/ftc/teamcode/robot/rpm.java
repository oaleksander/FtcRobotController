package org.firstinspires.ftc.teamcode.robot;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.misc.CommandSender;
import org.firstinspires.ftc.teamcode.superclasses.RobotModule;

public class rpm implements RobotModule {
    private final ElapsedTime rpmTime = new ElapsedTime();
    private final ElapsedTime feederTime = new ElapsedTime();
    private final ElapsedTime encoderFailureDetectionTime = new ElapsedTime();
    private final double time = 137;
    private final double lowRpm = 3470;
    private final double highRpm = 4600;
    private final double timeRpm = 137;
    private final double feederClose = 0.0735;
    private final double feederOpen = 0.35;

    @Config
    static class shooterPIDF {
        public static double kP = 36;
        public static double kI = 0.03;
        public static double kD = 7;
        public static double kF = 14.8;
        public static double kF_referenceVoltage = 13;
    }

    private LinearOpMode opMode;
    public DcMotorEx shooterMotor = null;
    private final CommandSender shooterVelocitySender = new CommandSender(p -> shooterMotor.setVelocity(p));
    private Servo feeder = null;
    private final CommandSender feederPositionSender = new CommandSender(p -> feeder.setPosition(p));
    private ShooterMode shooterMode = ShooterMode.OFF;
    private byte ringsToShoot = 0;
    private double timeToAccelerate_ms = 1;
    private double accelerationIncrement = 1;
    private double rpmTarget = 6000;
    private double currentVelocity = 0;
    private double velocityTarget = 2400;
    private boolean encoderFailureMode = false;

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
        try {
            shooterMotor.setVelocityPIDFCoefficients(shooterPIDF.kP, shooterPIDF.kI, shooterPIDF.kD, shooterPIDF.kF * shooterPIDF.kF_referenceVoltage / opMode.hardwareMap.voltageSensor.iterator().next().getVoltage());
        } catch (UnsupportedOperationException e) {
            opMode.telemetry.addData("Shooter PIDF error ", e.getMessage());
        }
        shooterMotor.setDirection(DcMotorEx.Direction.FORWARD);
        shooterMotor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        shooterMotor.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        setShootingMode(ShooterMode.HIGHGOAL);
        initializedservo();
        feederTime.reset();
    }

    private void initializedservo() {
        feeder = opMode.hardwareMap.get(Servo.class, "feeder");
        feeder.setPosition(feederClose);
    }

    public void reset() {
        feeder.setPosition(feederClose);
        shooterMotor.setVelocity(0);
        setShootingMode(ShooterMode.OFF);
        ringsToShoot = 0;
    }

    public void update() {
        if (ringsToShoot > 0 && feederTime.milliseconds() > time * 2.60) {
            feedRing();
            ringsToShoot--;
        }
        setFeederPosition(feederTime.milliseconds() < time && (velocityTarget != 0));
        currentVelocity = rpmTime.milliseconds() >= timeToAccelerate_ms ?
                velocityTarget
                : rpmTime.milliseconds() * accelerationIncrement * velocityTarget;
        shooterVelocitySender.send(currentVelocity);

        if (encoderFailureDetectionTime.seconds() > 1)
            if (velocityTarget == 0 || getCurrentRpm() != 0)
                encoderFailureDetectionTime.reset();
        if (velocityTarget != 0 && ringsToShoot == 0)
            updatePIDFCoeffs(encoderFailureDetectionTime.seconds() > 2);
    }

    public boolean getEncoderFailureMode() {
        return encoderFailureMode;
    }

    private final ElapsedTime PIDFUpdateTimer = new ElapsedTime();

    private void updatePIDFCoeffs(boolean encoderFailureMode) {
        if (encoderFailureMode != this.encoderFailureMode || PIDFUpdateTimer.seconds() > 0.5) {
            this.encoderFailureMode = encoderFailureMode;
            PIDFUpdateTimer.reset();
            try {
                if (this.encoderFailureMode)
                    shooterMotor.setVelocityPIDFCoefficients(0, 0, 0, shooterPIDF.kF * shooterPIDF.kF_referenceVoltage / opMode.hardwareMap.voltageSensor.iterator().next().getVoltage());
                else
                    shooterMotor.setVelocityPIDFCoefficients(shooterPIDF.kP, shooterPIDF.kI, shooterPIDF.kD, shooterPIDF.kF * shooterPIDF.kF_referenceVoltage / opMode.hardwareMap.voltageSensor.iterator().next().getVoltage());
            } catch (UnsupportedOperationException ignored) {
            }
        }
    }

    private void setFeederPosition(boolean push) {
        feederPositionSender.send(push ? feederOpen : feederClose);
    }

    private void setShootersetings(double Rpm, double time) {
        if (Rpm != rpmTarget || time != timeToAccelerate_ms) {
            rpmTarget = Rpm;
            if (time != 0) timeToAccelerate_ms = Math.abs(time);
            accelerationIncrement = rpmTarget / timeToAccelerate_ms / 6000;
            velocityTarget = rpmTarget * 0.4;
        }
    }

 /*  public void onshooter(boolean On) {
        if (On)
            rpmTime.reset();
        shooterIsOn = On;
    }*/

    public boolean isCorrectRpm() {
        return isCorrectRpm(25);
    }

    public double getRpmTarget() {
        return rpmTarget;
    }

    public double getCurrentRpm() {
        return shooterMotor.getVelocity() * 2.5;
    }

    public ShooterMode getShootingMode() {
        return shooterMode;
    }

    public void setShootingMode(ShooterMode mode) {
        if (mode != ShooterMode.OFF && shooterMode == ShooterMode.OFF)
            rpmTime.reset();
        shooterMode = mode;
        switch (mode) {
            case HIGHGOAL:
                setShootersetings(highRpm, timeRpm);
                break;
            case POWERSHOT:
                setShootersetings(lowRpm, timeRpm);
                break;
            case OFF:
                setShootersetings(0, timeRpm);
        }
    }

    public boolean isCorrectRpm(double error) {
        if (encoderFailureMode) return true;
        return Math.abs(currentVelocity - shooterMotor.getVelocity()) < error;
    }

    public void feedRing() {
        //  ringsToShoot = 1;
        feederTime.reset();
    }

    public void feedRings() {
        ringsToShoot = 3;
    }

    //private boolean shooterIsOn = false;
    public enum ShooterMode {
        HIGHGOAL,
        POWERSHOT,
        OFF
    }
}
