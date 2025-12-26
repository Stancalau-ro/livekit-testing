var MuteHelpers = (function() {
    function muteAudio() {
        if (window.liveKitClient) window.liveKitClient.muteAudio();
    }

    function unmuteAudio() {
        if (window.liveKitClient) window.liveKitClient.unmuteAudio();
    }

    function muteVideo() {
        if (window.liveKitClient) window.liveKitClient.muteVideo();
    }

    function unmuteVideo() {
        if (window.liveKitClient) window.liveKitClient.unmuteVideo();
    }

    function isAudioMuted() {
        return window.liveKitClient ? window.liveKitClient.isAudioMuted() : true;
    }

    function isVideoMuted() {
        return window.liveKitClient ? window.liveKitClient.isVideoMuted() : true;
    }

    function getLocalAudioTrackState() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    }

    function getLocalVideoTrackState() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    }

    function getRemoteParticipantTrackMuteState(participantIdentity, trackType) {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === participantIdentity) participant = p;
        });
        if (!participant) return null;
        var result = null;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === trackType) {
                result = {
                    muted: pub.isMuted,
                    enabled: pub.track ? pub.track.isEnabled : false,
                    sid: pub.trackSid
                };
            }
        });
        return result;
    }

    function getMuteEventCount() {
        if (window.TestStateStore) return window.TestStateStore.mute.getEventCount();
        return window.muteEvents ? window.muteEvents.length : 0;
    }

    function getLastMuteEvent() {
        if (window.TestStateStore) return window.TestStateStore.mute.getLastEvent();
        return window.muteEvents && window.muteEvents.length > 0
            ? window.muteEvents[window.muteEvents.length - 1]
            : null;
    }

    function clearMuteEvents() {
        if (window.TestStateStore) {
            window.TestStateStore.mute.clear();
        }
        window.muteEvents = [];
    }

    return {
        muteAudio: muteAudio,
        unmuteAudio: unmuteAudio,
        muteVideo: muteVideo,
        unmuteVideo: unmuteVideo,
        isAudioMuted: isAudioMuted,
        isVideoMuted: isVideoMuted,
        getLocalAudioTrackState: getLocalAudioTrackState,
        getLocalVideoTrackState: getLocalVideoTrackState,
        getRemoteParticipantTrackMuteState: getRemoteParticipantTrackMuteState,
        getMuteEventCount: getMuteEventCount,
        getLastMuteEvent: getLastMuteEvent,
        clearMuteEvents: clearMuteEvents
    };
})();

window.MuteHelpers = MuteHelpers;
