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

    return {
        getSubscriptionFailedEventCount: getSubscriptionFailedEventCount,
        isSubscriptionPermissionDenied: isSubscriptionPermissionDenied,
        getLastSubscriptionError: getLastSubscriptionError,
        getTrackStreamState: getTrackStreamState,
        isVideoSubscribed: isVideoSubscribed,
        setVideoSubscribed: setVideoSubscribed,
        clearDynacastState: clearDynacastState
    };
})();

window.SubscriptionHelpers = SubscriptionHelpers;
