var LiveKitTestHelpers = {
    isLiveKitLoaded: function() {
        return typeof LiveKit !== 'undefined';
    },

    getLastError: function() {
        return window.lastError || '';
    },

    isUsingMock: function() {
        return !!window.USING_MOCK_LIVEKIT;
    },

    isRealWebRTCConnectionVerified: function() {
        return window.REAL_WEBRTC_CONNECTION_VERIFIED || false;
    },

    isConnectionEstablished: function() {
        return window.connectionEstablished || false;
    },

    isClientConnected: function() {
        return window.liveKitClient && window.liveKitClient.isConnected();
    },

    getConnectionTime: function() {
        return window.connectionTime || 0;
    },

    getConsoleLogs: function() {
        return window.consoleLogCapture ? window.consoleLogCapture.join('\n') : 'No console logs captured';
    },

    isInMeetingRoom: function() {
        return window.liveKitClient && window.liveKitClient.isInMeetingRoom();
    },

    isScreenSharing: function() {
        return window.liveKitClient && window.liveKitClient.isScreenSharing() || false;
    },

    isScreenSharePermissionDenied: function() {
        return window.screenSharePermissionDenied || false;
    },

    getLastScreenShareError: function() {
        return window.lastScreenShareError || 'No error captured';
    },

    enableSimulcast: function() {
        window.simulcastEnabled = true;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(true);
    },

    disableSimulcast: function() {
        window.simulcastEnabled = false;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(false);
    },

    isSimulcastEnabled: function() {
        return window.liveKitClient ? window.liveKitClient.isSimulcastEnabled() : (window.simulcastEnabled !== false);
    },

    setVideoQualityPreference: function(quality) {
        window.currentVideoQuality = quality;
        if (window.liveKitClient) window.liveKitClient.setVideoQualityPreference(quality);
    },

    getVideoQualityPreference: function() {
        return window.liveKitClient ? window.liveKitClient.getVideoQualityPreference() : (window.currentVideoQuality || 'HIGH');
    },

    setMaxReceiveBandwidth: function(kbps) {
        if (window.liveKitClient) window.liveKitClient.setMaxReceiveBandwidth(kbps);
    },

    getRemoteVideoTrackWidthByPublisher: function(publisherIdentity) {
        if (!window.liveKitClient) return 0;
        if (!window.liveKitClient.room) return 0;
        var participantDiv = document.getElementById('participant-' + publisherIdentity);
        if (participantDiv) {
            var video = participantDiv.querySelector('video');
            if (video && video.videoWidth > 0) {
                return video.videoWidth;
            }
        }
        var remoteParticipants = window.liveKitClient.room.remoteParticipants;
        var width = 0;
        remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) {
                p.trackPublications.forEach(function(pub) {
                    if (pub.kind === 'video' && pub.track) {
                        if (pub.track.dimensions && pub.track.dimensions.width > 0) {
                            width = pub.track.dimensions.width;
                        } else if (pub.track.mediaStreamTrack) {
                            var settings = pub.track.mediaStreamTrack.getSettings();
                            if (settings.width) width = settings.width;
                        }
                    }
                });
            }
        });
        return width;
    },

    getRemoteVideoTracks: function() {
        return window.liveKitClient ? window.liveKitClient.getRemoteVideoTracks() : [];
    },

    getReceivingLayerInfo: function(publisherIdentity) {
        return window.liveKitClient ? window.liveKitClient.getReceivingLayerInfo(publisherIdentity) : null;
    },

    getSubscriptionFailedEventCount: function() {
        return window.subscriptionFailedEvents ? window.subscriptionFailedEvents.length : 0;
    },

    isSubscriptionPermissionDenied: function() {
        return window.subscriptionPermissionDenied || false;
    },

    getLastSubscriptionError: function() {
        return window.lastSubscriptionError || '';
    },

    getPlayingVideoElementCount: function() {
        try {
            return Array.from(document.querySelectorAll('video')).filter(function(v) {
                return v.videoWidth > 0 && v.videoHeight > 0 && !v.paused && !v.ended && v.readyState >= 2;
            }).length;
        } catch(e) {
            return 0;
        }
    },

    getSubscribedVideoTrackCount: function() {
        try {
            if (!window.liveKitClient || !window.liveKitClient.room) return 0;
            return Array.from(window.liveKitClient.room.tracks.values()).filter(function(t) {
                return t.kind === 'video' && t.isSubscribed;
            }).length;
        } catch(e) {
            return 0;
        }
    },

    muteAudio: function() {
        if (window.liveKitClient) window.liveKitClient.muteAudio();
    },

    unmuteAudio: function() {
        if (window.liveKitClient) window.liveKitClient.unmuteAudio();
    },

    muteVideo: function() {
        if (window.liveKitClient) window.liveKitClient.muteVideo();
    },

    unmuteVideo: function() {
        if (window.liveKitClient) window.liveKitClient.unmuteVideo();
    },

    isAudioMuted: function() {
        return window.liveKitClient ? window.liveKitClient.isAudioMuted() : true;
    },

    isVideoMuted: function() {
        return window.liveKitClient ? window.liveKitClient.isVideoMuted() : true;
    },

    getLocalAudioTrackState: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    },

    getLocalVideoTrackState: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var pub = window.liveKitClient.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        if (!pub || !pub.track) return null;
        return {
            muted: pub.isMuted,
            enabled: pub.track.isEnabled,
            sid: pub.trackSid
        };
    },

    getRemoteParticipantTrackMuteState: function(participantIdentity, trackType) {
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
    },

    getMuteEventCount: function() {
        return window.muteEvents ? window.muteEvents.length : 0;
    },

    getLastMuteEvent: function() {
        return window.muteEvents && window.muteEvents.length > 0
            ? window.muteEvents[window.muteEvents.length - 1]
            : null;
    },

    clearMuteEvents: function() {
        window.muteEvents = [];
    }
};

window.LiveKitTestHelpers = LiveKitTestHelpers;
