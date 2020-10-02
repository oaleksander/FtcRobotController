package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

import org.firstinspires.ftc.teamcode.math.Pose2D;
import org.openftc.revextensions2.ExpansionHubEx;
import org.openftc.revextensions2.ExpansionHubMotor;

import static com.qualcomm.robotcore.util.Range.clip;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.signum;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static org.firstinspires.ftc.teamcode.math.MathUtil.angleWrap;

public class WoENrobot extends LinearOpMode {


    /* Global runtime variables. */
    public static ElapsedTime Runtime = new ElapsedTime();
    public static boolean robotIsInitialized = false;


    /* Drivetrain hardware members. */
    public static ExpansionHubEx expansionHub1 = null;
    public static DcMotorEx driveFrontLeft = null;
    public static DcMotorEx driveFrontRight = null;
    public static DcMotorEx driveRearLeft = null;
    public static DcMotorEx driveRearRight = null;

    public static ExpansionHubEx expansionHub2 = null;
    public static ExpansionHubMotor odometerYL = null;
    public static ExpansionHubMotor odometerYR = null;
    public static ExpansionHubMotor odometerX = null;

    public static BNO055IMU imuLeft;
    public static BNO055IMU imuRight;

    /* Robot-specific hardware members. */
    public static DcMotorEx ringIntake = null;
    public static DcMotorEx ringLauncher = null;


    //   public static int cameraMonitorViewId;

    //   private final int rows = 640;
    //   private final int cols = 480;

    /* Drivetrain constatnts. */

    public static final double odometryWheelDiameterCm = 4.8;
    public static final double odometryCountsPerCM = (1440) / (odometryWheelDiameterCm * PI);
    public static final double odometryCMPerCounts = (odometryWheelDiameterCm * PI) / 1440;
    public static final float odometerYcenterOffsetCm = 13;
    public static final float odometerXcenterOffsetCm = 2;
    public float imuLeftAngleOffset = 0;
    public float imuRightAngleOffset = 0;

    public static double maxDriveSpeed = 1;
    public static double minDriveSpeed = 0.1;

    public static Odometry odometry;

    /* Constructor */
    @Override
    public void runOpMode() {
    }


    private double robotEstimatedHeading;
    private double robotGlobalXCoordinatePosition = 0, robotGlobalYCoordinatePosition = 0;
    private Pose2D WorldPosition;
    private float integratedAngle = 0;

    private boolean stopOdometry = false;
    private boolean stopIntakeControl = false;
    private boolean stopManipulatorControl = false;
    private boolean stopSlideControl = false;
    private boolean stopLiftControl = false;

    public static final double kP_distance = 0.0011, kD_distance = 0.000022;
    public static final double minError_distance = 5 * odometryCountsPerCM;

    public static final double kP_angle = 0.0, kD_angle = 0;
    public static final double minError_angle = Math.toRadians(3);

    public void Pos(int y_target, int x_target, double heading_target) {
        y_target *= odometryCountsPerCM;
        x_target *= odometryCountsPerCM;
        heading_target = angleWrap(toRadians(heading_target));
        ElapsedTime looptime = new ElapsedTime();
        double errold_y = y_target - robotGlobalYCoordinatePosition;
        double errold_x = x_target - robotGlobalXCoordinatePosition;
        double err_y = errold_y;
        double err_x = errold_x;
        double distanceError = Math.hypot(err_y, err_x);
        double angleError = angleWrap(heading_target - robotEstimatedHeading);
        double differr_angle;
        double errold_angle = angleError;


        while (opModeIsActive() && (distanceError > minError_distance || abs(angleError) > minError_angle)) {

            err_y = y_target - robotGlobalYCoordinatePosition;
            err_x = x_target - robotGlobalXCoordinatePosition;
            angleError = angleWrap(heading_target - robotEstimatedHeading);


            differr_angle = (angleError - errold_angle) / looptime.seconds();
            double differr_y = (err_y - errold_y) / looptime.seconds();
            double differr_x = (err_x - errold_x) / looptime.seconds();
            looptime.reset();
            errold_angle = angleError;
            errold_y = err_y;
            errold_x = err_x;

            double relativeY = err_y * kP_distance + differr_y * kD_distance;
            double relativeX = err_x * kP_distance + differr_x * kD_distance;

            double cosa = cos(robotEstimatedHeading);
            double sina = sin(robotEstimatedHeading);

            double controlY = relativeX * sina + relativeY * cosa;
            double controlX = relativeX * cosa - relativeY * sina;
            double controlAngle = angleError * kP_angle + differr_angle * kP_angle;

            omniMoveYX(0, 0, 0);

            telemetry.addData("y", controlY);
            telemetry.addData("x", controlX);
            telemetry.addData("a", controlAngle);
            telemetry.addData("ae", toDegrees(angleError));

            telemetry.addData("dist", odometryCMPerCounts * distanceError);
            telemetry.update();

            distanceError = Math.hypot(err_y, err_x);
        }
    }

    public void startRobot() {
        waitForStart();
        Runtime.reset();
        telemetry.addData("Status", "Running");
        telemetry.update();
    }

    public void initRobot() {
        if (!robotIsInitialized) {
            forceInitRobot();
            telemetry.addData("Status", "Initialization successful");
            telemetry.update();
        } else {
            stopAllMotors();
            setMotor0PowerBehaviors();
            setMotorDirections();
            telemetry.addData("Status", "Already initialized, ready");
            telemetry.update();
        }
    }

    private static final boolean defaultNames = true;

