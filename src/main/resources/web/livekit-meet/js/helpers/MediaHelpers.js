var MediaHelpers = (function() {
    function isScreenSharing() {
        return window.liveKitClient && window.liveKitClient.isScreenSharing() || false;
    }

    function isScreenSharePermissionDenied() {
        return window.TestStateStore
            ? window.TestStateStore.screenShare.isPermissionDenied()
            : (window.screenSharePermissionDenied || false);
    }

    function getLastScreenShareError() {
        return window.TestStateStore
            ? window.TestStateStore.screenShare.getLastError()
            : (window.lastScreenShareError || 'No error captured');
    }

    function enableSimulcast() {
        if (window.TestStateStore) {
            window.TestStateStore.simulcast.setEnabled(true);
        }
        window.simulcastEnabled = true;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(true);
    }

    function disableSimulcast() {
        if (window.TestStateStore) {
            window.TestStateStore.simulcast.setEnabled(false);
        }
        window.simulcastEnabled = false;
        if (window.liveKitClient) window.liveKitClient.setSimulcastEnabled(false);
    }

    function isSimulcastEnabled() {
        if (window.liveKitClient) return window.liveKitClient.isSimulcastEnabled();
        if (window.TestStateStore) return window.TestStateStore.simulcast.isEnabled();
        return window.simulcastEnabled !== false;
    }

    function setVideoQualityPreference(quality) {
        if (window.TestStateStore) {
            window.TestStateStore.simulcast.setCurrentQuality(quality);
        }
        window.currentVideoQuality = quality;
        if (window.liveKitClient) window.liveKitClient.setVideoQualityPreference(quality);
    }

    function getVideoQualityPreference() {
        if (window.liveKitClient) return window.liveKitClient.getVideoQualityPreference();
        if (window.TestStateStore) return window.TestStateStore.simulcast.getCurrentQuality();
        return window.currentVideoQuality || 'HIGH';
    }

    function setMaxReceiveBandwidth(kbps) {
        if (window.liveKitClient) window.liveKitClient.setMaxReceiveBandwidth(kbps);
    }

    function getRemoteVideoTrackWidthByPublisher(publisherIdentity) {
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
    }

    function getRemoteVideoTracks() {
        return window.liveKitClient ? window.liveKitClient.getRemoteVideoTracks() : [];
    }

    function getReceivingLayerInfo(publisherIdentity) {
        return window.liveKitClient ? window.liveKitClient.getReceivingLayerInfo(publisherIdentity) : null;
    }

    function getPlayingVideoElementCount() {
        try {
            return Array.from(document.querySelectorAll('video')).filter(function(v) {
                return v.videoWidth > 0 && v.videoHeight > 0 && !v.paused && !v.ended && v.readyState >= 2;
            }).length;
        } catch(e) {
            return 0;
        }
    }

    function getSubscribedVideoTrackCount() {
        try {
            if (!window.liveKitClient || !window.liveKitClient.room) return 0;
            return Array.from(window.liveKitClient.room.tracks.values()).filter(function(t) {
                return t.kind === 'video' && t.isSubscribed;
            }).length;
        } catch(e) {
            return 0;
        }
    }

    function isDynacastEnabled() {
        return window.liveKitClient && window.liveKitClient.isDynacastEnabled() || false;
    }

    return {
        isScreenSharing: isScreenSharing,
        isScreenSharePermissionDenied: isScreenSharePermissionDenied,
        getLastScreenShareError: getLastScreenShareError,
        enableSimulcast: enableSimulcast,
        disableSimulcast: disableSimulcast,
        isSimulcastEnabled: isSimulcastEnabled,
        setVideoQualityPreference: setVideoQualityPreference,
        getVideoQualityPreference: getVideoQualityPreference,
        setMaxReceiveBandwidth: setMaxReceiveBandwidth,
        getRemoteVideoTrackWidthByPublisher: getRemoteVideoTrackWidthByPublisher,
        getRemoteVideoTracks: getRemoteVideoTracks,
        getReceivingLayerInfo: getReceivingLayerInfo,
        getPlayingVideoElementCount: getPlayingVideoElementCount,
        getSubscribedVideoTrackCount: getSubscribedVideoTrackCount,
        isDynacastEnabled: isDynacastEnabled
    };
})();

window.MediaHelpers = MediaHelpers;
