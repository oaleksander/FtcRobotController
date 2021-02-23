package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.misc.ButtonSwitch
import org.firstinspires.ftc.teamcode.misc.SinglePressButton
import org.firstinspires.ftc.teamcode.robot.WoENrobot.conveyor
import org.firstinspires.ftc.teamcode.robot.WoENrobot.initRobot
import org.firstinspires.ftc.teamcode.robot.WoENrobot.movement
import org.firstinspires.ftc.teamcode.robot.WoENrobot.shooter
import org.firstinspires.ftc.teamcode.robot.WoENrobot.startRobot
import org.firstinspires.ftc.teamcode.robot.WoENrobot.wobbleManipulator
import org.firstinspires.ftc.teamcode.robot.rpm

@TeleOp()
class TeleOp_Single : LinearOpMode() {
    override fun runOpMode() {
        initRobot(this)
        startRobot()
        /* Wobble */
        val grabWobbleSwitch = ButtonSwitch { gamepad1.a }
        /* Conveyor */
        val conveyorOnOffSwitch = ButtonSwitch { gamepad1.back }
        /* Shooter */
        val shooterOnOffSwitch = ButtonSwitch { gamepad1.start }
        val threeRingPresser = SinglePressButton { gamepad1.right_stick_button }
        while (opModeIsActive()) {
            /* Wobble */
            wobbleManipulator.grabWobble(grabWobbleSwitch.get())
            wobbleManipulator.upmediumdown(gamepad1.y, gamepad1.x) // correct
            /* Conveyor */
            conveyor.enableConveyor(conveyorOnOffSwitch.get())
            /* Shooter */
            shooter.shootingMode =
                if (shooterOnOffSwitch.get()) rpm.ShooterMode.HIGHGOAL else rpm.ShooterMode.OFF
            if (gamepad1.b) shooter.feedRing()
            if (threeRingPresser.get()) shooter.feedRings()
            /* Drivetrain */
            movement.humanSetVelocity(
                gamepad1.left_stick_x.toDouble() + if (gamepad1.dpad_left) -1.0 else 0.0 + if (gamepad1.dpad_right) 1.0 else 0.0,
                -gamepad1.left_stick_y.toDouble() + if (gamepad1.dpad_up) 1.0 else 0.0 + if (gamepad1.dpad_down) -1.0 else 0.0,
                if (gamepad1.right_bumper) 0.25 else gamepad1.right_trigger.toDouble() - if (gamepad1.left_bumper) 0.25 else gamepad1.left_trigger.toDouble()
            )
        }
    }
}