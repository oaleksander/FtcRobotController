@file:Suppress("ClassName")

package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.math.Pose2D
import org.firstinspires.ftc.teamcode.opmodes.MovementMacros.shootPOWERSHOTAngle
import org.firstinspires.ftc.teamcode.opmodes.MovementMacros.shootPowerShotDynamic
import org.firstinspires.ftc.teamcode.robot.WoENrobot.movement

@Autonomous(name = "Pos test")
class Auto_Sandbox2 : AutoOpMode() {
    override fun main() {
        while (opModeIsActive()) {
            movement.pos(Pose2D(Double.NaN, 100.0, Math.toRadians(0.0)))
            movement.pos(Pose2D(Double.NaN, -100.0, Math.toRadians(0.0)))
           // shootPOWERSHOTAngle()
        }
        //shootPowerShotDynamic()
    }
}