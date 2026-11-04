package pk.qist.device.fakes

import pk.qist.device.deviceadmin.DevicePolicy
import pk.qist.device.deviceadmin.PolicyResult

/** Records each policy call in order; [result] is what every call returns. */
class FakeDevicePolicy : DevicePolicy {
    var result: PolicyResult = PolicyResult.Applied
    val calls = mutableListOf<String>()

    private fun record(name: String) = result.also { calls += name }

    override fun protect() = record("protect")

    override fun lock() = record("lock")

    override fun unlock() = record("unlock")

    override fun release() = record("release")

    override fun relinquish() = record("relinquish")
}
