package com.kidsee.core.network.webrtc

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class SdpSessionDescription(
    val type: String = "", // "OFFER" or "ANSWER"
    val description: String = ""
)

@Serializable
data class IceCandidateModel(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = ""
)

class WebRtcSignaling(
    private val database: FirebaseDatabase
) {
    constructor() : this(FirebaseDatabase.getInstance())

    suspend fun sendOffer(targetDeviceId: String, sdp: SdpSessionDescription) {
        database.getReference("webrtc/$targetDeviceId/offer")
            .setValue(sdp)
            .await()
    }

    suspend fun sendAnswer(targetDeviceId: String, sdp: SdpSessionDescription) {
        database.getReference("webrtc/$targetDeviceId/answer")
            .setValue(sdp)
            .await()
    }

    suspend fun sendIceCandidate(targetDeviceId: String, candidate: IceCandidateModel, isParent: Boolean) {
        val path = if (isParent) "webrtc/$targetDeviceId/parent_candidates" else "webrtc/$targetDeviceId/child_candidates"
        database.getReference(path).push().setValue(candidate).await()
    }

    fun observeOffer(deviceId: String): Flow<SdpSessionDescription?> = callbackFlow {
        val ref = database.getReference("webrtc/$deviceId/offer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(SdpSessionDescription::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeOffer cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAnswer(deviceId: String): Flow<SdpSessionDescription?> = callbackFlow {
        val ref = database.getReference("webrtc/$deviceId/answer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(SdpSessionDescription::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeAnswer cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeRemoteCandidates(deviceId: String, listenForParent: Boolean): Flow<IceCandidateModel> = callbackFlow {
        val path = if (listenForParent) "webrtc/$deviceId/parent_candidates" else "webrtc/$deviceId/child_candidates"
        val ref = database.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    child.getValue(IceCandidateModel::class.java)?.let { candidate ->
                        trySend(candidate)
                        child.ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeRemoteCandidates cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun clearSession(deviceId: String) {
        try {
            database.getReference("webrtc/$deviceId").removeValue().await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear WebRTC session for $deviceId")
        }
    }
}
