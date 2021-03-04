package org.firstinspires.ftc.teamcode.robot

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.*
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.teamcode.misc.CommandSender
import org.firstinspires.ftc.teamcode.robot.Shooter2.ShooterConfig.kD
import org.firstinspires.ftc.teamcode.robot.Shooter2.ShooterConfig.kI
import org.firstinspires.ftc.teamcode.robot.Shooter2.ShooterConfig.kP
import org.firstinspires.ftc.teamcode.superclasses.MultithreadRobotModule
import org.openftc.revextensions2.ExpansionHubServo
import kotlin.math.abs

class Shooter2: MultithreadRobotModule() {
    private val rpmTime = ElapsedTime()
    private val feederTime = ElapsedTime()
    private val encoderFailureDetectionTime = ElapsedTime()

    @Config
    internal object ShooterConfig {
        @JvmField
        var servoTime = 137.0
        @JvmField
        var servoReturnMultiplier = 2.6
        @JvmField
        var lowRpm = 3470.0
        @JvmField
        var highRpm = 4000.0
        @JvmField
        var timeRpm = 150.0
        @JvmField
        var feederClose = 0.165
        @JvmField
        var feederOpen = 0.42
        @JvmField
        var kP = 58.0
        @JvmField
        var kI = 0.001 //0.03
        @JvmField
        var kD = 0.05
      //  @JvmField
      //  var kF = 14.89
      //  @JvmField
      //  var kF_referenceVoltage = 12.485
    }

    private lateinit var shooterMotor: DcMotorEx
    private lateinit var voltageSensor: VoltageSensor
    private lateinit var feeder: ExpansionHubServo
    private val shooterPowerSender = CommandSender { p: Double -> shooterMotor.power = p }
    private val feederPositionSender = CommandSender { p: Double -> feeder.position = p }
    private var shooterMode = ShooterMode.OFF
    private var ringsToShoot: Int = 0
    private var timeToAccelerateMs = 1.0
    private var accelerationIncrement = 1.0
    var rpmTarget = 6000.0
        private set
    private var motorVelocityTarget = 0.0
    var currentRpm = 0.0
    var encoderFailureMode = false
        private set

    override fun initialize() {
        voltageSensor = WoENHardware.expansionHubVoltageSensor
        shooterMotor = WoENHardware.shooterMotor
        /*val motorConfigurationType = shooterMotor.motorType.clone()
        motorConfigurationType.achieveableMaxRPMFraction = 0.896476253
        motorConfigurationType.ticksPerRev = 24.0
        motorConfigurationType.gearing = 1.0
        motorConfigurationType.maxRPM = 6000.0
        shooterMotor.motorType = motorConfigurationType */
        shooterMotor.direction = DcMotorSimple.Direction.FORWARD
        shooterMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        shooterMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        shootingMode = ShooterMode.OFF
        initializedservo()
        feederTime.reset()
    }

    private fun initializedservo() {
        feeder = opMode.hardwareMap.get(Servo::class.java, "feeder") as ExpansionHubServo
        feeder.position = ShooterConfig.feederClose
    }

    override fun start() {
        feeder.position = ShooterConfig.feederClose
        shooterMotor.power = 0.0
        shootingMode = ShooterMode.OFF
        ringsToShoot = 0
    }

    override fun updateControlHub() {
        if (ringsToShoot > 0 && feederTime.milliseconds() > ShooterConfig.servoTime * ShooterConfig.servoReturnMultiplier) {
            feedRing()
            ringsToShoot--
        }
        setFeederPosition(feederTime.milliseconds() < ShooterConfig.servoTime && motorVelocityTarget != 0.0)
    }
    var rpmError = 0.0
    var rpmErrorOld = 0.0
    var P = 0.0
    var D = 0.0
    var I = 0.0
    var power = 0.0
    override fun updateExpansionHub() {
        currentRpm = getMotorRpm()
        if (rpmTarget != 0.0) {
            rpmError = rpmTarget - currentRpm
            P = rpmError * kP
            D = (rpmError - rpmErrorOld) * kD
            I += kI * rpmError
            power = (P + I + D) / 6000
            rpmErrorOld = rpmError
        }
        else {
            rpmError = 0.0
            power = 0.0
            rpmErrorOld = 0.0
            I = 0.0
        }
        shooterPowerSender.send(power)
    }

    override fun updateOther() {
    }

    private fun setFeederPosition(push: Boolean) {
        feederPositionSender.send(if (push) ShooterConfig.feederOpen else ShooterConfig.feederClose)
    }

    private fun setShootersetings(Rpm: Double, time: Double) {
        if (Rpm != rpmTarget || time != timeToAccelerateMs) {
            rpmTarget = Rpm
            if (time != 0.0) timeToAccelerateMs = abs(time)
            accelerationIncrement = rpmTarget / timeToAccelerateMs / 6000
            motorVelocityTarget = rpmTarget * 0.4
        }
    }

    private fun getMotorRpm(): Double = shooterMotor.velocity * 2.5

    var shootingMode: ShooterMode
        get() = shooterMode
        set(mode) {
            if (mode != ShooterMode.OFF && shooterMode == ShooterMode.OFF) rpmTime.reset()
            shooterMode = mode
            when (mode) {
                ShooterMode.HIGHGOAL -> setShootersetings(
                    ShooterConfig.highRpm,
                    ShooterConfig.timeRpm
                )
                ShooterMode.POWERSHOT -> setShootersetings(
                    ShooterConfig.lowRpm,
                    ShooterConfig.timeRpm
                )
                ShooterMode.OFF -> setShootersetings(0.0, ShooterConfig.timeRpm)
            }
        }

    fun isCorrectRpm(error: Double = 30.0): Boolean {
        return if (encoderFailureMode) true else abs(rpmTarget - currentRpm) < error // abs(currentVelocity - rpmNow / 2.5) < error
    }

    fun feedRing() {
        //  ringsToShoot = 1
        feederTime.reset()
    }

    fun feedRings() {
        ringsToShoot = 3
    }

    enum class ShooterMode {
        HIGHGOAL, POWERSHOT, OFF
    }
}