    public void forceInitRobot() {
        telemetry.addData("Status", "Initializing...");
        telemetry.update();

        if (defaultNames) {

            imuLeft = hardwareMap.get(BNO055IMU.class, "imuLeft");
            imuRight = hardwareMap.get(BNO055IMU.class, "imuRight");

            driveFrontLeft = hardwareMap.get(DcMotorEx.class, "driveFrontLeft");
            driveFrontRight = hardwareMap.get(DcMotorEx.class, "driveFrontRight");
            driveRearLeft = hardwareMap.get(DcMotorEx.class, "driveRearLeft");
            driveRearRight = hardwareMap.get(DcMotorEx.class, "driveRearRight");


        } else {

            driveFrontLeft = hardwareMap.get(DcMotorEx.class, "driveFrontLeft");
            driveFrontRight = hardwareMap.get(DcMotorEx.class, "driveFrontRight");
            driveRearLeft = hardwareMap.get(DcMotorEx.class, "driveRearLeft");
            driveRearRight = hardwareMap.get(DcMotorEx.class, "driveRearRight");

        }

        setMotor0PowerBehaviors();
        setMotorDirections();

        stopAllMotors();

        odometry = new Odometry(expansionHub2, odometerYL, odometerYR, odometerX);

        resetOdometryEncoders();

        robotIsInitialized = true;
        telemetry.addData("Status", "Force initialized");
        telemetry.update();
    }

    public void setMotorDirections() {
        driveFrontLeft.setDirection(DcMotorEx.Direction.FORWARD);
        driveFrontRight.setDirection(DcMotorEx.Direction.REVERSE);
        driveRearLeft.setDirection(DcMotorEx.Direction.FORWARD);
        driveRearRight.setDirection(DcMotorEx.Direction.REVERSE);

    }

    public void setMotor0PowerBehaviors() {
        driveFrontLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
        driveFrontRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
        driveRearLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
        driveRearRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
    }

    public void initializeGyroscopes() {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        imuLeft.initialize(parameters);
        imuRight.initialize(parameters);

        imuLeftAngleOffset = -imuLeft.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle;
        imuRightAngleOffset = -imuRight.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle;
    }

    private void resetOdometryEncoders() {
        driveFrontLeft.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        driveFrontRight.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        driveRearLeft.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        driveRearRight.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);

        driveFrontLeft.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        driveFrontRight.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        driveRearLeft.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        driveRearRight.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
    }

    public void stopAllMotors() {
        stopMoveMotors();
    }

    void stopMoveMotors() {
        driveMotorPowers(0, 0, 0, 0);
    }

    private void driveMotorPowers(double frontLeft, double frontRight, double rearLeft, double rearRight) {
        driveFrontLeft.setPower(clip(abs(frontLeft), minDriveSpeed, maxDriveSpeed) * signum(frontLeft));
        driveFrontRight.setPower(clip(abs(frontRight), minDriveSpeed, maxDriveSpeed) * signum(frontRight));
        driveRearLeft.setPower(clip(abs(rearLeft), minDriveSpeed, maxDriveSpeed) * signum(rearLeft));
        driveRearRight.setPower(clip(abs(rearRight), minDriveSpeed, maxDriveSpeed) * signum(rearRight));
    }

    void tankTurn(double speed) {
        tankMove(speed, -speed);
    }

    void tankMove(double speed) {
        tankMove(speed, speed);
    }


    private void tankMove(double speedL, double speedR) {
        speedL = Range.clip(speedL, -1.0, 1.0);
        speedR = Range.clip(speedR, -1.0, 1.0);

        driveMotorPowers(speedL, speedR, speedL, speedR);
    }

    @Deprecated
    void omniMove(double heading, double speed, double turn) {
        heading = Math.toRadians(heading);
        turn = Range.clip(turn, -1.0, 1.0);
        speed = Range.clip(speed, -1.0, 1.0);
        double frontways = speed * cos(heading);
        double sideways = speed * sin(heading);

        omniMoveYX(frontways, sideways, turn);
    }

    @Deprecated
    void omniMove(double heading, double speed) {
        heading = Math.toRadians(heading);
        speed = Range.clip(speed, -1.0, 1.0);
        omniMoveYX(speed * cos(heading), speed * sin(heading));
    }

    @Deprecated
    void omniMoveYX(double frontways, double sideways) {

        double LFRR = frontways + sideways;
        double RFLR = frontways - sideways;

        if (abs(LFRR) > 1.0 || abs(RFLR) > 1.0) {
            LFRR /= max(abs(LFRR), abs(RFLR));
            RFLR /= max(abs(LFRR), abs(RFLR));
        }

        driveMotorPowers(LFRR, RFLR, RFLR, LFRR);
    }

    @Deprecated
    public void omniMoveYX(double frontways, double sideways, double turn) {
        double FrontLeft = frontways + sideways + turn;
        double FrontRight = frontways - sideways - turn;
        double RearLeft = frontways - sideways + turn;
        double RearRight = frontways + sideways - turn;

        if (abs(FrontLeft) > 1.0 || abs(FrontRight) > 1.0 || abs(RearLeft) > 1.0 || abs(RearRight) > 1.0) {
            double maxabs = max(max(abs(FrontLeft), abs(FrontRight)), max(abs(RearLeft), abs(RearRight)));
            FrontLeft /= maxabs;
            FrontRight /= maxabs;
            RearLeft /= maxabs;
            RearRight /= maxabs;
        }

        driveMotorPowers(FrontLeft, FrontRight, RearLeft, RearRight);
    }

    public void setMaxDriveSpeed(double value) {
        maxDriveSpeed = clip(abs(value), 0, 1);
    }

    public void setMinDriveSpeed(double value) {
        minDriveSpeed = clip(abs(value), 0, 1);
    }

}

