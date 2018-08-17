package t.viewer.server.network

import t.viewer.server.matrix.MatrixController
import t.Context
import t.common.shared.ValueType
import t.common.shared.sample.Group
import t.viewer.server.matrix.ManagedMatrix
import t.platform.mirna.TargetTable
import t.viewer.server.Platforms

/**
 * A MatrixController that turns the main matrix into a ManagedNetwork
 * instead of a ManagedMatrix.
 */
class NetworkController(context: Context,
    groups: Seq[Group], initProbes: Seq[String],
    typ: ValueType,
    fullLoad: Boolean,
    sideMatrix: ManagedMatrix, targets: TargetTable,
    platforms: Platforms,
    initMainPageSize: Int) extends MatrixController[ManagedNetwork](context, groups, initProbes,
        MatrixController.groupPlatforms(context, groups), typ, fullLoad) {

  override def finish(mm: ManagedMatrix) = {
    new ManagedNetwork(mm.params, sideMatrix, targets, platforms, initMainPageSize)
  }
}
