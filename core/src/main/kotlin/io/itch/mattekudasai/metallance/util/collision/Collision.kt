package io.itch.mattekudasai.metallance.util.collision

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs


private val intersectionPoint = Vector2()
fun collides(positionA: Vector2, previousPositionA: Vector2, positionB: Vector2, previousPositionB: Vector2, safeDistance: Float): Boolean {
/*    val startingDistance = positionB.dst(positionA)
    val finalDistance = positionB.dst(positionA)
    val distanceA = positionA.dst(previousPositionA)
    val distanceB = positionB.dst(previousPositionB)
    if (distanceA < startingDistance && distanceA < finalDistance && distanceB < startingDistance && distanceB < finalDistance) {
        return false
    }*/
    if (Intersector.intersectSegments(previousPositionA, positionA, previousPositionB, positionB, null)) {
        return true
    }
    if (Intersector.distanceSegmentPoint(previousPositionA, positionA, positionB) < safeDistance) {
        return true
    }
    if (Intersector.distanceSegmentPoint(previousPositionA, positionA, previousPositionB) < safeDistance) {
        return true
    }
    if (Intersector.distanceSegmentPoint(previousPositionB, positionB, positionA) < safeDistance) {
        return true
    }
    if (Intersector.distanceSegmentPoint(previousPositionB, positionB, previousPositionA) < safeDistance) {
        return true
    }

    return false
}
