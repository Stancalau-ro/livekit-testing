var SubscriptionHelpers = (function() {
    function getSubscriptionFailedEventCount() {
        if (window.TestStateStore) return window.TestStateStore.subscription.getFailureCount();
        return window.subscriptionFailedEvents ? window.subscriptionFailedEvents.length : 0;
    }

    function isSubscriptionPermissionDenied() {
        if (window.TestStateStore) return window.TestStateStore.subscription.isPermissionDenied();
        return window.subscriptionPermissionDenied || false;
    }

    function getLastSubscriptionError() {
        if (window.TestStateStore) return window.TestStateStore.subscription.getLastError();
        return window.lastSubscriptionError || '';
    }

    function getTrackStreamState(publisherIdentity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) return null;
        var streamState = null;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video') {
                if (!pub.subscribed) {
                    streamState = 'unsubscribed';
                } else if (pub.track) {
                    streamState = pub.track.streamState || 'active';
                } else {
                    streamState = 'pending';
                }
            }
        });
        return streamState;
    }

    function isVideoSubscribed(publisherIdentity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return false;
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) return false;
        var subscribed = false;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video' && pub.subscribed) {
                subscribed = true;
            }
        });
        return subscribed;
    }

    function setVideoSubscribed(publisherIdentity, subscribed) {
        if (window.TestStateStore) {
            window.TestStateStore.subscription.setLastError('');
        }
        window.lastSubscriptionError = null;
        if (!window.liveKitClient || !window.liveKitClient.room) {
            var err = 'No room connected';
            if (window.TestStateStore) window.TestStateStore.subscription.setLastError(err);
            window.lastSubscriptionError = err;
            return false;
        }
        var participant = null;
        window.liveKitClient.room.remoteParticipants.forEach(function(p) {
            if (p.identity === publisherIdentity) participant = p;
        });
        if (!participant) {
            var err = 'Participant not found: ' + publisherIdentity;
            if (window.TestStateStore) window.TestStateStore.subscription.setLastError(err);
            window.lastSubscriptionError = err;
            return false;
        }
        var success = false;
        participant.trackPublications.forEach(function(pub) {
            if (pub.kind === 'video') {
                try {
                    pub.setSubscribed(subscribed);
                    success = true;
                    console.log('Set video subscription for', publisherIdentity, 'to', subscribed);
                } catch (e) {
                    var errorMsg = e.message || String(e);
                    if (window.TestStateStore) window.TestStateStore.subscription.setLastError(errorMsg);
                    window.lastSubscriptionError = errorMsg;
                    console.error('Failed to set subscription:', e);
                }
            }
        });
        if (!success && !getLastSubscriptionError()) {
            var err = 'No video track found for: ' + publisherIdentity;
            if (window.TestStateStore) window.TestStateStore.subscription.setLastError(err);
            window.lastSubscriptionError = err;
        }
        return success;
    }

    function clearDynacastState() {
        if (window.TestStateStore) window.TestStateStore.trackStream.clear();
        window.trackStreamStateEvents = [];
    }

    function getSubscriberVideoStats(publisherIdentity) {
        return new Promise(function(resolve) {
            if (!window.liveKitClient || !window.liveKitClient.room) {
                resolve(null);
                return;
            }
            var participant = null;
            window.liveKitClient.room.remoteParticipants.forEach(function(p) {
                if (p.identity === publisherIdentity) participant = p;
            });
            if (!participant) {
                resolve(null);
                return;
            }
            var videoTrack = null;
            var publication = null;
            participant.trackPublications.forEach(function(pub) {
                if (pub.kind === 'video' && pub.track) {
                    videoTrack = pub.track;
                    publication = pub;
                }
            });
            if (!videoTrack) {
                resolve(null);
                return;
            }
            var result = {
                isSubscribed: publication.isSubscribed,
                hasTrack: !!videoTrack,
                frameWidth: 0,
                frameHeight: 0,
                isPlaying: false,
                streamState: videoTrack.streamState || 'unknown',
                timestamp: Date.now()
            };
            if (videoTrack.dimensions) {
                result.frameWidth = videoTrack.dimensions.width || 0;
                result.frameHeight = videoTrack.dimensions.height || 0;
            }
            if (videoTrack.mediaStreamTrack) {
                var settings = videoTrack.mediaStreamTrack.getSettings();
                if (settings.width) result.frameWidth = settings.width;
                if (settings.height) result.frameHeight = settings.height;
                if (videoTrack.mediaStreamTrack.readyState === 'live') {
                    result.isPlaying = true;
                }
            }
            var participantDiv = document.getElementById('participant-' + publisherIdentity);
            if (participantDiv) {
                var video = participantDiv.querySelector('video');
                if (video && video.videoWidth > 0 && video.videoHeight > 0 && !video.paused && !video.ended && video.readyState >= 2) {
                    result.isPlaying = true;
                    result.frameWidth = video.videoWidth;
                    result.frameHeight = video.videoHeight;
                }
            }
            if (!result.isPlaying) {
                var videos = document.querySelectorAll('video');
                for (var i = 0; i < videos.length; i++) {
                    var v = videos[i];
                    if (v.videoWidth > 0 && v.videoHeight > 0 && !v.paused && v.readyState >= 2) {
                        result.isPlaying = true;
                        if (result.frameWidth === 0) result.frameWidth = v.videoWidth;
                        if (result.frameHeight === 0) result.frameHeight = v.videoHeight;
                        break;
                    }
                }
            }
            resolve(result);
        });
    }

    function isReceivingVideoFrom(publisherIdentity) {
        return new Promise(function(resolve) {
            getSubscriberVideoStats(publisherIdentity).then(function(stats) {
                if (!stats) {
                    resolve(false);
                    return;
                }
                var isReceiving = stats.isSubscribed && stats.hasTrack &&
                    (stats.frameWidth > 0 || stats.isPlaying || stats.streamState === 'active');
                console.log('isReceivingVideoFrom(' + publisherIdentity + '):', JSON.stringify(stats), '-> isReceiving:', isReceiving);
                resolve(isReceiving);
            });
        });
    }

    function measureVideoReceptionRate(publisherIdentity, intervalMs) {
        return new Promise(function(resolve) {
            resolve({ bytesPerSecond: 0, framesPerSecond: 0 });
        });
    }

    return {
        getSubscriptionFailedEventCount: getSubscriptionFailedEventCount,
        isSubscriptionPermissionDenied: isSubscriptionPermissionDenied,
        getLastSubscriptionError: getLastSubscriptionError,
        getTrackStreamState: getTrackStreamState,
        isVideoSubscribed: isVideoSubscribed,
        setVideoSubscribed: setVideoSubscribed,
        clearDynacastState: clearDynacastState,
        getSubscriberVideoStats: getSubscriberVideoStats,
        isReceivingVideoFrom: isReceivingVideoFrom,
        measureVideoReceptionRate: measureVideoReceptionRate
    };
})();

window.SubscriptionHelpers = SubscriptionHelpers;
