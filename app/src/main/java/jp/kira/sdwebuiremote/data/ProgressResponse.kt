package jp.kira.sdwebuiremote.data

import com.google.gson.annotations.SerializedName

data class ProgressResponse(
    val progress: Float,
    @SerializedName("eta_relative")
    val etaRelative: Float,
    val state: State
) {
    data class State(
        @SerializedName("sampling_step")
        val samplingStep: Int,
        @SerializedName("sampling_steps")
        val samplingSteps: Int
    )
}